package me.deltarays.discordconsole.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.deltarays.discordconsole.DiscordConsole
import me.deltarays.discordconsole.LogLevel
import me.deltarays.discordconsole.Utils
import me.deltarays.discordconsole.discord.DiscordChannel
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.event.Listener

class MainCommand(private val plugin: DiscordConsole) : TabExecutor, Listener {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (listOf("rl", "reload").contains(args.getOrNull(0)?.toLowerCase())) {
            if (!DiscordConsole.hasInternetConnection()) {
                if (sender is Player)
                    sender.sendMessage(
                        Utils.tacc(
                            plugin.getConfigManager().getPrefix() +
                                    " &cThe plugin requires an internet connection to work!"
                        )
                    )
                else Utils.logColored(
                    plugin.getConfigManager().getPrefix(),
                    "&cThe plugin requires an internet connection to work!",
                    LogLevel.SEVERE
                )
                return true
            }
            if (sender is Player) sender.sendMessage(
                Utils.tacc(plugin.getConfigManager().getPrefix() + " &aReloading the plugin...")
            )
            else Utils.logColored(plugin.getConfigManager().getPrefix(), " &aReloading the plugin...", LogLevel.INFO)
            plugin.reload()
        } else if (listOf("checkupdate", "updatecheck", "uc", "cu").contains(args.getOrNull(0)?.toLowerCase())) {
            if (sender is Player) sender.sendMessage(
                Utils.tacc(
                    plugin.getConfigManager().getPrefix()
                ) + " &aChecking for updates..."
            )
            else Utils.logColored(plugin.getConfigManager().getPrefix(), " &aChecking for updates...", LogLevel.INFO)
            val (logLevel, message) = plugin.checkUpdates()
            if (sender is Player) sender.sendMessage(Utils.tacc(plugin.getConfigManager().getPrefix() + " " + message))
            else Utils.logColored(plugin.getConfigManager().getPrefix(), message, logLevel)
        } else if (listOf("send", "sendmessage", "message").contains(args.getOrNull(0)?.toLowerCase())) {
            val messageArgs = args.slice(1 until args.size)
            DiscordChannel.channels.forEach { channel ->
                if (channel.name.toLowerCase() == messageArgs.getOrNull(0) || channel.id == messageArgs.getOrNull(0))
                    channel.enqueueMessage(messageArgs.slice(1 until args.size).joinToString(" "))
            }
        } else if (listOf("broadcast", "announce").contains(args.getOrNull(0)?.toLowerCase())) {
            DiscordChannel.channels.forEach { channel ->
                GlobalScope.launch(Dispatchers.IO) {
                    channel.sendMessage(args.slice(1 until args.size).joinToString(" "))
                }
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (args.size == 1) return mutableListOf("reload", "checkupdate", "message", "broadcast")
        else if (listOf("send", "sendmessage", "message").contains(args.getOrNull(0)?.toLowerCase())) {
            if (args.size == 2)
                return mutableListOf("<channel id / name>")
            else if (args.size == 3)
                return mutableListOf("<message>")
        } else if (listOf("broadcast", "announce").contains(args.getOrNull(0)?.toLowerCase()))
            if (args.size == 2)
                return mutableListOf("<message>")
        return mutableListOf()
    }
}