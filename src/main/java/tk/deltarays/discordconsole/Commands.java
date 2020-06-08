//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tk.deltarays.discordconsole;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.Listener;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Commands implements TabExecutor, Listener {
    String maincmd = "discordconsole";
    DiscordConsole main;

    public Commands(DiscordConsole main) {
        this.main = main;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase(this.maincmd)) {
            return false;
        } else {
            if (args.length == 1) {
                if (Arrays.asList("reload", "rl").contains(args[0].toLowerCase())) {
                    if (!sender.isOp() && !sender.hasPermission("discordconsole.reload")) {
                        sender.sendMessage(ConRef.tacc("&cYou don't have enough permissions to run that command!"));
                    } else {
                        sender.sendMessage(ConRef.getPlPrefix() + ConRef.tacc(" &7Reloading the configuration and reconnecting to the Discord Bot..."));
                        this.main.loadConfig();

                        try {
                            if (this.main.socket != null) {
                                this.main.socket.close(1002);
                                if (this.main.socket.timer != null) {
                                    this.main.socket.timer.purge();
                                    this.main.socket.timer.cancel();
                                }
                            }

                            this.main.socketConnect();
                            sender.sendMessage(ConRef.getPlPrefix() + ConRef.tacc(" &aSuccessfully reloaded the config and reconnected to the Discord Bot!"));
                        } catch (Exception e) {
                            sender.sendMessage(ConRef.getPlPrefix() + ConRef.tacc(" &cAn error was encountered while reconnecting to the Discord bot: " + e.getMessage()));
                            e.printStackTrace();
                        }
                    }

                    return true;
                }

                if (Arrays.asList("checkupdate", "updatecheck", "uc", "cu").contains(args[0].toLowerCase())) {
                    if (!sender.isOp() && !sender.hasPermission("discordconsole.checkupdate") && !sender.hasPermission("discordconsole.updatecheck")) {
                        sender.sendMessage(ConRef.tacc("&cYou don't have enough permissions to run that command!"));
                    } else if (!this.main.hasInternetConnection()) {
                        sender.sendMessage(ConRef.tacc("&cNo internet connection was found!"));
                    } else {
                        try {
                            sender.sendMessage(ConRef.getPlPrefix() + ConRef.tacc(" &7Checking for new versions..."));
                            OkHttpClient client = new OkHttpClient();
                            Request request = (new Builder()).url("https://api.github.com/repos/DeltaRays/DiscordConsole/releases").build();
                            Response response = client.newCall(request).execute();
                            JSONParser jsonParser = new JSONParser();

                            assert response.body() != null;

                            JSONArray releases = (JSONArray) jsonParser.parse(response.body().string());
                            response.close();
                            JSONObject latestRelease = (JSONObject) releases.get(0);
                            ArrayList<String> versions = new ArrayList<>();
                            Object[] releasesArr = releases.toArray();

                            for (Object t : releasesArr) {
                                JSONObject release = (JSONObject) t;
                                if (!(Boolean) release.get("draft") && !(Boolean) release.get("prerelease")) {
                                    versions.add((String) release.get("tag_name"));
                                }
                            }

                            if (!versions.contains(this.main.getDescription().getVersion())) {
                                sender.sendMessage(ConRef.tacc("&cApparently you have a plugin version that doesn't exist in the releases list. Either you're in an experimental build or something is wrong. If you're not in an experimental build then you should download the latest release here: &b&nhttps://www.spigotmc.org/resources/discordconsole.77503/"));
                            } else if (!latestRelease.get("tag_name").toString().equalsIgnoreCase(this.main.getDescription().getVersion())) {
                                sender.sendMessage(String.format(ConRef.getPlPrefix() + ConRef.tacc(" &7You're &6%s &7versions behind! (Latest version: &6%s&7) Download it here: &6%s"), versions.indexOf(this.main.getDescription().getVersion()), latestRelease.get("tag_name"), "https://www.spigotmc.org/resources/discordconsole.77503/"));
                            } else {
                                sender.sendMessage(ConRef.getPlPrefix() + ConRef.tacc(" &7You're using the latest DiscordConsole version!"));
                            }
                        } catch (Exception e) {
                            this.main.getLogger().warning("Error encountered while checking for versions!");
                            e.printStackTrace();
                        }
                    }

                    return true;
                }
            }

            sender.sendMessage(ConRef.tacc("\n&6DiscordConsole &7v&6" + this.main.getDescription().getVersion() + "\n&7Author: &6" + String.join("&7, &6", this.main.getDescription().getAuthors()) + "\n\n&7Available subcommands:\n&6reload&7: Reloads the DiscordConsole configuration\n&6updatecheck&7: Manually checks for updates"));
            return true;
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> tabCompletes = new ArrayList<>();
        if (args.length == 1) {
            tabCompletes.add("reload");
            tabCompletes.add("updatecheck");
            return tabCompletes;
        } else {
            return tabCompletes;
        }
    }
}
