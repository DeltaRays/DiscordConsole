package me.deltarays.discordconsole.discord


import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import me.deltarays.discordconsole.DiscordConsole
import me.deltarays.discordconsole.LogLevel
import me.deltarays.discordconsole.Utils
import me.deltarays.discordconsole.logging.LogType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.bukkit.Bukkit
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import kotlin.math.ceil

class DiscordSocket(uri: URI) : WebSocketClient(uri) {
    private lateinit var plugin: DiscordConsole
    var lastS: String? = null
    private var sessionId: String? = null
    private var isConnected = false
    private lateinit var botId: String
    private var isInvalid = false
    lateinit var job: Job

    companion object {
        fun getWSUrl(): String? {
            val client = OkHttpClient()
            val request = try {
                (Request.Builder()).url("$BASE_API_URL/gateway?v=8&encoding=json").build()
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
                parser.parse(response.body()?.string())
            } catch (e: Exception) {
                return null
            }
            return obj.asJsonObject.get("url").asString
        }
    }

    fun setHandlingPlugin(pl: DiscordConsole) {
        this.plugin = pl
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        if (plugin.config.getBoolean("debug", false))
            Utils.logColored(
                plugin.getConfigManager().getPrefix(), "&a[Socket] Socket opened!", LogLevel.DEBUG
            )
    }

    private val parser = JsonParser()
    private var ackReceived = true
    override fun onMessage(message: String?) {
        if (plugin.config.getBoolean("debug", false))
            Utils.logColored(
                plugin.getConfigManager().getPrefix(),
                "[Socket] Message received!\n$message",
                LogLevel.DEBUG
            )
        val payload: JsonObject = try {
            parser.parse(message).asJsonObject
        } catch (e: Exception) {
            if (plugin.config.getBoolean("debug", false)) {
                Utils.logColored(
                    plugin.getConfigManager().getPrefix(), "&4[WebSocket] Error!", LogLevel.SEVERE
                )
                e.printStackTrace()
            } else Utils.logColored(
                plugin.getConfigManager().getPrefix(),
                "&c[Discord Connection] Error!\nMessage: &f" + e.message + "\n&cSet debug to true in the config to find out more!",
                LogLevel.SEVERE
            )
            return
        }
        val op: Int = payload.get("op").asInt
        val unparsedS = payload.get("s")
        if (!unparsedS.isJsonNull) this.lastS = unparsedS.asString
        if (op == 10) {
            if (this.sessionId == null) {
                val heartbeatInterval = payload.get("d").asJsonObject.get("heartbeat_interval").asLong
                job = GlobalScope.launch(Dispatchers.IO) {
                    while (true) {
                        delay(heartbeatInterval)
                        if (isInvalid) {
                            this.cancel("invalid")
                        }
                        if (!ackReceived) {

                            close(3333, "No Heartbeat ACK received!")
                        }
                        val obj = JsonObject().apply {
                            addProperty("op", 1)
                            addProperty("d", lastS)
                        }
                        ackReceived = false
                        send(obj.toString())
                    }
                }
                val obj = JsonObject().apply {
                    addProperty("op", 2)
                    add("d", JsonObject().apply {
                        addProperty("token", plugin.getConfigManager().getBotToken())
                        addProperty("intents", 1 shl 9)
                        add("properties", JsonObject().apply {
                            addProperty("\$os", "unknown")
                            addProperty("\$browser", "DiscordSocket")
                            addProperty("\$device", "DiscordConsole")
                        })
                        val botSection = plugin.getConfigManager().getBotSection()
                        add("presence", JsonObject().apply {
                            if (botSection.isString("status"))
                                addProperty("status", botSection.getString("status"))
                            if (botSection.isConfigurationSection("activity")) {
                                val activitySection = botSection.getConfigurationSection("activity")
                                    ?: botSection.createSection("activity")
                                add("activities", JsonArray().apply {
                                    add(JsonObject().apply {
                                        addProperty("name", activitySection.getString("name"))
                                        addProperty(
                                            "type",
                                            activityNameToInt(activitySection.getString("type", "game"))
                                        )
                                        for (key in activitySection.getKeys(false)) {
                                            if (!listOf("name", "type").contains(key)) {
                                                if (activitySection.isString(key))
                                                    addProperty(key, activitySection.getString("key"))
                                                else if (activitySection.isBoolean(key))
                                                    addProperty(key, activitySection.getBoolean("key"))
                                                else
                                                    addProperty(key, activitySection.get("key") as Int)
                                            }
                                        }
                                    })
                                })
                            }
                        })
                    })
                }
                send(obj.toString())
            } else {
                val obj = JsonObject().apply {
                    addProperty("op", 6)
                    add("d", JsonObject().apply {
                        addProperty("token", plugin.getConfigManager().getBotToken())
                        addProperty("session_id", sessionId)
                        addProperty("seq", lastS)
                    })
                }
                send(obj.toString())
            }
        } else if (op == 0) {
            when (payload.get("t").asString) {
                "READY" -> handleReady(payload)
                "RESUMED" -> handleResumed()
                "MESSAGE_CREATE" -> handleMessage(payload)
            }
        } else if (op == 9) {
            val d = payload.get("d").asBoolean
            // d represents whether or not the session is resumable
            plugin.logger.info(d.toString())
            if (d) {
                Utils.logColored(
                    plugin.getConfigManager().getPrefix(),
                    "&e[Discord Connection] Unable to resume from when the bot was last connected! Creating a new connection...",
                    LogLevel.WARNING
                )
                this.sessionId = null
                this.lastS = null
                GlobalScope.launch(Dispatchers.Default) {
                    delay(ceil(Math.random() * 5).toLong())
                    reconnect()
                }
            } else {
                this.sessionId = null
                this.lastS = null
                Utils.logColored(
                    plugin.getConfigManager().getPrefix(),
                    "&c[Discord Connection] Session was marked as invalid by Discord! Check all your config data as there might be something incorrect.",
                    LogLevel.SEVERE
                )
            }
        } else if (op == 7) {
            this.close(3333, "Discord asked me to reconnect")
        } else if (op == 11) {
            this.ackReceived = true;
        }

    }

