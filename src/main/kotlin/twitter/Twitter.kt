package twitter

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.info
import pluginController.PluginConfig
import pluginController.PluginData
import pluginController.PluginMain
import utils.httpGet
import utils.proxy
import utils.recentSearchUrlGenerator
import java.net.URL
import java.net.URLEncoder
import kotlin.math.min

var globalNextToken: String? = ""

fun getNewestTweet(
    target: String = "from:YuGiOh_OCG_INFO"
): JSONObject? {
    return try {
        PluginMain.logger.info(target.substring(5))
        var timeline = httpGet(
            recentSearchUrlGenerator(
                searchTarget = target,
                sinceID = PluginData.lastTweetID[target.substring(5)].toString()
                //去掉from:
            )
        )
        if (timeline.containsKey("errors")){
            val errorMessage = timeline.getJSONArray("errors").getJSONObject(0).getString("message")
            if (errorMessage.contains("must be a tweet id created after")){
                timeline = httpGet(
                    recentSearchUrlGenerator(
                        searchTarget = target,
                    )
                )
            }
        }
        timeline
    } catch (e: Exception) {
        PluginMain.logger.info(e.message)
        null
    }
}

suspend fun getTimelineAndSendMessage(
    inquirerGroup: Group,
    nextToken: String = "",
    maxCount: Int = 1,
    startCount: Int = 0,
    target: String = "from:YuGiOh_OCG_INFO",
    maxResults: Int = 10,
) {
    try {
        val timeline = httpGet(
            recentSearchUrlGenerator(
                nextToken = nextToken,
                searchTarget = URLEncoder.encode(target, "utf-8"),
                maxResults = maxResults
            )
        )
        val tweetMeta = JSON.parseObject(timeline.getJSONObject("meta").toString())
        val resultCount = tweetMeta?.getString("result_count").toString()
        PluginMain.logger.info { "成功获取$resultCount" + "条tweets" }
        when {
            resultCount.toInt() > 0 ->
                inquirerGroup.sendMessage("成功获取${resultCount}条推文")
            else ->
                inquirerGroup.sendMessage(
                    PlainText("哎呀，什么都没有找到呢")
                        + Image("{886929C6-7DEB-5878-3549-9DAB9310D323}.gif")
                )
        }

        val tweetData = timeline.getJSONArray("data")
        val tweetMedia = timeline.getJSONObject("includes")?.getJSONArray("media")
        val authors = timeline.getJSONObject("includes")?.getJSONArray("users")

        globalNextToken = tweetMeta?.getString("next_token")

        var mediaUrls: MutableList<String> = mutableListOf()

        for (count in startCount until min(startCount + maxCount, min(10, resultCount.toInt()))) {
            var toSay: Message = buildMessageChain { }

            val newestTweet = tweetData?.getJSONObject(count)
            val newestText = newestTweet?.getString("text").toString()
            val authorID = newestTweet?.getString("author_id")

            if (newestTweet != null) {
                if (newestTweet.containsKey("attachments")) {
                    val mediaKeys = newestTweet.getJSONObject("attachments").getJSONArray("media_keys").toList()
                    mediaUrls = getMediaUrlsFromKeys(tweetMedia, mediaKeys)
                }

            }

            for (i in 0 until authors?.size!!) {
                val users = authors.getJSONObject(i)
                if (users.getString("id").toString() == authorID) {
                    toSay = PlainText(
                        users.getString("name").toString() +
                            "(@${users.getString("username")}):\r\n"
                    )
                }
            }

            if ("null" != newestText) {
                toSay += newestText.toPlainText()
            }
            // 由于tx不让一次发送约100(104?)个字符以上的PlainText，故此处使用特殊处理分割,可以通过命令开关

            if (PluginConfig.ifNeedToSplit) toSay = sendAndSplitToUnder100(toSay.content.toPlainText(), inquirerGroup)

            if (mediaUrls.isNotEmpty()) {
                PluginMain.logger.info("有${mediaUrls.size}张图片")
                mediaUrls.forEach {
                    PluginMain.logger.info("url = $it")
                    toSay += Image(
                        URL(it).openConnection(proxy).getInputStream()
                            .uploadAsImage(inquirerGroup)
                            .imageId
                    )
                }
                mediaUrls.clear()
            }

            if (!toSay.isContentEmpty()) inquirerGroup.sendMessage(toSay)

        }
    } catch (e: Exception) {
        when {
            e.toString() == "No Available Bearer Token" -> {
                inquirerGroup.sendMessage(
                    PlainText(
                        "哎呀，还没有设置Bearer Token呢"
                    ) +
                        Image("{886929C6-7DEB-5878-3549-9DAB9310D323}.gif")
                )
            }
        }
    }

}

suspend fun getRealMediaUrlFromTwitterID(id:String):String{
    return try {
        val tweet = httpGet(
            "${PluginConfig.APIs["showSingle"]}" + "id=$id"
        )
        val extendedEntities = JSON.parseObject(tweet.getJSONObject("extended_entities").toString())
        val medias = extendedEntities.getJSONArray("media")
        val media = medias.getJSONObject(0)
        val mediaInfo = media.getJSONObject("video_info")
        mediaInfo.getJSONArray("variants").getJSONObject(0)
            .getString("url")
    } catch (e:Exception){
        println("error at getRealMediaUrlFromTwitterID: $e ")
        ""
    }
}

fun checkUserName(userName: String): String {
    try {
        val userData = httpGet(
            PluginConfig.APIs["usersBy"].toString() +
                "/username/$userName"
        )
        if (userData.containsKey("errors")) throw Exception("No Such User")
        return userData.getJSONObject("data").getString("name")
    } catch (e: Exception) {
        throw e
    }
}

fun getMediaUrlsFromKeys(
    tweetMedia: JSONArray?,
    mediaKeys: List<Any>,
): MutableList<String> {
    val mediaUrls: MutableList<String> = mutableListOf()
    for (i in 0 until tweetMedia?.size!!) {
        val media = tweetMedia.getJSONObject(i)
        if (media.getString("type") == "photo" && media.getString("media_key").toString() in mediaKeys) {
            mediaUrls.add(media.getString("url"))
        }
    }
    return mediaUrls
}

suspend fun sendAndSplitToUnder100 (message : PlainText, target: Contact) : MessageChain { //将要发送的PlainText拆分成最长99的字节并发送
    PluginMain.logger.info("正在分割长句$message")
    var messageText = message.content
    while (messageText.length > 99){
        val sub100 =  messageText.substring(0, 99)
        target.sendMessage(sub100.toPlainText())
        messageText = messageText.substring(99, messageText.length)
    }
    return messageText.toPlainText().toMessageChain()
}