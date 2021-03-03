package me.deltarays.discordconsole

import me.deltarays.discordconsole.logging.DiscordChannel
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.net.URI

/**
 * ```yaml
 *  bot:
 *      token: BOTTOKEN
 *      status: online | dnd | invisible
 *      activity:
 *          text: STRING
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
        newSocket()
        socket.connect()
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
        socket = DiscordSocket(URI.create(url as String))
        socket.setHandlingPlugin(this)
    }

}