package io.github.deltarays.discordconsole;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.*;

public class Main extends JavaPlugin {
    private static final Logger logger = (Logger) LogManager.getRootLogger();
    Boolean firstLoad = false;
    @Override
    public void onLoad() {
        if(!getDataFolder().exists()){
            firstLoad = true;
            getDataFolder().mkdir();
        }
        loadConfig();
        if(getConfig().getString("ChannelId").equals("000000000000000000")){
            firstLoad = true;
            getLogger().severe(ChatColor.DARK_RED + "No channel id was provided! Go to the plugins folder, DiscordConsole, config.yml to set the channel id");
        } else if(getConfig().getString("BotToken").equals("TOKEN")){
            firstLoad = true;
            getLogger().severe(ChatColor.DARK_RED +"No bot token was provided! Go to the plugins folder, DiscordConsole, config.yml to set the bot token");
        }
        try {
            if(!firstLoad) {
                socketConnect();
                LogAppender appender = new LogAppender(this);
                logger.addAppender(appender);
            }
        } catch (Exception e)  {
            if(getConfig().getBoolean("Debug")) {
                getLogger().severe(e.toString());
            }
        }
    }

    @Override
    public void onEnable() {
        try {
            Bukkit.getConsoleSender().sendMessage("DiscordConsole has been enabled!");
            getServer().getPluginManager().registerEvents(new Events(this), this);
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
    public void socketConnect(String sessionId) throws Exception {
        DiscordSocket socket = new DiscordSocket(URI.create(getDiscordWSUrl()), this, sessionId);
        socket.connect();

    }
    @Override
    public void onDisable() {
        try {
            JSONObject json = new JSONObject();
            json.put("content", "The server is shutting down!");
            URL endpoint = null;
            endpoint = new URL(String.format("https://discordapp.com/api/v6/channels/%s/messages", getConfig().getString("ChannelId")));
            String params = json.toString();
            HttpURLConnection con = (HttpURLConnection) endpoint.openConnection();
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("user-agent", "");
            con.setRequestProperty("Authorization", "Bot " + getConfig().getString("BotToken"));
            con.setDoOutput(true);
            OutputStream out = con.getOutputStream();
            out.write(params.getBytes());
            out.flush();
            out.close();
            Integer responsecode = con.getResponseCode();
        } catch(Exception e){
            if(getConfig().getBoolean("Debug")){
                getLogger().severe(e.toString());
            }
        }
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
