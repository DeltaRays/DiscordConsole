package me.deltarays.discordconsole

import com.google.gson.JsonParser
import me.deltarays.discordconsole.commands.CustomCommand
import me.deltarays.discordconsole.commands.MainCommand
import me.deltarays.discordconsole.discord.DiscordChannel
import me.deltarays.discordconsole.discord.DiscordGuild
import me.deltarays.discordconsole.discord.DiscordSocket
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.bukkit.Bukkit
import org.bukkit.command.CommandMap
import org.bukkit.plugin.java.JavaPlugin
import java.net.NetworkInterface
import java.net.URI

/**
 * ```yaml
 *  bot:
 *      token: BOTTOKEN
 *      status: online | dnd | invisible
 *      activity:
 *          name: STRING
 *          type: playing | other stuff
 *
 *  prefix: "&7[&6DiscordConsole&7]"
 *  check-updates: BOOLEAN
 *  channels:
 *      'ID':
 *          refresh-rate: NUMBER
 *          topic: 'STRING'
 *          console:
 *              format: ''
 *              commands-enabled: BOOLEAN # Whether or not messages sent in that channel get executed as console commands
 *              send-startup: BOOLEAN # Whether or not to send startup messages
 *              filter: REGEX
 *          chat:
 *              format: ''
 *              topic: STRING
 *              filter: REGEX
 *          joins:
 *              format: ''
 *              topic: STRING
 *              filter: REGEX
 *          quits:
 *              format: ''
 *              topic: STRING
 *              filter: REGEX
 *
 *  commands:
 *      NAME: MESSAGE
 *      #discordlink: https://discord.gg/WSaWztJ
 *
 *
 *  discord commands:
 *      NAME: MESSAGE
 *
 *  debug: BOOLEAN
 * ```
 */


/**
 * The main class of the DiscordConsole plugin
 * @author DeltaRays
 */
class DiscordConsole : JavaPlugin() {
    private val logger: Logger = LogManager.getRootLogger()
    private val configManager = ConfigManager(this)
    private val client = OkHttpClient()
    private val parser = JsonParser()

    /**
     * Gets the custom yaml configuration
     */
    override fun getConfig(): CustomConfig {
        return configManager.configuration
    }

    fun getConfigManager(): ConfigManager {
        return configManager
    }

    fun exposeCommandMap() {
        val field = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
        field.isAccessible = true
        this.commandMap = field.get(Bukkit.getServer()) as CommandMap?
    }

    override fun onLoad() {
        resetChannelsGuilds()
        exposeCommandMap()
        configManager.loadConfig()
        if (hasInternetConnection()) {
            newSocket()
            socket.connect()
        } else {
            Utils.logColored(
                configManager.getPrefix(),
                "&4The plugin requires an internet connection to work!",
                LogLevel.SEVERE
            )
        }
    }

    /**
     * Removes the discord channels and bulk sends all remaining messages
     */
    private fun resetChannelsGuilds() {
        DiscordChannel.channels.forEach {
            it.destroy()
        }
        for (guild in DiscordGuild.guilds) {
            guild.destroy()
        }
    }

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(Events(this), this)
        Bukkit.getPluginCommand("discordconsole")?.setExecutor(MainCommand(this));
        if (configManager.shouldCheckUpdates()) {
            val (logLevel, message) = checkUpdates()
            Utils.logColored(configManager.getPrefix(), message, logLevel)
        }
        if (isFirstLoad) {
            Utils.logColored(
                configManager.getPrefix(),
                "&7Thanks for installing DiscordConsole!\n" +
                        " To understand how to use it make sure to check https://github.com/DeltaRays/DiscordConsole/wiki out!",
                LogLevel.INFO
            )
        }
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
        if (this::socket.isInitialized && !(socket.isClosed || socket.isClosing)) {
            socket.close(3334)
            socket.job.cancel()
        }
        socket = DiscordSocket(URI.create(url as String))
        socket.setHandlingPlugin(this)
    }

    fun reload() {
        this.configManager.loadConfig()
        registerCustomCommands()

        if (hasInternetConnection()) {
            newSocket()
            socket.connect()
        } else {
            Utils.logColored(
                configManager.getPrefix(),
                "&4The plugin requires an internet connection to work!",
                LogLevel.SEVERE
            )
        }
    }

    companion object {
        fun hasInternetConnection(): Boolean {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (interfac in interfaces) {
                if (interfac.isUp || interfac.isLoopback) return true
            }
            return false
        }

        var serverHasStartedUp = false
        var isFirstLoad = false
    }

    fun checkUpdates(): Pair<LogLevel, String> {
        val request =
            Request.Builder().url("https://api.github.com/repos/DeltaRays/DiscordConsole/releases").build()
        val response = client.newCall(request).execute()
        val parsed = parser.parse(response.body()?.string()).asJsonArray
        response.close()
        val versions = mutableListOf<String>()
        parsed.forEach { release ->
            val parsedRelease = release.asJsonObject
            if (!(parsedRelease.get("draft").asBoolean && parsedRelease.get("prerelease").asBoolean))
                versions.add(parsedRelease.get("tag_name").asString)
        }
        return if (!versions.contains(description.version)) {
            Pair(
                LogLevel.WARNING,
                "&cApparently you have a plugin version that doesn't exist in the releases list. Either you're in an experimental build or something is wrong. If you're not in an experimental build then you should download the latest release here: &b&nhttps://www.spigotmc.org/resources/discordconsole.77503/"
            )
        } else if (versions.getOrNull(0) === description.version) {
            Pair(LogLevel.INFO, "&aYou're using the latest DiscordConsole version!")
        } else Pair(
            LogLevel.INFO,
            "&7You're &6${versions.indexOf(description.version)}&7 versions behind! (Latest version: &6${
                versions.getOrNull(0)
            }&7)"
        )
    }

    var commandMap: CommandMap? = null
    fun registerCustomCommands() {
        val cmdSection = configManager.getCustomCmdSection()
        cmdSection.getKeys(false).forEach { key ->
            val value = cmdSection.get(key).toString()
            commandMap?.register(key, "discordconsole", CustomCommand(key, Utils.convertPlaceholders(value)))
            Bukkit.getOnlinePlayers().forEach { player ->
                player.updateCommands()
            }
        }
    }

}