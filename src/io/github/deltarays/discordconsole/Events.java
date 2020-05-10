package io.github.deltarays.discordconsole;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
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
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (main.firstLoad) {
            if (player.isOp()) {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        player.sendMessage(ConRef.getPlPrefix() + " Welcome to DiscordConsole! To be able to send console messages to a discord channel, the plugin needs a few things, if you want to add them go to the plugins folder, DiscordConsole, and open the config.yml file. Further steps await you there. \nP.S. if you encounter any issues feel free to dm me on discord! (§6DeltaRays#0054§7)");
                    }
                }, 2000);
            }
        }
        if (player.isOp() && main.hasInternetConnection()) {
            try {
                InputStream inputStream = new URL("https://api.github.com/repos/DeltaRays/DiscordConsole/releases").openStream();
                Scanner scanner = new Scanner(inputStream);
                StringBuilder response = new StringBuilder();
                while (scanner.hasNext()) {
                    response.append(scanner.next());
                }
                JSONParser jsonParser = new JSONParser();
                JSONArray releases = (JSONArray) jsonParser.parse(response.toString());
                JSONObject latestRelease = (JSONObject) releases.get(0);
                ArrayList<String> versions = new ArrayList<>();
                for (Object t : releases.toArray()) {
                    JSONObject release = (JSONObject) t;
                    versions.add((String) release.get("tag_name"));
                }
                if ((!main.getConfig().isSet("checkForUpdates") || main.getConfig().getBoolean("checkForUpdates")) && !latestRelease.get("tag_name").toString().equalsIgnoreCase(main.getDescription().getVersion())) {
                    player.sendMessage(String.format("§7You're §6%s §7version(s) behind! (Latest version: §6%s§7) §7Download it here: §6%s", versions.indexOf(main.getDescription().getVersion()), latestRelease.get("tag_name"), "https://www.spigotmc.org/resources/discordconsole.77503/"));
                }
            } catch (Exception exc) {
                main.getLogger().warning("Error encountered while checking for versions!");
                exc.printStackTrace();
            }
        } else if (!main.hasInternetConnection()) {
            main.getLogger().severe("Disabling the plugin! No internet connection was found! Unable to interact with discord!");
            main.getServer().getPluginManager().disablePlugin(main);
        }
    }
}
