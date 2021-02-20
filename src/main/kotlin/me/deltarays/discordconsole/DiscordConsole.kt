package me.deltarays.discordconsole

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class DiscordConsole : JavaPlugin() {
    val logger: Logger = LogManager.getRootLogger();
    private lateinit var configuration: CustomConfig
    private var configFile = File(dataFolder.absolutePath, "config.yml")
    fun createConfig() {
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

}