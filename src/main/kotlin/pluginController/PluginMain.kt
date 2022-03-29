package pluginController

import commandHandler.messageEventHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.utils.info
import twitter.checkNewTweet

object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "org.sddn.hibiki",
        name = "HibikiTwitterAnnouncer",
        version = "1.1.5"
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

        globalEventChannel().subscribeAlways<GroupMessageEvent> {
            val messageText = message.contentToString()
            messageEventHandler(messageText)
            delay(100L)
        }

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