package me.deltarays.discordconsole


import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class DiscordSocket(uri: URI) : WebSocketClient(uri) {
    lateinit var plugin: DiscordConsole

    companion object {
        fun getWSUrl(): String? {
            val resp: String;
            val client = OkHttpClient()
            val request = try {
                (Request.Builder()).url("https://discordapp.com/api/v6/gateway").build()
            } catch (e: Exception) {
                return null
            }
            val response = try {
                client.newCall(request).execute()
            } catch (e: Exception) {
                return null
            }
            val parser = JsonParser()
            val obj = try {
                parser.parse(response.body().toString())
            } catch (e: Exception) {
                return null
            }
            return obj.asJsonObject.get("url").toString()
        }
    }

    fun setHandlingPlugin(pl: DiscordConsole) {
        this.plugin = pl
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        TODO("Not yet implemented")
    }

    override fun onMessage(message: String?) {
        TODO("Not yet implemented")
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onError(ex: Exception?) {
        TODO("Not yet implemented")
    }
}