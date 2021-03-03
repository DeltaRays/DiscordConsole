package me.deltarays.discordconsole.logging

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import me.deltarays.discordconsole.DiscordConsole
import me.deltarays.discordconsole.LogLevel
import me.deltarays.discordconsole.Utils
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.*


class DiscordChannel(private val id: String, private val plugin: DiscordConsole, var types: HashMap<String, LogType>) {
    private val client = OkHttpClient()
    private var queue: Queue<String> = LinkedList()
    var job: Job

    init {
        channels.add(this)

        job = GlobalScope.launch(Dispatchers.IO) {
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
    }

    fun flush() {
        runBlocking(Dispatchers.IO) {
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

    companion object {
        val channels = mutableListOf<DiscordChannel>()
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
                    .url("https://discordapp.com/api/v8/channels/$id/messages")
                    .post(body)
                    .addHeader("Authorization", "Bot ${plugin.getConfigManager().getBotToken()}")
                    .build()
                val response = client.newCall(request).execute()
                val code = response.code()
                if (code == 404) {
                    Utils.logColored(plugin.getConfigManager().getPrefix(), "&cChannel with id &4$id &cdoesn't exist!", LogLevel.SEVERE)
                    channels.remove(this@DiscordChannel)
                } else if (code == 403) {
                    Utils.logColored(plugin.getConfigManager().getPrefix(),
                        "&cThe bot doesn't have access to channel with id &4$id&c!",
                        LogLevel.SEVERE
                    )
                    channels.remove(this@DiscordChannel)
                }
                response.close()

            }
        }
}