package me.deltarays.discordconsole.discord

import com.google.gson.JsonParser
import kotlinx.coroutines.*
import me.deltarays.discordconsole.DiscordConsole
import me.deltarays.discordconsole.LogLevel
import me.deltarays.discordconsole.Utils
import okhttp3.OkHttpClient
import okhttp3.Request

class DiscordGuild(val id: String, private val plugin: DiscordConsole) {
    private val getDataJob: Job

    private val client = OkHttpClient()
    var channels = mutableListOf<DiscordChannel>()
    private val parser = JsonParser()

    var hasData = false
    lateinit var name: String;
    var memberCount: Int? = null; // approximate_member_count in json
    var description: String? = null;

    fun destroy() {
        getDataJob.cancel()
        guilds.removeIf { guild -> guild == this }
    }

    init {
        guilds.add(this)
        getDataJob = GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                val request = Request.Builder()
                    .url("$BASE_API_URL/guilds/$id?with_counts=true")
                    .get()
                    .addHeader("Authorization", "Bot ${plugin.getConfigManager().getBotToken()}")
                    .addHeader("Accept", "application/json")
                    .build()
                val response = client.newCall(request).execute()
                val code = response.code()
                if (code == 404) {
                    Utils.logColored(
                        plugin.getConfigManager().getPrefix(),
                        "&cGuild with id &4$id &cdoesn't exist!",
                        LogLevel.SEVERE
                    )
                    DiscordChannel.channels.removeIf { channel ->
                        channel.guild?.id == id
                    }
                } else if (code == 403) {
                    Utils.logColored(
                        plugin.getConfigManager().getPrefix(),
                        "&cThe bot doesn't have access to guild with id &4$id&c!",
                        LogLevel.SEVERE
                    )
                    DiscordChannel.channels.removeIf { channel ->
                        channel.guild?.id == id
                    }
                }
                val json = parser.parse(response.body()?.string()).asJsonObject
                response.close()
                hasData = true
                name = json.get("name").asString
                description = if (json.get("description").isJsonNull) null else json.get("description").asString
                memberCount = json.get("approximate_member_count").asInt
                delay(20000)
            }
        }
    }


    companion object {
        val guilds = mutableListOf<DiscordGuild>()
    }
}