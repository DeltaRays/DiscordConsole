package me.deltarays.discordconsole


import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.*

class DiscordSocket(uri: URI) : WebSocketClient(uri) {
    lateinit var plugin: DiscordConsole
    lateinit var timer: Timer
    var lastS: String? = null

    companion object {
        fun getWSUrl(): String? {
            val resp: String;
            val client = OkHttpClient()
            val request = try {
                (Request.Builder()).url("https://discordapp.com/api/v8/gateway?v=8&encoding=json").build()
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
        if (plugin.config.getBoolean("debug", false))
            plugin.logger.info("[WebSocket] Connected to discord!")
    }

    val parser = JsonParser()
    override fun onMessage(message: String?) {
        val payload: JsonObject = try {
            parser.parse(message).asJsonObject
        } catch (e: Exception) {
            if (plugin.config.getBoolean("debug", false)) {
                plugin.logger.error(("[WebSocket] Error!"))
                e.printStackTrace()
            } else
                plugin.logger.error("[Discord Connection] Error!\nMessage: " + e.message)
            return
        }
        val op: Int = payload.get("op").asInt
        val s = payload.get("s").asString
        if (s != null) this.lastS = s
        if (op == 10) {
            val heartbeatInterval = payload.get("d").asJsonObject.get("heartbeat_interval").asLong
            timer.schedule(object : TimerTask() {
                override fun run() {
                    val obj = JsonObject().apply {
                        addProperty("op", 1)
                        addProperty("d", lastS)
                    }
                }
            }, 1, heartbeatInterval)

            val obj = JsonObject().apply {
                addProperty("op", 2)
                add("d", JsonObject().apply {
                    addProperty("token", "") // TODO("Add token")
                    addProperty("intents", 1 shl 9)
                    add("properties", JsonObject().apply {
                        addProperty("\$os", "unknown")
                        addProperty("\$browser", "DiscordSocket")
                        addProperty("\$device", "DiscordConsole")
                    })
                })
            }
            this.send(obj.toString())
        }

    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onError(ex: Exception?) {
        TODO("Not yet implemented")
    }
}