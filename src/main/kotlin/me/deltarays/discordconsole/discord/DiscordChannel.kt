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
import java.util.concurrent.LinkedBlockingQueue


class DiscordChannel(val id: String, private val plugin: DiscordConsole, var types: MutableSet<LogType>) {
    private val client = OkHttpClient()
    private var queue: LinkedBlockingQueue<String> = LinkedBlockingQueue()
    private var canChangeTopic = true
    private val parser = JsonParser()
    private var sendMessageJob: Job? = null
    private var getDataJob: Job? = null
    var guild: DiscordGuild? = null

    private var hasData = false
    lateinit var name: String
    lateinit var topic: String

    init {
        channels.add(this)
    }

    private fun destroy() {
        getDataJob?.cancel()
        sendMessageJob?.cancel()
        flush()
        channels.removeIf { channel -> channel == this }
    }

    private fun flush() {
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

    fun initializeJobs() {
        if (sendMessageJob != null) return
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
                    destroy()
                } else if (code == 403) {
                    Utils.logColored(
                        plugin.getConfigManager().getPrefix(),
                        "&cThe bot doesn't have access to channel with id &4$id&c!",
                        LogLevel.SEVERE
                    )
                    destroy()
                }
                val json = parser.parse(response.body()?.string()).asJsonObject
                if (json.has("errors")) {
                    Utils.logColored(
                        plugin.getConfigManager().getPrefix(),
                        "&cAn error was encountered while getting data for the channel with id $id! (Is the id correct?)",
                        LogLevel.SEVERE
                    )
                    destroy()
                }
                val guildId = json.get("guild_id").asString
                guild = DiscordGuild.guilds.find { g ->
                    g.id == guildId
                } ?: DiscordGuild(json.get("guild_id").asString, plugin)
                guild!!.channels.add(this@DiscordChannel)
                hasData = true
                name = json.get("name").asString
                topic = json.get("topic").asString
                response.close()
                val topic = getConfigTopic()
                if (topic != null)
                    setTopic(Utils.convertPlaceholders(topic))
                delay(20000)

            }
        }
    }


    companion object {
        val channels = mutableListOf<DiscordChannel>()
        private val client = OkHttpClient()
        private fun initializeAll(plugin: DiscordConsole) {
            val configManager = plugin.getConfigManager()
            val channels = configManager.getChannels()
            // Loops through all the channels
            channels.getKeys(false).forEach { channelId ->
                if (!channelId.startsWith("cmt_") && channelId.equals("id", true)) {
                    val channel = configManager.getChannel(channelId)
                    val keys = mutableSetOf<LogType>()
                    // Loops through the log types of all channels and adds the right ones to the list
                    for (logType in channel.getKeys(false)) {
                        if (logType.startsWith("cmt_")) continue
                        val logSection = channel.getConfigurationSection(logType) ?: continue
                        val isActive = logSection.getBoolean("active", false)
                        if (!isActive) continue
                        keys.add(LogType.valueOf(logType.toUpperCase()))
                    }
                    DiscordChannel(channelId, plugin, keys)
                }
            }

            /**
             * Removes the discord channels, bulk sends all remaining messages and re initializes the channels
             */


        }


        fun resetChannelsGuilds(plugin: DiscordConsole) {
            channels.forEach { channel ->
                channel.destroy()
            }
            DiscordGuild.guilds.forEach { guild ->
                guild.destroy()
            }

            initializeAll(plugin)
        }


        fun sendMessageAsync(channelId: String, botToken: String, message: String): Deferred<Int> =
            GlobalScope.async(Dispatchers.IO) {
                val obj = JsonObject().apply {
                    add("allowed_mentions", JsonObject().apply {
                        add("parse", JsonArray())
                    })
                    addProperty("content", message)
                }
                val body = RequestBody.create(MediaType.get("application/json"), obj.toString())
                val request = Request.Builder()
                    .url("$BASE_API_URL/channels/$channelId/messages")
                    .post(body)
                    .addHeader("Authorization", "Bot $botToken")
                    .build()
                val response = client.newCall(request).execute()
                val code = response.code()
                response.close()
                return@async code
            }
    }

    fun enqueueMessage(message: String) {
        queue.add(message)
    }

    fun getMessageFormat(type: LogType): String {
        return plugin.getConfigManager().getChannel(id).getConfigurationSection(type.name.toLowerCase())
            ?.getString("format", type.defaultFormat)!!
    }

    private fun getSendingCooldown(): Long {
        return plugin.getConfigManager().getChannel(id).getLong("cooldown", 1111)
    }

    private fun getConfigTopic(): String? {
        return plugin.getConfigManager().getChannel(id).getString("topic")
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun sendMessage(message: String) =
        coroutineScope {
            withContext(Dispatchers.IO) {
                val code =
                    sendMessageAsync(id, plugin.getConfigManager().getBotToken() ?: "", message).await()
                if (code == 404) {
                    Utils.logColored(
                        plugin.getConfigManager().getPrefix(),
                        "&cChannel with id &4$id &cdoesn't exist!",
                        LogLevel.SEVERE
                    )
                    destroy()
                } else if (code == 403) {
                    Utils.logColored(
                        plugin.getConfigManager().getPrefix(),
                        "&cThe bot doesn't have access to channel with id &4$id&c!",
                        LogLevel.SEVERE
                    )
                    destroy()
                }
                return@withContext
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
            when (response.code()) {
                400 -> {
                    Utils.logColored(
                        plugin.getConfigManager().getPrefix(),
                        "&cThe parameters sent to channel $id's topic change are invalid! Topic changing for that channel will be disabled until a reload",
                        LogLevel.WARNING
                    )
                    canChangeTopic = false
                }
                404 -> {
                    Utils.logColored(
                        plugin.getConfigManager().getPrefix(),
                        "&cChannel with id &4$id &cdoesn't exist! Their topic won't be changed!",
                        LogLevel.WARNING
                    )
                    destroy()
                }
                403 -> {
                    Utils.logColored(
                        plugin.getConfigManager().getPrefix(),
                        "&cThe bot doesn't have access to edit channel &4$id&c's topic! Topic changing for that channel will be disabled until a reload",
                        LogLevel.WARNING
                    )
                    canChangeTopic = false
                }
            }
            response.close()
        }
    }
}