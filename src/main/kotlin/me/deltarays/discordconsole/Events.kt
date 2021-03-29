package me.deltarays.discordconsole

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.deltarays.discordconsole.discord.DiscordChannel
import me.deltarays.discordconsole.logging.LogType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.ServerLoadEvent
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class Events(private val plugin: DiscordConsole) : Listener {
    @EventHandler
    fun serverStartup(evt: ServerLoadEvent) {
        DiscordConsole.serverHasStartedUp = true
    }

    @EventHandler
    fun chat(evt: AsyncPlayerChatEvent) {
        fun parse(str: String, player: Player, message: String, format: String): String {
            return Utils.convertPlaceholders(str, player = player)
                .replace(Regex("%chat_player_name%", RegexOption.IGNORE_CASE), player.name)
                .replace(Regex("%chat_message%", RegexOption.IGNORE_CASE), message)
                .replace(Regex("%chat_format%", RegexOption.IGNORE_CASE), format)
                .replace(Regex("%date\\[(.*?)]%", RegexOption.IGNORE_CASE)) { e ->
                    val dateFormat = SimpleDateFormat(e.groupValues.getOrElse(0) { "HH:mm:ss" });
                    dateFormat.format(Date())
                }
        }
        for (channel in DiscordChannel.channels) {
            if (channel.types.containsValue(LogType.CHAT)) {
                val channelSection = plugin.getConfigManager().getChannel(channel.id)
                val chatSection = channelSection.getConfigurationSection("chat") ?: channelSection.createSection("chat")
                val chatFormat = channel.getMessageFormat(LogType.CHAT)
                val result = parse(chatFormat, evt.player, evt.message, evt.format)
                val chatFilter = chatSection.getString("filter") ?: ""
                if (chatFilter.isNotEmpty() && !Pattern.compile(chatFilter).matcher(result).find())
                    return
                GlobalScope.launch(Dispatchers.IO) {
                    channel.sendMessage(result)
                }
            }
        }
    }

    @EventHandler
    fun joins(evt: PlayerJoinEvent) {
        val p = evt.player
        if (p.isOp || p.hasPermission("discordconsole.admin")) {
            if (DiscordConsole.isFirstLoad)
                p.sendMessage(Utils.tacc("&7Thanks for installing DiscordConsole!\n To understand how to use it make sure to check https://github.com/DeltaRays/DiscordConsole/wiki out!"))
            if (plugin.getConfigManager().shouldCheckUpdates())
                p.sendMessage(Utils.tacc(plugin.checkUpdates().second))
        }
        TODO()
    }

    @EventHandler
    fun quits(evt: PlayerQuitEvent) {
        TODO()
    }

    @EventHandler
    fun deaths(evt: PlayerDeathEvent) {
        TODO()
    }


}