    private fun handleReady(payload: JsonObject) {
        isConnected = true
        val d = payload.get("d").asJsonObject
        this.sessionId = d.get("session_id").asString
        this.botId = d.get("user").asJsonObject.get("id").asString
        Utils.logColored(
            plugin.getConfigManager().getPrefix(),
            "&a[Discord Connection] Successfully connected!",
            LogLevel.INFO
        )
    }

    private fun handleResumed() {
        Utils.logColored(
            plugin.getConfigManager().getPrefix(),
            "&a[Discord Connection] Bot has successfully resumed from where it last disconnected!",
            LogLevel.INFO
        )
    }

    private fun handleMessage(payload: JsonObject) = GlobalScope.launch(Dispatchers.IO) {
        val d = payload.get("d").asJsonObject
        val channelId = d.get("channel_id").asString
        val content = d.get("content").asString.replace("\\\\\"", "\"").replace("\\\\\\\\", "\\\\");
        val author = d.get("author").asJsonObject
        val authorId = author.get("id").asString
        val channel = DiscordChannel.channels.find { c ->
            c.id == channelId
        }
        if (authorId == botId) return@launch
        if (channel != null) {
            if (channel.types.containsValue(LogType.CONSOLE)) {
                val channelSection = plugin.getConfigManager().getChannel(channel.id)
                val consoleSection =
                    channelSection.getConfigurationSection("console") ?: channelSection.createSection("console")
                if (consoleSection.getBoolean("commands-enabled", false)) {
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), content)
                    })
                    return@launch
                }
            }
        }
        val discordCommandSection = plugin.getConfigManager().getCustomDiscordCmdSection()
        val member = d.get("member").asJsonObject
        for (key in discordCommandSection.getKeys(false)) {
            if (content.startsWith(key)) {
                val code = DiscordChannel.sendMessage(
                    channelId,
                    plugin.getConfigManager().getBotToken() ?: "",
                    Utils.convertPlaceholders(
                        discordCommandSection.get(key).toString(),
                        memberUser = Pair(member, author)
                    )
                ).await()
                if (code == 403) {
                    Utils.logColored(
                        plugin.getConfigManager().getPrefix(),
                        "&cThe bot doesn't have access to send messages to channel with id &4$channelId&c!",
                        LogLevel.SEVERE
                    )
                }
            }
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        this.isConnected = false
        job.cancel("closed")
        if (plugin.config.getBoolean("debug", false)) {
            Utils.logColored(
                plugin.getConfigManager().getPrefix(), String.format(
                    "[WebSocket] Closed!\nRemote: %s\nCode: %s\nReason:%s",
                    remote,
                    code,
                    reason
                ), LogLevel.DEBUG
            )
        }
        when (code) {
            4004 -> {
                Utils.logColored(
                    plugin.getConfigManager().getPrefix(), "&cAn invalid bot token was provided!", LogLevel.SEVERE
                )
                this.isInvalid = true
                return
            }
            4000 -> {
                Utils.logColored(
                    plugin.getConfigManager().getPrefix(),
                    "&e[Discord Connection] Discord encountered an unknown error! Reconnecting...",
                    LogLevel.WARNING
                )
                this.reconnect()
            }
            4001, 4002, 4003, 4005, 4007, 4012, 4013, 4014 -> {
                Utils.logColored(
                    plugin.getConfigManager().getPrefix(),
                    "&4[Discord Connection] The plugin's author made an oopsie! Contact him on discord (https://discord.gg/WSaWztJ) and tell him that you encountered an issue with" +
                            String.format(
                                "code: %s and reason: %s", code, reason
                            ),
                    LogLevel.SEVERE
                )
                this.isInvalid = true
            }
            4008 -> {
                Utils.logColored(
                    plugin.getConfigManager().getPrefix(),
                    "&e[Discord Connection] The bot was disconnected for doing something too fast! Reconnecting...",
                    LogLevel.WARNING
                )
                this.reconnect()
            }
            4009 -> {
                Utils.logColored(
                    plugin.getConfigManager().getPrefix(),
                    "&e[Discord Connection] The bot timed out! Reconnecting...",
                    LogLevel.INFO
                )
                this.reconnect()
            }
            3333 -> {
                Utils.logColored(
                    plugin.getConfigManager().getPrefix(),
                    "&e[Discord Connection] The bot was disconnected! Reconnecting...",
                    LogLevel.INFO
                )
                this.reconnect()
            }
        }

    }

    override fun onError(e: Exception?) {
        if (plugin.config.getBoolean("debug", false)) {
            Utils.logColored(
                plugin.getConfigManager().getPrefix(), "&4[WebSocket] Error!", LogLevel.SEVERE
            )
            e?.printStackTrace()
        } else Utils.logColored(
            plugin.getConfigManager().getPrefix(),
            "&c[Discord Connection] Error!\nMessage: &f" + e?.message + "\n&cSet debug to true in the config to find out more!",
            LogLevel.SEVERE
        )
    }
}


fun activityNameToInt(activity: String?): Short? {
    return when (activity?.toLowerCase()) {
        "game" -> 0
        "streaming" -> 1
        "listening" -> 2
        "custom" -> 4
        "competing" -> 5
        else -> null
    }
}