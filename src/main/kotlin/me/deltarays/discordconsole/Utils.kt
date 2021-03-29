package me.deltarays.discordconsole

import me.clip.placeholderapi.PlaceholderAPI
import me.deltarays.discordconsole.discord.DiscordChannel
import me.deltarays.discordconsole.discord.DiscordGuild
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.metadata.MetadataValue
import java.util.*
import kotlin.collections.HashMap
import java.awt.SystemColor.text
import java.lang.StringBuilder


/**
 * An utils object, so that I don't need to keep repeating code
 */
object Utils {
    /**
     * Shorthand for translateAlternateColorCodes
     * @param text The text in which to replace the color codes
     * @return The text with the replaced color codes
     */
    fun tacc(text: String): String {
        return ChatColor.translateAlternateColorCodes('&', text)
    }

    /**
     * Logs a colored message using colored codes to the console
     * @param level The LogLevel
     * @param prefix The prefix of the plugin for the logs
     *
     */
    fun logColored(prefix: String, text: String, level: LogLevel) {
        val levelMsg =
            when (level) {
                LogLevel.WARNING -> "&e[WARNING] &r"
                LogLevel.SEVERE -> "&4[SEVERE] &r"
                LogLevel.DEBUG -> "&d[DEBUG] &r"
                else -> ""
            }
        Bukkit.getConsoleSender()
            .sendMessage(tacc(String.format("%s%s %s", levelMsg, prefix, text)))
    }

    private val runtime = Runtime.getRuntime()
    private val byteToGB = 1073741824
    private val byteToMB = 1048576
    fun convertPlaceholders(
        initMessage: String,
        player: Player? = null,
        channel: DiscordChannel? = null,
        guild: DiscordGuild? = null
    ): String {
        var message = initMessage
        val unvanishedPlayers = Bukkit.getOnlinePlayers().filter { p ->
            !isVanished(p)
        }.size
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
            message = PlaceholderAPI.setPlaceholders(player, initMessage)

        val placeholders = HashMap<String, String>().apply {
            put("player_count", unvanishedPlayers.toString())
            put("player_max", Bukkit.getServer().maxPlayers.toString())
            put("date", Date().toString())
            if (DiscordConsole.serverHasStartedUp)
                put("total_players", Bukkit.getServer().offlinePlayers.size.toString())
            put("motd", Bukkit.getServer().motd)
            put("name", Bukkit.getServer().name)
            put("version", Bukkit.getServer().version)
            if (channel != null) {
                put("channel_name", channel.name)
                put("channel_topic", channel.topic)
                put("channel_id", channel.id)
            }
            if (guild != null) {
                put("guild_name", guild.name)
                put("guild_id", guild.id)
                put("guild_members", guild.memberCount.toString())
                put("guild_description", guild.description)
            }
        }

        val arr = message.toCharArray()
        val stringBuilder = StringBuilder()
        var i = 0
        while (i < arr.size) {
            if (arr[i] != '%') {
                stringBuilder.append(arr[i])
            } else {
                val start = i++
                while (arr[i] != '%') i++
                val chars = placeholders[message.substring(start, i)]
                stringBuilder.append(chars)
            }
            ++i
        }
        return stringBuilder.toString()
    }
}


private fun isVanished(player: Player): Boolean {
    val vanishedData = player.getMetadata("vanished").iterator()
    var metadataValue: MetadataValue
    do {
        if (!vanishedData.hasNext())
            return false
        metadataValue = vanishedData.next()
    } while (!metadataValue.asBoolean())
    return true
}

/**
 * The levels of messages that can be sent to the console
 */
enum class LogLevel {
    INFO,
    WARNING,
    SEVERE,
    DEBUG
}
