package me.deltarays.discordconsole

import org.bukkit.configuration.ConfigurationSection
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


    /**
     * Gets the current bot token.
     * @return The bot token
     */
    fun getBotToken(): String? {
        val botSection: ConfigurationSection =
            configuration.getConfigurationSection("bot") ?: configuration.createSection("bot")
        return botSection.getString("token")
    }

    fun shouldCheckUpdates(): Boolean {
        return configuration.getBoolean("check-updates", true)
    }

    /**
     * Gets the ConfigurationSection containing the channels.
     */
    private fun getChannels(): ConfigurationSection {
        var section = configuration.getConfigurationSection("channels")
        if (section == null) section = configuration.createSection("channels")
        return section
    }

    /**
     * Gets a channel's ConfigurationSection from its id.
     */
    fun getChannel(id: String): ConfigurationSection {
        val channels = getChannels()
        return channels.getConfigurationSection(id) ?: channels.createSection(id)
    }
}