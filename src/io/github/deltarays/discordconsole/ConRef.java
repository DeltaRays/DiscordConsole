package io.github.deltarays.discordconsole;

import me.clip.placeholderapi.PlaceholderAPI;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import java.lang.management.ManagementFactory;
import java.util.Date;

public class ConRef { //Config Refiner
    public static DiscordConsole pl = DiscordConsole.getPlugin(DiscordConsole.class);

    public static String getPlPrefix() {
        String prefix = pl.getConfig().isSet("prefix") ? pl.getConfig().getString("prefix") : "&7[&6DiscordConsole&7]";
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    public static String replaceExpressions(String text, boolean hideSensitiveInfo) {
        int unvanishedPlayers = 0;
        if (hideSensitiveInfo) { //Supported by PremiumVanish, SuperVanish, VanishNoPacket and a few more plugins (I don't know which)
            for (Object p : Bukkit.getServer().getOnlinePlayers().toArray()) {
                Player player = (Player) p;
                if (!isVanished(player)) unvanishedPlayers++;
            }
        } else unvanishedPlayers = Bukkit.getServer().getOnlinePlayers().size();
        text = text.replaceAll("%player_count%", String.valueOf(unvanishedPlayers));
        text = text.replaceAll("%player_max%", String.valueOf(Bukkit.getServer().getMaxPlayers()));
        text = text.replaceAll("%date%", new Date().toString());
        if (pl.isEnabled()) {
            text = text.replaceAll("%total_players%", String.valueOf(Bukkit.getServer().getOfflinePlayers().length));
        } else {
            text = text.replaceAll("%total_players%", "0");
        }
        text = text.replaceAll("%uptime%", DurationFormatUtils.formatDuration(ManagementFactory.getRuntimeMXBean().getUptime(), "**D:mm:ss**", true));
        text = text.replaceAll("%motd%", Bukkit.getServer().getMotd());
        text = text.replaceAll("%used_memory%", String.valueOf(Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory()));
        text = text.replaceAll("%max_memory%", String.valueOf(Runtime.getRuntime().maxMemory()));
        text = text.replaceAll("%used_memory_gb%", String.valueOf(Math.round((Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory()) / 102.4) / 10));
        text = text.replaceAll("%max_memory_gb%", String.valueOf(Math.round(Runtime.getRuntime().maxMemory() / 102.4) / 10));
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
            text = PlaceholderAPI.setPlaceholders(null, text);
        return text;
    }

    private static boolean isVanished(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }
}
