package io.github.deltarays.discordconsole;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.Listener;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Commands implements TabExecutor, Listener {
    String maincmd = "discordconsole";
    DiscordConsole main;

    public Commands(DiscordConsole main) {
        super();
        this.main = main;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase(maincmd)) {
            if (args.length == 1) {
                if (Arrays.asList("reload", "rl").contains(args[0].toLowerCase())) {
                    if (sender.isOp() || sender.hasPermission("discordconsole.reload")) {
                        main.loadConfig();
                        try {
                            if (main.socket != null) {
                                main.socket.close(1002);
                                if (main.socket.timer != null) {
                                    main.socket.timer.purge();
                                    main.socket.timer.cancel();
                                }
                            }
                            main.socketConnect();
                            sender.sendMessage(ConRef.getPlPrefix() + " §aSuccessfully reloaded the config and reconnected to the Discord Bot!");
                        } catch (Exception e) {
                            sender.sendMessage(ConRef.getPlPrefix() + " §cAn error was encountered while reconnecting to the Discord bot: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        sender.sendMessage("§cYou don't have enough permissions to run that command!");
                    }
                    return true;
                } else if (Arrays.asList("checkupdate", "updatecheck", "uc", "cu").contains(args[0].toLowerCase())) {
                    if (sender.isOp() || sender.hasPermission("discordconsole.checkupdate") || sender.hasPermission("discordconsole.updatecheck")) {
                        if (!main.hasInternetConnection()) {
                            sender.sendMessage("§cNo internet connection was found!");
                        } else {
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
                                releases.forEach((Object t) -> {
                                    JSONObject release = (JSONObject) t;
                                    versions.add((String) release.get("tag_name"));
                                });
                                if (!versions.contains(main.getDescription().getVersion())) {
                                    sender.sendMessage("§cApparently you have a plugin version that doesn't exist in the releases list. Either you're in an experimental build or something is wrong. If you're not in an experimental build then you should download the latest release here: §b§n" + "https://www.spigotmc.org/resources/discordconsole.77503/");
                                } else {
                                    sender.sendMessage(String.format("§7You're §6%s §7version(s) behind! (Latest version: §6%s§7) §7Download it here: §6%s", versions.indexOf(main.getDescription().getVersion()), latestRelease.get("tag_name"), "https://www.spigotmc.org/resources/discordconsole.77503/"));
                                }
                            } catch (Exception exc) {
                                main.getLogger().warning("Error encountered while checking for versions!");
                                exc.printStackTrace();
                            }
                        }
                    } else {
                        sender.sendMessage("§cYou don't have enough permissions to run that command!");
                    }
                    return true;
                }
            }
            sender.sendMessage("\n§6DiscordConsole §7v§6" + main.getDescription().getVersion() + "\n§7Author: §6" + String.join("§7, §6", main.getDescription().getAuthors()) + "\n\n§7Available subcommands:\n§6reload§7: Reloads the DiscordConsole configuration\n§6updatecheck§7: Manually checks for updates");
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> tabCompletes = new ArrayList<>();
        if (args.length == 1) {
            tabCompletes.add("reload");
            tabCompletes.add("updatecheck");
            return tabCompletes;
        }
        return tabCompletes;
    }
}
