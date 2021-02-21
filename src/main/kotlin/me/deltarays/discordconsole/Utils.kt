package me.deltarays.discordconsole

import org.bukkit.Bukkit
import org.bukkit.ChatColor

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

    fun logColored(plugin: DiscordConsole, text: String, level: LogLevel) {
        val levelMsg =
            when (level) {
                LogLevel.WARNING -> "&e[WARNING] &r"
                LogLevel.SEVERE -> "&4[SEVERE] &r"
                LogLevel.DEBUG -> "&d[DEBUG] &r"
                else -> ""
            }
        Bukkit.getConsoleSender()
            .sendMessage(tacc(String.format("%s%s %s", levelMsg, plugin.getConfigManager().getPrefix(), text)))
    }
}

enum class LogLevel {
    INFO,
    WARNING,
    SEVERE,
    DEBUG
}