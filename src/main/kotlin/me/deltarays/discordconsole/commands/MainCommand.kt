package me.deltarays.discordconsole.commands

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
            val channel = DiscordChannel.channels.find { c ->
                c.name.toLowerCase() == messageArgs.getOrNull(0) || c.id == messageArgs.getOrNull(0)
            }
            if (channel == null) {
                sender.sendMessage(
                    Utils.tacc(
                        plugin.getConfigManager()
                            .getPrefix() + " &cThat channel is not in the plugin's list of channels!"
                    )
                )
                return true
            }
            channel.enqueueMessage(messageArgs.slice(1 until messageArgs.size).joinToString(" "))
            sender.sendMessage(
                Utils.tacc(
                    plugin.getConfigManager().getPrefix() + " &aSent message to the channel ${channel.name}!"
                )
            )

        } else if (listOf("broadcast", "announce").contains(args.getOrNull(0)?.toLowerCase())) {
            DiscordChannel.channels.forEach { channel ->
                channel.enqueueMessage(args.slice(1 until args.size).joinToString(" "))
            }
            sender.sendMessage(
                Utils.tacc(
                    plugin.getConfigManager().getPrefix() + " &aBroadcast message to all discord channels!"
                )
            )
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.size == 1) return mutableListOf("reload", "checkupdate", "message", "broadcast")
        else if (listOf("send", "sendmessage", "message").contains(args.getOrNull(0)?.toLowerCase())) {
            if (args.size == 2) {
                val list = mutableListOf<String>()
                DiscordChannel.channels.forEach { channel ->
                    list.add(channel.name)
                    list.add(channel.id)
                }
                list.add("<id/name>")
                return list
            } else if (args.size == 3)
                return listOf("<message>")
        } else if (listOf("broadcast", "announce").contains(args.getOrNull(0)?.toLowerCase()))
            if (args.size == 2)
                return listOf("<message>")
        return mutableListOf()
    }
}