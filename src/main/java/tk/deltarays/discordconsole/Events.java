//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tk.deltarays.discordconsole;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class Events implements Listener {
    DiscordConsole main;

    public Events(DiscordConsole main) {
        this.main = main;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (this.main.firstLoad && player.isOp()) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    player.sendMessage(ConRef.getPlPrefix() + ConRef.tacc(" Welcome to DiscordConsole! To be able to send console messages to a discord channel, the plugin needs a few things, if you want to add them go to the plugins folder, DiscordConsole, and open the config.yml file. Further steps await you there. \nP.S. if you encounter any issues feel free to dm me on discord! (&6DeltaRays#0054&7)"));
                }
            }, 2000L);
        }

        if (player.isOp() && this.main.hasInternetConnection()) {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = (new Builder()).url("https://api.github.com/repos/DeltaRays/DiscordConsole/releases").build();
                Response response = client.newCall(request).execute();
                JSONParser jsonParser = new JSONParser();

                assert response.body() != null;

                JSONArray releases = (JSONArray) jsonParser.parse(response.body().string());
                response.close();
                JSONObject latestRelease = (JSONObject) releases.get(0);
                ArrayList versions = new ArrayList();
                Object[] arrReleases = releases.toArray();

                for (Object t : arrReleases) {
                    JSONObject release = (JSONObject) t;
                    if (!(Boolean) release.get("draft") && !(Boolean) release.get("prerelease")) {
                        versions.add(release.get("tag_name"));
                    }
                }

                if ((!this.main.getConfig().isSet("checkForUpdates") || this.main.getConfig().getBoolean("checkForUpdates")) && !latestRelease.get("tag_name").toString().equalsIgnoreCase(this.main.getDescription().getVersion())) {
                    player.sendMessage(ConRef.tacc(String.format("&7You're &6%s &7version(s) behind! (Latest version: &6%s&7) &7Download it here: &6%s", versions.indexOf(this.main.getDescription().getVersion()), latestRelease.get("tag_name"), "https://www.spigotmc.org/resources/discordconsole.77503/")));
                }
            } catch (Exception e) {
                this.main.getLogger().warning("Error encountered while checking for versions!");
                e.printStackTrace();
            }
        } else if (!this.main.hasInternetConnection()) {
            this.main.getLogger().severe("Disabling the plugin! No internet connection was found! Unable to interact with discord!");
            this.main.getServer().getPluginManager().disablePlugin(this.main);
        }

    }
}
