package io.github.deltarays.discordconsole;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class Events implements Listener {
    DiscordConsole main;

    public Events(DiscordConsole main) {
        super();
        this.main = main;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (main.firstLoad) {
            if (p.isOp()) {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f[&bDiscordConsole&f] Welcome to DiscordConsole! To be able to send console messages to a discord channel, the plugin needs a few things, if you want to add them go to the plugins folder, DiscordConsole, and open the config.yml file.\nFurther steps await you there :)\n&bP.S. if you encounter any issues feel free to dm me on discord! (DeltaRays#0054)"));
                    }
                }, 2000);
            }
        }
        if (p.isOp()) {
            Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&f[&bDiscordConsole&f] A new DiscordConsole version was found! Download it at &bhttps://www.spigotmc.org/resources/discordconsole.77503&f."));
                    }
                }, 3000);
            });
        }
        ;
    }
}
