package pluginController

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object PluginConfig : AutoSavePluginConfig("config") {
    val APIs: MutableMap<String, String> by value(
        mutableMapOf(
            "usersBy" to "https://api.twitter.com/2/users/by",
            "recent" to "https://api.twitter.com/2/tweets/search/recent?query=",
            "showSingle" to "https://api.twitter.com/1.1/statuses/show.json?",
            "translate" to "https://translation.googleapis.com/language/translate/v2"
        )
    )

    val Tokens: MutableMap<String, String> by value(
        mutableMapOf(
            "bearerToken" to "",
            "apiKey" to ""
        )
    )

    var ifNeedToSplit : Boolean by value(false)


}