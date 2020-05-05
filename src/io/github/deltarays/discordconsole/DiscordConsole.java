package io.github.deltarays.discordconsole;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.*;
import java.util.*;

public class DiscordConsole extends JavaPlugin {
    private static final Logger logger = (Logger) LogManager.getRootLogger();
    Boolean firstLoad = false;
    DiscordSocket socket = null;
    LogAppender appender;
    @Override
    public void onLoad() {
        if(!getConfig().getBoolean("sendStartupMessages")) sendDiscordMessage("Server is starting up!");
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
                appender = new LogAppender(this);
                logger.addAppender(appender);
            }
        } catch (Exception e)  {
            if(getConfig().getBoolean("Debug")) {
                getLogger().severe(e.toString());
            }
        }
    }
    private final Commands commands = new Commands(this);
    @Override
    public void onEnable() {
        try {
            Bukkit.getConsoleSender().sendMessage("DiscordConsole has been enabled!");
            getServer().getPluginManager().registerEvents(new Events(this), this);
            getCommand(commands.maincmd).setExecutor(commands);
            if(socket == null|| ((socket.isClosed() || socket.isClosing()) && !socket.isOpening())){
                appender.startupDone = true;
                socketConnect();
                if(getConfig().getBoolean("Debug")) getLogger().info("[Discord Websocket] Restarted the socket after a reload!");
            }
        } catch (Exception e)  {
            if(getConfig().getBoolean("Debug")) {
                getLogger().severe(e.toString());
            }
        }
        if(!hasInternetConnection()){
            getLogger().warning("No internet connection was found! Unable to check for updates.");
        } else if(newVersionExists()){
            getLogger().info("A new DiscordConsole version was found! Download it at https://www.spigotmc.org/resources/discordconsole.77503/");
        }
        if(!firstLoad) {
            Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
                @Override
                public void run() {

                    appender.startupDone = true;
                    sendDiscordMessage("The server has started up!");
                }
            });
        }
    }
    public void socketConnect() throws Exception {
        try {
            if (socket != null) {
                socket.close(1002, "Reconnecting to the socket!");
                if (socket.timer != null) {
                    socket.timer.cancel();
                    socket.timer.purge();
                    if (getConfig().getBoolean("Debug"))
                        getLogger().info("[Discord WebSocket] Stopping the old heartbeat before connecting to the socket again!");
                }
            }
            socket = new DiscordSocket(URI.create(getDiscordWSUrl()), this);
            socket.connect();
        } catch (Exception e){
            getLogger().severe("[Discord WebSocket] Error encountered while connecting to the socket!\n"+ e.toString());
        }
    }
    public void socketConnect(String sessionId) throws Exception {
        try {
            if (socket != null) {
                socket.close(1002, "Reconnecting to the socket!");
                if (socket.timer != null) {
                    socket.timer.cancel();
                    socket.timer.purge();
                    if (getConfig().getBoolean("Debug"))
                        getLogger().info("[Discord WebSocket] Stopping the old heartbeat before connecting to the socket again!");
                }
            }
            DiscordSocket socket = new DiscordSocket(URI.create(getDiscordWSUrl()), this, sessionId);
            socket.connect();
        } catch (Exception e){
            getLogger().severe("[Discord WebSocket] Error encountered while connecting to the socket!\n"+ e.toString());
    }

    }
    @Override
    public void onDisable() {
        try {
            appender.startupDone = false;
            socket.timer.purge();
            socket.timer.cancel();
            sendDiscordMessage("Server is shutting down...");
            if(socket.isOpen() && !socket.isClosing() ) {
                socket.close(1002, "DiscordConsole has been disabled!");
            }
        } catch(Exception e){
            if(getConfig().getBoolean("Debug")) {
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
        StringBuilder wsURL = new StringBuilder();
        String inputLine;
        while((inputLine = in.readLine()) != null){
            wsURL.append(inputLine);
        }
        in.close();
        wsURL = new StringBuilder(wsURL.toString().replaceAll("\\{(.|\\n)*\"url\":\\s*\"(.*)\"(.|\\n)*\\}", "$2"));
        return wsURL.toString();
    }
    public void loadConfig(){
            if(!getDataFolder().exists()) {
                firstLoad = true;
                if(!getDataFolder().mkdir()){
                    getLogger().warning("Unable to create the DiscordConsole directory");
                };
                saveDefaultConfig();
            }
            File config = new File(getDataFolder(), "config.yml");
            getConfig().options().copyDefaults(true);
            getConfig().options().header("Configuration file for DiscordConsole\nMade by DeltaRays (DeltaRays#0054 on Discord)\n\n ------ BotToken ------ \nThe discord bot's token, if you don't know how to get it this is how to:\n" +
                    "Go to https://discordapp.com/developers/applications/, if you haven't created the application yet create one, otherwise open the application\n" +
                    "go to the Bot section and create a new bot, then copy the token and paste it in here" + "\n\n ------ ChannelId ------ \nThe id of the channel the console logs are going to be sent to, if you don't know how to get it this is how to:\n" +
                    "go to your discord settings, in the Appearance tab scroll down to Advanced and there enable Developer Mode,\n" +
                    "Exit the settings and right click on the channel you want the logs to send to,\n" +
                    "and click on \"Copy ID\" to copy the id, and paste it in here" + "\n\n ------ ChannelRefreshRate ------\n In seconds, every how often the logs should be sent to the channel (minimum is 1 second)\n\n ------ ConsoleCommandsEnabled ------\n Whether anything typed in the console channel should be sent to the server as a console command, can be either true or false\n\n ------ sendStartupMessages ------ \nWhether the server startup messages should be sent to the discord console\n\n ------ Debug ------ \nWhether the debug messages should be sent in console\n");
            saveConfig();
    }

    public HttpURLConnection sendDiscordMessage(String message){
        HttpURLConnection con = null;
        try {
            JSONObject json = new JSONObject();
            json.put("content", message);
            URL endpoint = null;
            endpoint = new URL(String.format("https://discordapp.com/api/v6/channels/%s/messages", getConfig().getString("ChannelId")));
            String params = json.toString();
            con = (HttpURLConnection) endpoint.openConnection();
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("user-agent", "");
            con.setRequestProperty("Authorization", "Bot " + getConfig().getString("BotToken"));
            con.setDoOutput(true);
            OutputStream out = con.getOutputStream();
            out.write(params.getBytes());
            out.flush();
            out.close();
            Integer responsecode = con.getResponseCode();
            return con;
        } catch(Exception e){
            getLogger().severe("Error encountered in sending message to Discord!\n" +e.toString());
        }
        return con;
    }
    public boolean newVersionExists(){
        try {
            InputStream inputStream = new URL("https://api.spigotmc.org/legacy/update.php?resource=77503").openStream();
            Scanner scanner = new Scanner(inputStream);
            String version = getDescription().getVersion();
            if(scanner.hasNext()){
                version = scanner.next();
            }
            if(!version.equals(getDescription().getVersion())){
                return true;
            }
        } catch(Exception ee){
            getLogger().warning("Couldn't look for updates: " + ee.getMessage());
        }
        return false;
    }
    public boolean hasInternetConnection(){
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface interf = interfaces.nextElement();
                if (interf.isUp() && !interf.isLoopback())
                    return true;
            }
        } catch(Exception e){
            getLogger().warning("Error in checking internet connection: " + e.getMessage());
            if(getConfig().getBoolean("Debug")){
                getLogger().warning(e.toString());
            }
        }
        return false;
    }
}
