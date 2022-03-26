package utils

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import pluginController.PluginConfig
import pluginController.PluginMain
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL


fun recentSearchUrlGenerator(
    searchTarget: String = "YuGiOh_OCG_INFO",
    nextToken: String = "",
    expansions: String = "attachments.media_keys",
    mediaFields: String = "url",
    sinceID: String = "0",
    maxResults: Int = 10,
): String {
    return "${PluginConfig.APIs["recent"]}" +
        searchTarget +
        "&expansions=author_id,$expansions" +
        "&media.fields=$mediaFields" +
        "&user.fields=username,name" +
        if (nextToken != "") "&next_token=$nextToken" else "" +
        if (sinceID != "0") "&since_id=$sinceID" else ""
}

val bearerToken = PluginConfig.Tokens["bearerToken"]

fun httpGet(url: String): JSONObject {
    if (bearerToken == "") throw Exception("No Available Bearer Token")

    PluginMain.logger.info("Now Getting from $url")
    val link = URL(url)

    val connection = link.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 5000
    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
    connection.setRequestProperty("Authorization", "Bearer $bearerToken")

    try {
        val reader = BufferedReader(
            InputStreamReader(connection.inputStream, "utf-8")
        )
        val output: String = reader.readLine()
        return JSON.parseObject(output)
    } catch (exception: Exception) {
        PluginMain.logger.info(exception.message)
        if (connection.responseCode == 400) {
            return JSON.parseObject("{\"meta\":{\"result_count\":-1}}")
        }
        throw Exception(exception.message)
    }

}

