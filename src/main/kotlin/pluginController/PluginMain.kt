package pluginController

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.utils.info
import twitter.checkNewTweet
import commandHandler.messageEventHandler
import kotlin.random.Random

object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "org.sddn.hibiki",
        name = "HibikiTwitterAnnouncer",
        version = "1.1.3"
    ) {
        author("七度")

        info(
            """
            转发官推内容到QQ
        """.trimIndent()
        )

        // author 和 info 可以删除.
    }
) {
    lateinit var bot: Bot

    override fun onEnable() {
        logger.info { "Plugin loaded" }

        PluginConfig.reload()
        PluginData.reload()
        PluginData.ifGroupListHasChanged = true

        PluginMain.launch {
            while (true) {
                try {
                    bot = Bot.instances[0]
                    bot
                    break
                } catch (e: Exception) {
                    logger.info("error : ${e.message}")
                    delay(1000)
                }
            }

            checkNewTweet(bot)
        }
    }

}