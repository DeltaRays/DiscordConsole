//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tk.deltarays.discordconsole;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ConRef {
    public static DiscordConsole pl = DiscordConsole.getPlugin(DiscordConsole.class);

    public ConRef() {
    }

    public static String getPlPrefix() {
        String prefix = pl.getConfig().isSet("prefix") ? pl.getConfig().getString("prefix") : "&7[&6DiscordConsole&7]";
        return tacc(prefix);
    }

    public static String replaceExpressions(String text, boolean hideSensitiveInfo) {
        final int unvanishedPlayers = Math.round((float) Bukkit.getOnlinePlayers().stream().filter((p) -> !isVanished(p)).count());
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            text = PlaceholderAPI.setPlaceholders(null, text);
        }

        char[] arr = text.toCharArray();
        StringBuilder out = new StringBuilder();
        Map<String, String> placeholders = new HashMap<String, String>() {
            {
                this.put("player_count", String.valueOf(unvanishedPlayers));
                this.put("player_max", String.valueOf(Bukkit.getServer().getMaxPlayers()));
                this.put("date", (new Date()).toString());
                this.put("total_players", String.valueOf(ConRef.pl.isEnabled() ? Bukkit.getServer().getOfflinePlayers().length : "0"));
                this.put("motd", Bukkit.getServer().getMotd());
            }
        };

        for (int i = 0; i < arr.length; ++i) {
            if (arr[i] != '%') {
                out.append(arr[i]);
            } else {
                int start = i++;
                while (arr[i] != '%') i++;
                String chars = placeholders.get(text.substring(start, i));
                out.append(chars);
            }
        }

        return out.toString();
    }

    private static boolean isVanished(Player player) {
        Iterator vanishedData = player.getMetadata("vanished").iterator();

        MetadataValue meta;
        do {
            if (!vanishedData.hasNext()) {
                return false;
            }

            meta = (MetadataValue) vanishedData.next();
        } while (!meta.asBoolean());

        return true;
    }

    public static String tacc(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
