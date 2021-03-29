package me.deltarays.discordconsole.commands

import me.deltarays.discordconsole.Utils
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CustomCommand(name: String, private val message: String) : Command(name) {
    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        val msg = if (sender is Player)
            Utils.convertPlaceholders(message, sender as Player)
        else Utils.convertPlaceholders(message, null)
        sender.sendMessage(Utils.tacc(msg))
        return true
    }
}