package me.deltarays.discordconsole

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class Events(private val plugin: DiscordConsole) : Listener {
    @EventHandler
    fun serverStartup() {
        plugin.serverHasStartedUp = true
    }
}