package me.deltarays.discordconsole

import org.bukkit.ChatColor

object Utils {
    fun tacc(text: String): String {
        return ChatColor.translateAlternateColorCodes('&', text)
    }
}