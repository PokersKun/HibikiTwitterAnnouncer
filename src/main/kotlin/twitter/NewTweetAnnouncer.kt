package twitter

import com.alibaba.fastjson.JSON
import com.fasterxml.jackson.databind.JsonMappingException
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.coroutines.awaitObject
import dataClass.TranslationDeserializer
import kotlinx.coroutines.delay
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.info
import pluginController.PluginConfig
import pluginController.PluginData
import pluginController.PluginMain
import utils.proxy
import java.net.URL

suspend fun checkNewTweet(bot: Bot) {
    val group = mutableSetOf<Group>()
    lateinit var targetGroup: Group

    while (true) {
        if (PluginData.ifGroupListHasChanged) {
            PluginMain.logger.info("Mirai重新启动或检测到订阅群列表发生变化，正在重新加载群列表")
            group.clear()
            while (true) {
                if (!PluginData.groups.isNullOrEmpty()) break
                delay(10000L)
            }
            PluginData.groups.forEach {
                while (true) {
                    try {
                        targetGroup = bot.getGroup(it)!!
                        targetGroup
                        break
                    } catch (e: Exception) {
                        delay(1000)
                    }
                }
                group.add(targetGroup)
            }
            PluginMain.logger.info("加载群列表完成，共加载${group.size}个群")
            PluginData.ifGroupListHasChanged = false
        }

        PluginMain.logger.info("检测开始")

        if (PluginData.groups.isEmpty()) {
            continue
        }

        group.forEach lit@{ it ->
            val thisGroup: Group = it
            if (PluginData.listeningListByGroup[it.id].isNullOrEmpty()) {
                PluginMain.logger.info("本群没有关注列表")
                return@lit
            }
            val listenerList = PluginData.listeningListByGroup[it.id] as MutableSet<String>
            PluginMain.logger.info("群${it.name}订阅列表共有${listenerList.size}位推主")
            if (listenerList.isNullOrEmpty()) return@lit
            listenerList.forEach l@{
                try {
                    singleTryForNewTweet(thisGroup, it)
                }catch (e: Exception) {
                    PluginMain.logger.info("error: ${e.message} ${e.cause}")
                    delay(60000L)
                    return@l
                }
            }
        }

        PluginMain.logger.info("检测正常结束")
        delay(60000L)
    }
}

private suspend fun singleTryForNewTweet(group: Group, target: String) {
    val newestTweets = getNewestTweet(
        "from:$target"
    ) ?: throw Exception("Fail on getting newest Tweet")

    val tweetMeta = JSON.parseObject(newestTweets.getJSONObject("meta").toString())
    val resultCount = tweetMeta?.getString("result_count").toString()
    PluginMain.logger.info { "获取来自${target}的$resultCount" + "条新tweets" }

    if (resultCount == "0") {
        PluginMain.logger.info("@${target}暂时没有更新")
        delay(1000L)
        return
    }

    var ifFirstToSend = true

    for (count in 0 until resultCount.toInt()) {
        val data = newestTweets.getJSONArray("data").getJSONObject(count)
        val newestText = data.getString("text")
        val newestTweetID = data.getString("id")

        if (ifFirstToSend) PluginData.lastTweetID[target] = newestTweetID

        if (PluginData.filterWith[target]!=null){
            PluginData.filterWith[target]!!.forEach{
                if (!newestText.contains(it)) {
                    PluginMain.logger.info("有消息被用户过滤器阻止")
                    return
                }
            }
        }

        if (PluginData.filterWithout[target]!=null){
            PluginData.filterWithout[target]!!.forEach{
                if (newestText.contains(it)) {
                    PluginMain.logger.info("有消息被用户过滤器阻止")
                    return
                }
            }
        }

        if (newestText.contains("RT")) {   // 过滤转发
            PluginMain.logger.info("有消息被用户过滤器阻止")
            continue
        }

        val mediaUrls = if (data.containsKey("attachments")) {
            getMediaUrlsFromKeys(
                tweetMedia = newestTweets.getJSONObject("includes").getJSONArray("media"),
                mediaKeys = data.getJSONObject("attachments").getJSONArray("media_keys")
            )
        } else null

        var toSay = PlainText("@${target}:\r\n").toMessageChain()
        if ("null" != newestText) {

            toSay += newestText.toPlainText()
        }
        // 由于tx不让新号一次发送约100(104?)个字符以上的PlainText，故此处使用特殊处理分割,可以通过命令开关

        if (ifFirstToSend) {
//            group.sendMessage("关注的推主@${target}有新推了哦")
            ifFirstToSend = false
        }

        if (PluginConfig.ifNeedToSplit) toSay = sendAndSplitToUnder100(toSay.content.toPlainText(), group)

        if (!mediaUrls.isNullOrEmpty()) {
            PluginMain.logger.info("有${mediaUrls.size}张图片")
            mediaUrls.forEach {
                var ifSuccess: Boolean
                do {
                    try {
                        toSay += Image(
                            URL(it).openConnection(proxy).getInputStream()
                                .uploadAsImage(group)
                                .imageId
                        )
                        ifSuccess = true
                    } catch (e:Exception){
                        PluginMain.logger.info("Error at uploading image: ${e.message}")
                        ifSuccess = false
                    }
                } while (!ifSuccess)
            }
            mediaUrls.clear()
        } else continue   // 仅发送图片

        if (!toSay.isContentEmpty()) {
            val apiKey = PluginConfig.Tokens["apiKey"]
            if (apiKey == "") throw Exception("No Available Api Key")

            try {
                val result = Fuel.post("${PluginConfig.APIs["translate"]}",
                    listOf("key" to apiKey))
                    .jsonBody("{ \"q\" : \"$newestText\", " +
                        "  \"target\": \"zh\",\n" +
                        "  \"format\": \"text\" }")
                    .awaitObject(TranslationDeserializer)
                if (result.data.translations[0].translatedText != "")
                    toSay += "\r\nGoogle Translation:\r\n${result.data.translations[0].translatedText}"
                PluginMain.logger.info("正在向群${group.name}发送推送：${toSay}")
                group.sendMessage(toSay)
                delay(1000L)
            } catch (exception: Exception) {
                when (exception) {
                    is HttpException -> throw Exception("A network request exception was thrown: ${exception.message}")
                    is JsonMappingException -> throw Exception("A serialization/deserialization exception was thrown: ${exception.message}")
                    else -> throw Exception("An exception [${exception.javaClass.simpleName}\"] was thrown")
                }
            }
        }
    }

}