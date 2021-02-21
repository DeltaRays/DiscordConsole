package me.deltarays.discordconsole

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.net.URI

/**
 * @author DeltaRays
 * The main class of the DiscordConsole plugin
 */
class DiscordConsole : JavaPlugin() {
    val logger: Logger = LogManager.getRootLogger()
    private val configManager = ConfigManager(this)
    var serverHasStartedUp = false;

    /**
     * Gets the custom yaml configuration
     */
    override fun getConfig(): CustomConfig {
        return configManager.configuration
    }

    fun getConfigManager(): ConfigManager {
        return configManager
    }

    override fun onLoad() {
        configManager.loadConfig()
        newSocket()
        socket.connect()
        Bukkit.getPluginManager().registerEvents(Events(this), this)
    }

    override fun onEnable() {
    }


    private lateinit var socket: DiscordSocket

    /**
     * Creates a new socket
     * (Note, should only be called once as usually socket.reconnect() is used in case of disconnections.)
     */
    private fun newSocket() {
        val url = DiscordSocket.getWSUrl()
        if (url == null)
            logger.error("Error encountered while connecting to discord!")
        socket = DiscordSocket(URI.create(url))
        socket.setHandlingPlugin(this)
    }

}