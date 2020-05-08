package io.github.deltarays.discordconsole;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URL;
import java.util.*;

public class DiscordConsole extends JavaPlugin {
    private static final Logger logger = (Logger) LogManager.getRootLogger();
    private final Commands commands = new Commands(this);
    Boolean firstLoad = false;
    DiscordSocket socket = null;
    LogAppender appender;
    ArrayList<String> workingChannels = new ArrayList<>();

    private static String getDiscordWSUrl() {
        StringBuilder wsURL = new StringBuilder();
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://discordapp.com/api/v6/gateway").openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("user-agent", "");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                wsURL.append(inputLine);
            }
            in.close();
            wsURL = new StringBuilder(wsURL.toString().replaceAll("\\{(.|\\n)*\"url\":\\s*\"(.*)\"(.|\\n)*}", "$2"));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return wsURL.toString();
    }

    @Override
    public void onLoad() {
        loadConfig();
        if (!getConfig().getBoolean("sendStartupMessages")) sendDiscordMessage("Server is starting up!");
        if (getConfig().getStringList("channelIds").get(0).equalsIgnoreCase("000000000000000000")) {
            firstLoad = true;
            getLogger().severe(ChatColor.DARK_RED + "No channel id was provided! Go to the plugins folder, DiscordConsole, config.yml to set the channel id");
        } else if (getConfig().getString("botToken").equals("TOKEN")) {
            firstLoad = true;
            getLogger().severe(ChatColor.DARK_RED + "No bot token was provided! Go to the plugins folder, DiscordConsole, config.yml to set the bot token");
        }
        try {
            if (!firstLoad) {
                if (hasInternetConnection()) {
                    appender = new LogAppender(this);
                    logger.addAppender(appender);
                    socketConnect();
                } else {
                    getLogger().severe("Disabling the plugin! No internet connection was found! Unable to interact with discord!");
                    getServer().getPluginManager().disablePlugin(this);
                }
            }
        } catch (Exception e) {
            getLogger().severe("Error encountered while connecting to the socket/adding a log appender");
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        try {
            Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Lag(), 100L, 1L);
            Bukkit.getServer().getConsoleSender().sendMessage(ConRef.getPlPrefix() + " §aDiscordConsole has been enabled!");
            getServer().getPluginManager().registerEvents(new Events(this), this);
            getCommand(commands.maincmd).setExecutor(commands);
            if (socket == null || ((socket.isClosed() || socket.isClosing()) && !socket.isOpening())) {
                appender.startupDone = true;
                socketConnect();
                if (getConfig().getBoolean("debug"))
                    Bukkit.getServer().getConsoleSender().sendMessage(ConRef.getPlPrefix() + " [§6Discord Websocket§7] §aRestarted the socket after a reload!");
            }
        } catch (Exception e) {
            getLogger().severe("Error encountered while enabling plugin!\n" + e.toString());
            e.printStackTrace();
        }
        if (!hasInternetConnection()) {
            getLogger().severe("Disabling the plugin! No internet connection was found! Unable to interact with discord!");
            getServer().getPluginManager().disablePlugin(this);
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
                    if (!((boolean) release.get("draft") || (boolean) release.get("prerelease")))
                        versions.add((String) release.get("tag_name"));
                });
                if (!versions.contains(getDescription().getVersion())) {
                    getLogger().warning("Apparently you have a plugin version that doesn't exist in the releases list. Either you're in an experimental build or something is wrong. If you're not in an experimental build then you should download the latest release here: " + "https://www.spigotmc.org/resources/discordconsole.77503/");
                } else if (!getConfig().isSet("checkForUpdates") || getConfig().getBoolean("checkForUpdates")) {
                    Bukkit.getServer().getConsoleSender().sendMessage(String.format(ConRef.getPlPrefix() + " §7You're §6%s §7versions behind! (Latest version: §6%s§7) Download it here: §6%s", versions.indexOf(getDescription().getVersion()), latestRelease.get("tag_name"), "https://www.spigotmc.org/resources/discordconsole.77503/"));
                }
            } catch (Exception e) {
                getLogger().warning("Error encountered while checking for version!");
                e.printStackTrace();
            }
        }
        if (!firstLoad && hasInternetConnection()) {
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {

                if (appender != null) if (appender.startupDone != null) appender.startupDone = true;
                sendDiscordMessage("The server has started up!");
            });
        }
    }

    public void socketConnect() {
        if (!hasInternetConnection()) {
            getLogger().severe("Disabling the plugin! No internet connection was found! Unable to interact with discord!");
            getServer().getPluginManager().disablePlugin(this);
        } else {
            try {
                if (socket != null) {
                    socket.close(1002, "Reconnecting to the socket!");
                    if (socket.timer != null) {
                        socket.timer.cancel();
                        socket.timer.purge();
                        if (getConfig().getBoolean("debug"))
                            getLogger().info("[Discord WebSocket] Stopping the old heartbeat before connecting to the socket again!");
                    }
                }
                socket = new DiscordSocket(URI.create(getDiscordWSUrl()), this);
                socket.connect();
            } catch (Exception e) {
                getLogger().severe("[Discord WebSocket] Error encountered while connecting to the socket!");
                e.printStackTrace();
            }
        }

    }

    public void socketConnect(String sessionId) {
        if (!hasInternetConnection()) {
            getLogger().severe("Disabling the plugin! No internet connection was found! Unable to interact with discord!");
            getServer().getPluginManager().disablePlugin(this);
        } else {
            try {
                if (socket != null) {
                    socket.close(1002, "Reconnecting to the socket!");
                    if (socket.timer != null) {
                        socket.timer.cancel();
                        socket.timer.purge();
                        if (getConfig().getBoolean("debug"))
                            getLogger().info("[Discord WebSocket] Stopping the old heartbeat before connecting to the socket again!");
                    }
                }
                socket = new DiscordSocket(URI.create(getDiscordWSUrl()), this, sessionId);
                socket.connect();
                Timer t = new Timer();
                t.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (!socket.isConnected() && socket.isOpen() && !socket.isInvalid) {
                            socket.close(1002);
                            socketConnect(sessionId);
                        }
                    }
                }, 10000);
            } catch (Exception e) {
                getLogger().severe("[Discord WebSocket] Error encountered while connecting to the socket!");
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onDisable() {
        try {
            appender.startupDone = false;
            if (socket != null) {
                if (socket.timer != null) {
                    socket.timer.purge();
                    socket.timer.cancel();
                }
                sendDiscordMessage("Server is shutting down...");
                if (socket.isOpen() && !socket.isClosing()) {
                    socket.close(1002, "DiscordConsole has been disabled!");
                }
            }
        } catch (Exception e) {
            getLogger().severe("Error while disabling plugin!");
            e.printStackTrace();
        }
        Bukkit.getServer().getConsoleSender().sendMessage(ConRef.getPlPrefix() + " §cDiscordConsole has been successfully disabled!");
    }

    public void loadConfig() {
        reloadConfig();
        if (!getDataFolder().exists()) {
            firstLoad = true;
            if (!getDataFolder().mkdir()) {
                getLogger().warning("Unable to create the DiscordConsole directory");
            }
            saveDefaultConfig();
        }
        if (getConfig().isSet("channelId")) {
            getConfig().set("channelIds", new String[]{getConfig().getString("channelId")});
            getConfig().set("channelId", null);
        }
        if (getConfig().getString("botStatus").isEmpty()) {
            getConfig().set("botStatus", "online");
        }
        workingChannels = new ArrayList<>();
        workingChannels.addAll(getConfig().getStringList("channelIds"));
        for (Object key : getConfig().getKeys(false).toArray()) {
            String k = (String) key;
            if (k.charAt(0) == k.toUpperCase().charAt(0)) {
                String newK = k.substring(0, 1).toLowerCase() + k.substring(1);
                getConfig().set(newK, getConfig().get(k));
                getConfig().set(k, null);
            }
        }

        getConfig().options().copyDefaults(true);
        getConfig().options().header("Configuration file for DiscordConsole\nMade by DeltaRays (DeltaRays#0054 on Discord)\n\n ------ botToken ------ \nThe discord bot's token, if you don't know how to get it this is how to:\n" +
                "Go to https://discordapp.com/developers/applications/, if you haven't created the application yet create one, otherwise open the application\n" +
                "go to the Bot section and create a new bot, then copy the token and paste it in here" + "\n\n ------ channelIds ------ \nThe ids of the channels the console logs are going to be sent to, this is a config example, in which messages are going to be sent to three channels:\nchannelIds:\n- '660337933743816724'\n- '707221730946449450'\n- '707215090121834538'\n (Note: you can only edit channels when the server restarts) if you don't know how to get a channel id this is how to:\n" +
                "go to your discord settings, in the Appearance tab scroll down to Advanced and there enable Developer Mode,\n" +
                "Exit the settings and right click on the channel you want the logs to send to,\n" +
                "and click on \"Copy ID\" to copy the id, and paste it in here" + "\n\n ------ channelRefreshRate ------\n In seconds, every how often the logs should be sent to the channel (minimum is 1 second)\n\n ------ consoleCommandsEnabled ------\n Whether anything typed in the console channel should be sent to the server as a console command, can be either true or false\n\n ------ sendStartupMessages ------ \nWhether the server startup messages should be sent to the discord console\n\n ------ checkForUpdates ------\nWhether or not new updates should be checked automatically (you can always just )\n\n ------ prefix ------\nChange the plugin's prefix to anything you'd like!\n\n ------ botStatus ------ \nThe bot's status (can be online, dnd (do not disturb), idle or invisible)\n\n ------ botStatusText ------ \nThe bot's status's text (What you see under their name), added after Playing (example: Playing 'on Minecraft'). You can also use placeholders to be able to customize the status more.\n%tps%: Returns the server's tps\n%player_count%: Sends the unvanished player count (supports SuperVanish, VanishNoPackets, PremiumVanish and a few more vanish plugins)\n%player_max%: Wends the maximum online players\n%date%: Sends the time\n%total_players%: Sends the number of players to have ever joined the server.\n%uptime%: Sends the server uptime\n%motd%: Sends the server motd\n%used_memory%: Sends the used memory in MB\n%max_memory%: Sends the max memory the server can use (use %used_memory_gb% and %max_memory_gb% for the memory in GB)\n\n ------ debug ------ \nWhether the debug messages should be sent in console\n");
        saveConfig();
    }

    public void sendDiscordMessage(String message) {
        workingChannels.forEach((Object cId) -> {
            try {
                String channelId = (String) cId;
                HttpURLConnection con;
                JSONObject json = new JSONObject();
                JSONObject json2 = new JSONObject();
                JSONArray jsonArr = new JSONArray();
                json2.put("parse", jsonArr);
                json.put("content", message);
                json.put("allowed_mentions", json2);
                URL endpoint;
                endpoint = new URL(String.format("https://discordapp.com/api/v6/channels/%s/messages", channelId));
                String params = json.toString();
                con = (HttpURLConnection) endpoint.openConnection();
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("user-agent", "");
                con.setRequestProperty("Authorization", "Bot " + getConfig().getString("botToken"));
                con.setDoOutput(true);
                OutputStream out = con.getOutputStream();
                out.write(params.getBytes());
                out.flush();
                out.close();
                int responseCode = con.getResponseCode();
                if (responseCode == 404) {
                    Bukkit.getScheduler().runTask(this, () -> {
                        getLogger().severe("Channel with id " + channelId + " doesn't exist!");
                        workingChannels.remove(channelId);
                    });
                } else if (responseCode == 403) {
                    Bukkit.getScheduler().runTask(this, () -> {
                        getLogger().severe("The bot can't type in " + channelId + "!");
                        workingChannels.remove(channelId);
                    });
                }
            } catch (Exception e) {
                getLogger().severe("Error encountered in sending message to Discord!");
                e.printStackTrace();
            }
        });
    }

    public boolean hasInternetConnection() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface interf = interfaces.nextElement();
                if (interf.isUp() && !interf.isLoopback())
                    return true;
            }
        } catch (Exception e) {
            getLogger().warning("Error in checking internet connection: " + e.getMessage());
            if (getConfig().getBoolean("debug")) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void updateBotStatus() {
        if (socket != null) {
            if (socket.isOpen() && socket.isConnected()) {
                String botStatus = getConfig().getString("botStatus");
                ArrayList<String> statuses = new ArrayList<>();
                statuses.addAll(Arrays.asList("online", "dnd", "invisible", "idle"));
                if (!statuses.contains(botStatus)) botStatus = "online";
                JSONObject game = new JSONObject();
                game.put("name", ConRef.replaceExpressions(getConfig().getString("botStatusText"), true));
                game.put("type", 0);
                JSONObject presResp = new JSONObject();
                if (!getConfig().getString("botStatusText").isEmpty()) presResp.put("game", game);
                presResp.put("status", botStatus);
                presResp.put("afk", false);
                presResp.put("since", null);
                JSONObject resp = new JSONObject();
                resp.put("op", 3);
                resp.put("d", presResp);
                String update = resp.toJSONString();
                socket.send(update);
            }
        }
    }

}
