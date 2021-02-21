package me.deltarays.discordconsole

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerLoadEvent

class Events(private val plugin: DiscordConsole) : Listener {
    @EventHandler
    fun serverStartup(evt: ServerLoadEvent) {
        if (evt.type === ServerLoadEvent.LoadType.STARTUP)
            plugin.serverHasStartedUp = true
    }
}