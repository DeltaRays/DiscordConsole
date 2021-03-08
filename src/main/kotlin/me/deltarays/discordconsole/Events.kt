package me.deltarays.discordconsole

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.ServerLoadEvent

class Events(private val plugin: DiscordConsole) : Listener {
    @EventHandler
    fun serverStartup(evt: ServerLoadEvent) {
        if (evt.type === ServerLoadEvent.LoadType.STARTUP)
            plugin.serverHasStartedUp = true
    }

    @EventHandler
    fun chat(evt: AsyncPlayerChatEvent) {
        TODO()
    }

    @EventHandler
    fun joins(evt: PlayerJoinEvent) {
        val p = evt.player
        if(p.isOp || p.hasPermission("discordconsole.admin")){

        }
        TODO()
    }

    @EventHandler
    fun quits(evt: PlayerQuitEvent) {
        TODO()
    }
}