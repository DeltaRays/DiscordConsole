package me.deltarays.discordconsole.discord

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import me.deltarays.discordconsole.DiscordConsole
import me.deltarays.discordconsole.LogLevel
import me.deltarays.discordconsole.Utils
import me.deltarays.discordconsole.logging.LogType
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.*


class DiscordChannel(val id: String, private val plugin: DiscordConsole, var types: HashMap<String, LogType>) {
    private val client = OkHttpClient()
    private var queue: Queue<String> = LinkedList()
    private var canChangeTopic = false
    private val parser = JsonParser()
    private var sendMessageJob: Job
    private var getDataJob: Job
    var guild: DiscordGuild? = null

    var hasData = false
    lateinit var name: String
    lateinit var topic: String

    init {
        channels.add(this)

        sendMessageJob = GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                delay(getSendingCooldown())
                val builder = StringBuilder()
                while (queue.isNotEmpty()) {
                    val value = queue.poll()
                    if (builder.length + value.length > 1999) {
                        sendMessage(builder.toString())
                        builder.clear()
                    }
                    builder.appendLine(value)
                }
                if (builder.isNotEmpty()) sendMessage(builder.toString())
            }
        }
        getDataJob = GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                val request = Request.Builder()
                    .url("$BASE_API_URL/channels/$id")
                    .get()
                    .addHeader("Authorization", "Bot ${plugin.getConfigManager().getBotToken()}")
                    .addHeader("Accept", "application/json")
                    .build()
                val response = client.newCall(request).execute()
                val code = response.code()
                if (code == 404) {
                    Utils.logColored(
                        plugin.getConfigManager().getPrefix(),
                        "&cChannel with id &4$id &cdoesn't exist!",
                        LogLevel.SEVERE
                    )
                    channels.remove(this@DiscordChannel)
                } else if (code == 403) {
                    Utils.logColored(
                        plugin.getConfigManager().getPrefix(),
                        "&cThe bot doesn't have access to channel with id &4$id&c!",
                        LogLevel.SEVERE
                    )
                    channels.remove(this@DiscordChannel)
                }
                val json = parser.parse(response.body()?.string()).asJsonObject
                val guildId = json.get("guild_id").asString
                guild = DiscordGuild.guilds.find { g ->
                    g.id == guildId
                } ?: DiscordGuild(json.get("guild_id").asString, plugin)
                guild!!.channels.add(this@DiscordChannel)
                hasData = true
                name = json.get("name").asString
                topic = json.get("topic").asString
                response.close()
                delay(20000)

            }
        }
    }

    fun destroy() {
        flush()
        getDataJob.cancel()
        sendMessageJob.cancel()
        channels.remove(this)
    }

    fun flush() {
        runBlocking(Dispatchers.IO) {
            val builder = StringBuilder()
            while (queue.isNotEmpty()) {
                val value = queue.poll()
                if (builder.length + value.length > 1999) {
                    sendMessage(builder.toString())
                    builder.clear()
                    delay(100)
                }
                builder.appendLine(value)
            }
            if (builder.isNotEmpty()) sendMessage(builder.toString())
        }
    }

    companion object {
        val channels = mutableListOf<DiscordChannel>()

        fun initializeAll() {
            TODO("Need to make the method to init the channel")
        }
    }

    fun enqueueMessage(message: String) {
        queue.add(message)
    }

    fun getMessageFormat(type: LogType): String {
        return plugin.getConfigManager().getChannel(id).getString(type.name.toLowerCase(), type.defaultFormat) as String
    }

    fun getSendingCooldown(): Long {
        return plugin.getConfigManager().getChannel(id).getLong("cooldown", 1111)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun sendMessage(message: String) =
        coroutineScope {
            withContext(Dispatchers.IO) {
                val obj = JsonObject().apply {
                    add("allowed_mentions", JsonObject().apply {
                        add("parse", JsonArray())
                    })
                    addProperty("content", message)
                }
                val body = RequestBody.create(MediaType.get("application/json"), obj.toString())
                val request = Request.Builder()
                    .url("$BASE_API_URL/channels/$id/messages")
                    .post(body)
                    .addHeader("Authorization", "Bot ${plugin.getConfigManager().getBotToken()}")
                    .build()
                val response = client.newCall(request).execute()
                val code = response.code()
                if (code == 404) {
                    Utils.logColored(
                        plugin.getConfigManager().getPrefix(),
                        "&cChannel with id &4$id &cdoesn't exist!",
                        LogLevel.SEVERE
                    )
                    channels.remove(this@DiscordChannel)
                } else if (code == 403) {
                    Utils.logColored(
                        plugin.getConfigManager().getPrefix(),
                        "&cThe bot doesn't have access to channel with id &4$id&c!",
                        LogLevel.SEVERE
                    )
                    channels.remove(this@DiscordChannel)
                }
                response.close()

            }
        }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun setTopic(topic: String) = coroutineScope {
        withContext(Dispatchers.IO) {
            if (!canChangeTopic) return@withContext
            if (topic.length > 1024) {
                Utils.logColored(
                    plugin.getConfigManager().getPrefix(),
                    "&cThe channel with id $id's topic must be less than 1024 characters long!",
                    LogLevel.WARNING
                )
                return@withContext
            }
            val obj = JsonObject().apply {
                addProperty("topic", topic)
            }
            val body = RequestBody.create(MediaType.get("application/json"), obj.toString())
            val request = Request.Builder()
                .url("$BASE_API_URL/channels/$id")
                .patch(body)
                .addHeader("Authorization", "Bot ${plugin.getConfigManager().getBotToken()}")
                .build()
            val response = client.newCall(request).execute()
            val code = response.code()
            if (code == 400) {
                Utils.logColored(
                    plugin.getConfigManager().getPrefix(),
                    "&cThe parameters sent to channel $id's topic change are invalid! Topic changing for that channel will be disabled until a reload",
                    LogLevel.WARNING
                )
                canChangeTopic = false
            } else if (code == 404) {
                Utils.logColored(
                    plugin.getConfigManager().getPrefix(),
                    "&cChannel with id &4$id &cdoesn't exist! Their topic won't be changed!",
                    LogLevel.WARNING
                )
                channels.remove(this@DiscordChannel)
            } else if (code == 403) {
                Utils.logColored(
                    plugin.getConfigManager().getPrefix(),
                    "&cThe bot doesn't have access to edit channel &4$id&c's topic! Topic changing for that channel will be disabled until a reload",
                    LogLevel.WARNING
                )
                canChangeTopic = false
            }
            response.close()
        }
    }
}