package io.github.deltarays.discordconsole;


import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

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
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                main.reloadConfig();
                try {
                    main.socketConnect();
                    sender.sendMessage("§f[§bDiscordConsole§f] Successfully reloaded the config and reconnected to the Discord Bot!");
                } catch (Exception e) {
                    sender.sendMessage("§f[§bDiscordConsole§f] §cAn error was encountered while reconnecting to the Discord bot: " + e.getMessage());
                }
            } else {
                sender.sendMessage("\n§bDiscordConsole §fv§b" + main.getDescription().getVersion() + "\n§bAuthor: §b" + String.join("§f, §b", main.getDescription().getAuthors()) + "\n\n§bAvailable subcommands:\n§bReload§f: Reloads the DiscordConsole configuration");
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> tabCompletes = new ArrayList<String>();
        if (args.length == 1) {
            tabCompletes.add("reload");
            return tabCompletes;
        }
        return tabCompletes;
    }
}
