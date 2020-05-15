package tk.deltarays.discordconsole;

import me.clip.placeholderapi.PlaceholderAPI;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ConRef { //Config Refiner
    public static DiscordConsole pl = DiscordConsole.getPlugin(DiscordConsole.class);

    public static String getPlPrefix() {
        String prefix = pl.getConfig().isSet("prefix") ? pl.getConfig().getString("prefix") : "&7[&6DiscordConsole&7]";
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    public static String replaceExpressions(String text, boolean hideSensitiveInfo) {
        int unvanishedPlayers = Math.round(Bukkit.getOnlinePlayers().stream().filter(p -> !isVanished(p)).count());
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
            text = PlaceholderAPI.setPlaceholders(null, text);
        char[] arr = text.toCharArray();
        StringBuilder out = new StringBuilder();
        Map<String, String> placeholders = new HashMap<String, String>() {{
            put("%player_count%", String.valueOf(unvanishedPlayers));
            put("%player_max%", String.valueOf(Bukkit.getServer().getMaxPlayers()));
            put("%date%", new Date().toString());
            put("%total_players%", String.valueOf(pl.isEnabled() ? Bukkit.getServer().getOfflinePlayers().length : "0"));
            put("%motd%", Bukkit.getServer().getMotd());
        }};
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == '%') {
                int start = i++;
                while (arr[i] != '%') i++;
                i++;
                String chars = placeholders.get(text.substring(start, i));
                out.append(chars);
                if (arr.length != i) {
                    out.append(arr[i]);
                }
            } else {
                out.append(arr[i]);
            }
        }
        return out.toString();
    }

    private static boolean isVanished(Player player) {
        for (MetadataValue meta : player.getMetadata("vanished")) {
            if (meta.asBoolean()) return true;
        }
        return false;
    }
}
