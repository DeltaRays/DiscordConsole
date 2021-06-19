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
            DiscordConsole.isFirstLoad = true
        }
        configFile.createNewFile()
        configuration = CustomConfig()
        configuration.load(configFile)
        if(DiscordConsole.isFirstLoad){
            setConfigurationDefaults()
            saveConfig()
        }
    }

    fun saveConfig(){
        configuration.save(configFile)
    }

    fun setConfigurationDefaults() {
        configuration.addRaw(0,
            "bot:\n" +
                "    token: \"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\"\n" +
                "    status: online # The bot's status (online, dnd, idle, invisible)\n" +
                "    activity:\n" +
                "        name: STRING # The name of the bot's status\n" +
                "        type: playing # The type of the bot's status (game, listening, watching, etc.)\n" +
                "\n" +
                "prefix: \"&7[&6DiscordConsole&7]\" # The plugin's console prefix\n" +
                "check-updates: true # Whether the server should check for updates\n" +
                "channels:\n" +
                "    'ID': # The channel's id\n" +
                "        refresh-rate: 1000 # Every how many milliseconds messages should be sent\n" +
                "        topic: 'Console channel' # The channel's topic\n" +
                "        console:\n" +
                "            active: true\n" +
                "            format: '[{date[HH:mm:ss]}] [{thread}/{level}] {message}' # How the console messages are structured\n" +
                "            commands-enabled: true # Whether or not messages sent in that channel get executed as console commands\n" +
                "            send-startup: true # Whether or not to send startup messages\n" +
                "           filter: '' # The regular expression to filter each message\n" +
                "       chat:\n" +
                "           active: false\n" +
                "           format: '{player}: {message}'\n" +
                "           filter: '' # The regular expression to filter each message\n" +
                "           minecraft-discord:\n" +
                "               enabled: BOOLEAN # Whether or not anything sent in that channel will be sent as a chat message\n" +
                "               format: ''\n" +
                "       joins:\n" +
                "           active: false\n" +
                "           format: '{player} joined the server'\n" +
                "           filter: '' # The regular expression to filter each message\n" +
                "       quits:\n" +
                "           active: false\n" +
                "           format: '{player} left the server'\n" +
                "           filter: '' # The regular expression to filter each message\n" +
                "       deaths:\n" +
                "           active: false\n" +
                "           format: '{message}'\n" +
                "           filter: '' # The regular expression to filter each message\n" +
                "       startup:\n" +
                "           active: true\n" +
                "           format: \"The server has started up!\"\n" +
                "       shutdown:\n" +
                "           active: true\n" +
                "           format: \"The server has stopped!\"\n" +
                "\n" +
                "commands:\n" +
                "    #NAME: MESSAGE\n" +
                "    discordlink: https://discord.gg/WSaWztJ\n" +
                "\n" +
                "\n" +
                "discord-commands:\n" +
                "     #NAME: MESSAGE\n" +
                "\n" +
                "debug: BOOLEAN"
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