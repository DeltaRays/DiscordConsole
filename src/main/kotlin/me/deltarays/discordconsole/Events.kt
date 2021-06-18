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
import org.bukkit.event.player.PlayerAdvancementDoneEvent
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
TODO("Server startup")
    }


    fun parseChat(str: String, player: Player, message: String, format: String): String {
        return Utils.convertPlaceholders(str, player = player)
            .replace(Regex("%chat_player_name%", RegexOption.IGNORE_CASE), player.name)
            .replace(Regex("%chat_message%", RegexOption.IGNORE_CASE), message)
            .replace(Regex("%chat_format%", RegexOption.IGNORE_CASE), format)
            .replace(Regex("%date\\[(.*?)]%", RegexOption.IGNORE_CASE)) { e ->
                val dateFormat = SimpleDateFormat(e.groupValues.getOrElse(0) { "HH:mm:ss" });
                dateFormat.format(Date())
            }
    }

    @EventHandler
    fun chat(evt: AsyncPlayerChatEvent) {

        for (channel in DiscordChannel.channels) {
            if (channel.types.containsValue(LogType.CHAT)) {
                val channelSection = plugin.getConfigManager().getChannel(channel.id)
                val chatSection = channelSection.getConfigurationSection("chat") ?: channelSection.createSection("chat")
                val chatFormat = channel.getMessageFormat(LogType.CHAT)
                val result = parseChat(chatFormat, evt.player, evt.message, evt.format)
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
        for (channel in DiscordChannel.channels) {
            if (channel.types.contains("JOINS")) {
                val fmt: String = channel.getMessageFormat(LogType.JOINS)
                val channelSection = plugin.getConfigManager().getChannel(channel.id)
                val joinSection =
                    channelSection.getConfigurationSection("joins") ?: channelSection.createSection("joins")
                val formatted = Utils.convertPlaceholders(fmt, p).replace(Regex("\\{player}", RegexOption.IGNORE_CASE), p.name)
                val filterStr = joinSection.getString("filter") ?: ""
                if (filterStr.isNotEmpty()) {
                    val filter = Pattern.compile(filterStr)
                    if (!filter.matcher(formatted).find()) continue
                }
                channel.enqueueMessage(formatted)
            }
        }
    }

    @EventHandler
    fun quits(evt: PlayerQuitEvent) {
        val p = evt.player
        for (channel in DiscordChannel.channels) {
            if (channel.types.contains("QUITS")) {
                val fmt: String = channel.getMessageFormat(LogType.QUITS)
                val channelSection = plugin.getConfigManager().getChannel(channel.id)
                val quitSection =
                    channelSection.getConfigurationSection("quits") ?: channelSection.createSection("quits")
                val formatted = Utils.convertPlaceholders(fmt, p).replace(Regex("\\{player}", RegexOption.IGNORE_CASE), p.name)
                val filterStr = quitSection.getString("filter") ?: ""
                if (filterStr.isNotEmpty()) {
                    val filter = Pattern.compile(filterStr)
                    if (!filter.matcher(formatted).find()) continue
                }
                channel.enqueueMessage(formatted)
            }
        }
    }

    @EventHandler
    fun deaths(evt: PlayerDeathEvent) {
        val p = evt.entity
        for (channel in DiscordChannel.channels) {
            if (channel.types.contains("DEATH")) {
                val fmt: String = channel.getMessageFormat(LogType.DEATH)
                val channelSection = plugin.getConfigManager().getChannel(channel.id)
                val quitSection =
                    channelSection.getConfigurationSection("death") ?: channelSection.createSection("death")
                val formatted = Utils.convertPlaceholders(fmt, p).replace(Regex("\\{player}", RegexOption.IGNORE_CASE), p.name)
                    .replace(Regex("\\{message}", RegexOption.IGNORE_CASE), evt.deathMessage ?: "")
                val filterStr = quitSection.getString("filter") ?: ""
                if (filterStr.isNotEmpty()) {
                    val filter = Pattern.compile(filterStr)
                    if (!filter.matcher(formatted).find()) continue
                }
                channel.enqueueMessage(formatted)
            }
        }
    }

}