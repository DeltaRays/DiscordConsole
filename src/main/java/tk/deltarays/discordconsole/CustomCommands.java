//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tk.deltarays.discordconsole;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CustomCommands extends Command {
    String message;

    protected CustomCommands(String name, String message) {
        super(name);
        this.message = message;
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        sender.sendMessage(this.message);
        return false;
    }
}
