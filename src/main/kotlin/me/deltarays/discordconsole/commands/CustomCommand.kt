package me.deltarays.discordconsole.commands

import me.deltarays.discordconsole.Utils
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class CustomCommand(name: String, private val message: String) : Command(name) {
    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        sender.sendMessage(Utils.tacc(message))
        return true
    }
}