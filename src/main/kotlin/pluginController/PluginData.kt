package pluginController

import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value

object PluginData : AutoSavePluginData("data") {
    val groups: MutableSet<Long> by value()
    val listeningListByGroup: MutableMap<Long, MutableSet<String>> by value()
    var ifGroupListHasChanged: Boolean by value(true)
    var lastTweetID: MutableMap<String, String> by value()
    var filterWith: MutableMap<String, MutableSet<String>> by value()
    var filterWithout: MutableMap<String, MutableSet<String>> by value()
}
