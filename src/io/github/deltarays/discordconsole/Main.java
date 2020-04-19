package io.github.deltarays.discordconsole;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class Main extends JavaPlugin {
    private static final Logger logger = (Logger) LogManager.getRootLogger();
    @Override
    public void onEnable() {
        loadConfig();
        if(getConfig().getString("ChannelId") == "000000000000000000"){
            getLogger().severe(ChatColor.DARK_RED + "No channel id was provided! Go to the plugins folder, DiscordConsole, config.yml to set the channel id");
            getServer().getPluginManager().disablePlugin(this);
        }
        try {
            socketConnect();
            LogAppender appender = new LogAppender(this);
            logger.addAppender(appender);
            Bukkit.getConsoleSender().sendMessage("DiscordConsole has been enabled!");
        } catch (Exception e)  {
            if(getConfig().getBoolean("Debug")) {
                getLogger().severe(e.toString());
            }
        }
    }
    public void socketConnect() throws Exception {
        DiscordSocket socket = new DiscordSocket(URI.create(getDiscordWSUrl()), this);
        socket.connect();
    }

    @Override
    public void onDisable() {
        getLogger().info("DiscordConsole has been successfully disabled!");
    }
    private static String getDiscordWSUrl() throws Exception{
        HttpURLConnection connection = (HttpURLConnection) new URL("https://discordapp.com/api/v6/gateway").openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("user-agent", "");
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String wsURL = "";
        String inputLine;
        while((inputLine = in.readLine()) != null){
            wsURL += inputLine;
        }
        in.close();
        wsURL = wsURL.replaceAll("\\{(.|\\n)*\"url\":\\s*\"(.*)\"(.|\\n)*\\}", "$2");
        return wsURL;
    }
    public void loadConfig(){
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
    }
}
