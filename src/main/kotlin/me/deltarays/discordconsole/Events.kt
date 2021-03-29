package me.deltarays.discordconsole

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.ServerLoadEvent

class Events(private val plugin: DiscordConsole) : Listener {
    @EventHandler
    fun serverStartup(evt: ServerLoadEvent) {
        DiscordConsole.serverHasStartedUp = true
    }

    @EventHandler
    fun chat(evt: AsyncPlayerChatEvent) {
        TODO()
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