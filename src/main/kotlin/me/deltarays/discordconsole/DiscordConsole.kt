package me.deltarays.discordconsole

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.net.URI

class DiscordConsole : JavaPlugin() {
    val logger: Logger = LogManager.getRootLogger();
    private lateinit var configuration: CustomConfig
    private var configFile = File(dataFolder.absolutePath, "config.yml")
    fun loadConfig() {
        if (!configFile.exists()) configFile.parentFile.mkdirs()
        configFile.createNewFile()
        configuration = CustomConfig()
        configuration.load(configFile)
    }

    override fun getConfig(): YamlConfiguration {
        return configuration
    }



    override fun onEnable() {
    }

    private lateinit var socket: DiscordSocket
    fun newSocket() {
        val url = DiscordSocket.getWSUrl()
        if (url == null)
            logger.error("Error encountered while connecting to discord!")
        socket = DiscordSocket(URI.create(url as String))
        socket.setHandlingPlugin(this)
    }

}