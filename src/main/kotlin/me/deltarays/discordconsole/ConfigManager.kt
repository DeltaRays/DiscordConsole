package me.deltarays.discordconsole

import org.bukkit.configuration.ConfigurationSection
import java.io.File
import java.util.*
import kotlin.math.roundToInt

/**
 * @author DeltaRays
 * A class to manage the configuration file (for example to convert old config files to new versions without breaking anything)
 */
class ConfigManager(var plugin: DiscordConsole) {
    lateinit var configuration: CustomConfig
    private var configFile = File(plugin.dataFolder.absolutePath, "config.yml")

    fun getPrefix(): String {
        return configuration.getString("prefix", "&7[&6DiscordConsole&7]") as String
    }

    /**
     * Loads the configuration file inside the {@see configuration}
     */
    fun loadConfig() {
        if (!configFile.exists()) {
            configFile.parentFile.mkdirs()
            configFile.createNewFile()
            DiscordConsole.isFirstLoad = true
            recreateDefaultFile()

        }
        configFile.createNewFile()
        configuration = CustomConfig()
        configuration.load(configFile)
        updateConfig()
    }

    fun saveConfig() {
        configuration.save(configFile)
    }

    private fun getConfigVersion(): String? {
        return configuration.getString("version")
    }

    /**
     * Updates the config from a version to a new one
     */
    private fun updateConfig() {
        if (getConfigVersion() == "1.4.0") return // TODO for future versions: change this
        if (getConfigVersion() == null) {
            configuration.save(File(plugin.dataFolder, "config_old-${(Date().time / 1000.0).roundToInt()}.yml"))
            val botToken = configuration.getString("botToken")
            val channelRefreshRate =
                configuration.getLong("channelRefreshRate") * 1000 // It used to be in seconds, now it's in milliseconds
            val consoleCommandsEnabled = configuration.getBoolean("consoleCommandsEnabled")
            val sendStartupMessages = configuration.getBoolean("sendStartupMessages")
            val botStatus = configuration.getString("botStatus")
            val botActivity = configuration.getString("botActivity")
            val botstatusText = configuration.getString("botStatusText")
            val debug = configuration.getBoolean("debug")
            val prefix = configuration.getString("prefix")
            val channels = configuration.getConfigurationSection("channels")
            val channelMap = hashMapOf<String, Pair<String, String>>()
            channels!!.getKeys(false).forEach { key ->
                val channel = channels.getConfigurationSection(key)!!
                val filter = channel.getString("filter", "")!!
                val topic = channel.getString("topic", "")!!
                channelMap[key] = Pair(filter, topic)
            }
            recreateDefaultFile()
            configuration = CustomConfig()
            configuration.load(configFile)
            configuration.apply {
                set("bot.token", botToken)
                set("bot.status", botStatus)
                set("version", "1.4.0")
                set("bot.activity.type", botActivity)
                set("bot.activity.name", botstatusText)
                set("prefix", prefix)
                set("debug", debug)
                channelMap.keys.forEach { key ->
                    val value = channelMap[key]!!
                    set("channels.$key.refreshRate", channelRefreshRate)
                    set("channels.$key.topic", value.second)
                    set("channels.$key.console.active", true)
                    set("channels.$key.console.format", "[{date[HH:mm:ss]}] [{thread}/{level}] {message}")
                    set("channels.$key.console.commands-enabled", consoleCommandsEnabled)
                    set("channels.$key.console.send-startup", sendStartupMessages)
                    set("channels.$key.console.filter", value.first)
                }
            }
            saveConfig()

        }
        // TODO for future versions:
        // If we make more config changes just add another check here for the old version
        // so that it automatically passes through multiple versions without us doing anything manually
    }

    private fun recreateDefaultFile() {
        val defaultConfigFile = plugin.getResource("cfg.yml")!!
        configFile.delete()
        configFile.createNewFile()
        configFile.writeBytes(defaultConfigFile.readBytes())
    }

    fun getBotSection(): ConfigurationSection {
        return configuration.getConfigurationSection("bot") ?: configuration.createSection("bot")
    }

    /**
     * Gets the current bot token.
     * @return The bot token
     */
    fun getBotToken(): String? {
        val botSection = getBotSection()
        return botSection.getString("token")
    }

    fun shouldCheckUpdates(): Boolean {
        return configuration.getBoolean("check-updates", true)
    }

    /**
     * Gets the ConfigurationSection containing the channels.
     */
    fun getChannels(): ConfigurationSection {
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

    fun getCustomCmdSection(): ConfigurationSection {
        return configuration.getConfigurationSection("commands") ?: configuration.createSection("commands")
    }

    fun getCustomDiscordCmdSection(): ConfigurationSection {
        return configuration.getConfigurationSection("discord-commands")
            ?: configuration.createSection("discord-commands")
    }

}