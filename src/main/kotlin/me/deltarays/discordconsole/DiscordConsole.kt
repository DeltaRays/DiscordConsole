package me.deltarays.discordconsole

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.bukkit.plugin.java.JavaPlugin

class DiscordConsole : JavaPlugin() {
    val logger: Logger = LogManager.getRootLogger();
    override fun onEnable() {
    }
}