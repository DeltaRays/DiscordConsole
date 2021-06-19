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
        if (!configFile.exists()) {
            configFile.parentFile.mkdirs()
            setConfigurationDefaults()
            DiscordConsole.isFirstLoad = true
        }
        configFile.createNewFile()
        configuration = CustomConfig()
        configuration.load(configFile)
    }

    fun setConfigurationDefaults() {
        configuration.set(
            "bot.token",
            "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
            "The discord bot's token"
        )
        configuration.set("bot.status", "online", "The bot's status (online, dnd, idle, invisible)")
        configuration.set("bot.activity.name", "on minecraft", "The name of the bot's status")
        configuration.set(
            "bot.activity.type",
            "playing",
            "The type of the bot's status (game, listening, watching, etc.)"
        )
        configuration.set("prefix", "&7[&6DiscordConsole&7]", "The plugin's console prefix")
        configuration.set("check-updates", true, "Whether the server should check for updates")

        configuration.addRaw(
            "channels:\n" +
                    "    'ID':\n" +
                    "        refresh-rate: NUMBER\n" +
                    "        topic: 'STRING'\n" +
                    "        console:\n" +
                    "            active: true\n" +
                    "            format: ''\n" +
                    "            commands-enabled: BOOLEAN # Whether or not messages sent in that channel get executed as console commands\n" +
                    "            send-startup: BOOLEAN # Whether or not to send startup messages\n" +
                    "            filter: REGEX\n" +
                    "        chat:\n" +
                    "            active: true\n" +
                    "            format: ''\n" +
                    "            filter: REGEX\n" +
                    "            discord-minecraft:\n" +
                    "                enabled: BOOLEAN # Whether or not anything sent in that channel will be sent as a chat message\n" +
                    "                format: ''\n" +
                    "        joins:\n" +
                    "            active: true\n" +
                    "            format: ''\n" +
                    "            filter: REGEX\n" +
                    "        quits:\n" +
                    "            active: true\n" +
                    "            format: ''\n" +
                    "            filter: REGEX\n" +
                    "        deaths:\n" +
                    "            active: true\n" +
                    "            format: ''\n" +
                    "            filter: REGEX\n" +
                    "        startup:\n" +
                    "            active: true\n" +
                    "            format: \"The server has started up!\"\n" +
                    "        shutdown:\n" +
                    "            active: true\n" +
                    "            format: \"The server has stopped!\"" +
                    "\n\n\n"
        )

        configuration.addRaw(
            "commands:\n" +
                    "    #Structured like NAME: MESSAGE\n" +
                    "    discordlink: \"https://discord.gg/WSaWztJ\"\n" +
                    "\n\n" +
                    "discord-commands:\n" +
                    "    NAME: MESSAGE" +
                    "\n\n"
        )

        configuration.set(
            "debug",
            true,
            "Whether the server should send messages (useful if something doesn't work and the developer needs more information)"
        )

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