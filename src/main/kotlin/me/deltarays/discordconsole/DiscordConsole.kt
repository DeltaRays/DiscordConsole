package me.deltarays.discordconsole

import com.google.gson.JsonParser
import me.deltarays.discordconsole.commands.MainCommand
import me.deltarays.discordconsole.logging.DiscordChannel
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.bukkit.Bukkit
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
 *  check-updated: BOOLEAN
 *  channels:
 *      'ID':
 *          refresh-rate: NUMBER
 *          console:
 *              format: ''
 *              commands: BOOLEAN # Whether or not messages sent in that channel get executed as console commands
 *              topic: STRING # The discord channel's topic
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
    var serverHasStartedUp = false
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

    override fun onLoad() {
        resetChannels()
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
    private fun resetChannels() {
        DiscordChannel.channels.forEachIndexed { index, discordChannel ->
            DiscordChannel.channels.removeAt(index);
            discordChannel.flush()
        }
    }

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(Events(this), this)
        Bukkit.getPluginCommand("discordconsole")?.setExecutor(MainCommand(this));
        if (configManager.shouldCheckUpdates()) {
            val (logLevel, message) = checkUpdates()
            Utils.logColored(configManager.getPrefix(), message, logLevel)
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
        if (hasInternetConnection()) {
            newSocket()
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

}