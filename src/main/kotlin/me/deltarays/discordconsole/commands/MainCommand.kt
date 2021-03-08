package me.deltarays.discordconsole.commands

import com.google.gson.JsonParser
import me.deltarays.discordconsole.DiscordConsole
import me.deltarays.discordconsole.LogLevel
import me.deltarays.discordconsole.Utils
import me.deltarays.discordconsole.logging.DiscordChannel
import okhttp3.OkHttpClient
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.event.Listener

class MainCommand(private val plugin: DiscordConsole) : TabExecutor, Listener {
    val client = OkHttpClient()
    val parser = JsonParser()

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
            val message = args.slice(1..args.size).joinToString(" ")
            DiscordChannel.channels.forEach { channel ->
                channel.enqueueMessage(message)
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
        if (args.size == 1) return mutableListOf("reload", "checkupdate", "sendmessage")
        return mutableListOf()
    }
}