package me.deltarays.discordconsole

import java.io.File

/**
 * @author DeltaRays
 * A class to manage the configuration file (for example to convert old config files to new versions without breaking anything)
 */
class ConfigManager(plugin: DiscordConsole) {
    lateinit var configuration: CustomConfig
    var configFile = File(plugin.dataFolder.absolutePath, "config.yml")

    fun getPrefix(): String {
        return configuration.getString("prefix", "&7[&6DiscordConsole&7]") as String
    }

    /**
     * Loads the configuration file inside the {@see configuration}
     */
    fun loadConfig() {
        if (!configFile.exists()) configFile.parentFile.mkdirs()
        configFile.createNewFile()
        configuration = CustomConfig()
        configuration.load(configFile)
    }
}