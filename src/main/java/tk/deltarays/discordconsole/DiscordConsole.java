package tk.deltarays.discordconsole;


import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

public class DiscordConsole extends JavaPlugin {
    private static final Logger logger = (Logger) LogManager.getRootLogger();
    private final Commands commands = new Commands(this);
    Boolean firstLoad = false;
    DiscordSocket socket = null;
    LogAppender appender;
    HashMap<String, HashMap> workingChannels = new HashMap<>();

    private static String getDiscordWSUrl() {
        String resp = null;
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url("https://discordapp.com/api/v6/gateway").build();
            Response response = client.newCall(request).execute();
            JSONParser parser = new JSONParser();
            assert response.body() != null;
            JSONObject obj = (JSONObject) parser.parse(response.body().string());
            resp = (String) obj.get("url");
            response.close();
        } catch (IOException | ParseException e) {
            DiscordConsole.getPlugin(DiscordConsole.class).getLogger().severe("[Discord WebSocket] Error finding URL!");
            e.printStackTrace();
        }
        return resp;
    }

    @Override
    public void onLoad() {
        loadConfig();
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
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                Bukkit.getConsoleSender().sendMessage(ConRef.getPlPrefix() + " §aPlaceholderAPI placeholders loaded!");
            }
            Bukkit.getServer().getConsoleSender().sendMessage(ConRef.getPlPrefix() + " §aDiscordConsole has been enabled!");
            getServer().getPluginManager().registerEvents(new Events(this), this);
            Objects.requireNonNull(getCommand(commands.maincmd)).setExecutor(commands);
            if (socket == null || ((socket.isClosed() || socket.isClosing()) && !socket.isOpening())) {
                if (appender == null) {
                    appender = new LogAppender(this);
                    logger.addAppender(appender);
                }
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
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url("https://api.github.com/repos/DeltaRays/DiscordConsole/releases").build();
                Response response = client.newCall(request).execute();
                JSONParser jsonParser = new JSONParser();
                assert response.body() != null;
                JSONArray releases = (JSONArray) jsonParser.parse(response.body().string());
                response.close();
                JSONObject latestRelease = (JSONObject) releases.get(0);
                ArrayList<String> versions = new ArrayList<>();
                for (Object t : releases.toArray()) {
                    JSONObject release = (JSONObject) t;
                    if (!((boolean) release.get("draft") || (boolean) release.get("prerelease")))
                        versions.add((String) release.get("tag_name"));
                }
                if (!versions.contains(getDescription().getVersion())) {
                    getLogger().warning("Apparently you have a plugin version that doesn't exist in the releases list. Either you're in an experimental build or something is wrong. If you're not in an experimental build then you should download the latest release here: " + "https://www.spigotmc.org/resources/discordconsole.77503/");
                } else if (!getConfig().isSet("checkForUpdates") || getConfig().getBoolean("checkForUpdates")) {
                    if (!latestRelease.get("tag_name").toString().equalsIgnoreCase(getDescription().getVersion())) {
                        Bukkit.getServer().getConsoleSender().sendMessage(String.format(ConRef.getPlPrefix() + " §7You're §6%s §7versions behind! (Latest version: §6%s§7) Download it here: §6%s", versions.indexOf(getDescription().getVersion()), latestRelease.get("tag_name"), "https://www.spigotmc.org/resources/discordconsole.77503/"));
                    } else {
                        Bukkit.getConsoleSender().sendMessage(ConRef.getPlPrefix() + " §7You're using the latest DiscordConsole version!");
                    }
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
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
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
        if (getConfig().isSet("channelId") && !getConfig().isSet("channelIds")) {
            getConfig().set("channelIds", new String[]{getConfig().getString("channelId")});
            getConfig().set("channelId", null);
        }
        if (getConfig().isSet("channelIds")) {
            ArrayList list = (ArrayList) getConfig().getStringList("channelIds");
            getConfig().set("channelIds", null);
            ConfigurationSection chanSect = getConfig().createSection("channels");
            for (Object k : list.toArray()) {
                if (!chanSect.isSet(k + ".filter")) {
                    chanSect.set(k + ".filter", "");
                }
                if (!chanSect.isSet(k + ".topic")) {
                    chanSect.set(k + ".topic", "Players online: %player_count%/%player_max%");
                }
            }
            saveConfig();
            reloadConfig();
        }
        if (!getDataFolder().exists()) {
            firstLoad = true;
            if (!getDataFolder().mkdir()) {
                getLogger().warning("Unable to create the DiscordConsole directory");
            }
            saveDefaultConfig();
        }


        if (Objects.requireNonNull(getConfig().getString("botStatus")).isEmpty()) {
            getConfig().set("botStatus", "online");
        }
        for (Object key : getConfig().getKeys(false).toArray()) {
            String k = (String) key;
            if (k.charAt(0) == k.toUpperCase().charAt(0)) {
                String newK = k.substring(0, 1).toLowerCase() + k.substring(1);
                getConfig().set(newK, getConfig().get(k));
                getConfig().set(k, null);
            }
        }
        saveConfig();
        reloadConfig();
        getConfig().options().copyDefaults(true);
        ConfigurationSection chanSect = getConfig().getConfigurationSection("channels");
        ConfigurationSection finalChanSect1 = chanSect;
        if (finalChanSect1 != null) {
            finalChanSect1.getKeys(false).forEach((String k) -> {
                if (k.contains("0000000000000000")) {
                    finalChanSect1.set(k, null);
                }
                if (!finalChanSect1.isSet(k + ".filter")) {
                    finalChanSect1.set(k + ".filter", "");
                }
                if (!finalChanSect1.isSet(k + ".topic")) {
                    finalChanSect1.set(k + ".topic", "Players online: %player_count%/%player_max%");
                }
            });
        }
        getConfig().options().header("Configuration file for DiscordConsole\nMade by DeltaRays (DeltaRays#0054 on Discord)\n\n ------ botToken ------ \nThe discord bot's token, if you don't know how to get it this is how to:\n" +
                "Go to https://discordapp.com/developers/applications/, if you haven't created the application yet create one, otherwise open the application\n" +
                "go to the Bot section and create a new bot, then copy the token and paste it in here" + "\n\n ------ channels ------ \n" +
                "The channels the console logs are going to be sent to, this is a config example, in which messages are going to be sent to two channels:\n" +
                "channels:\n" +
                "  '519911076691968006':\n" +
                "    filter: 'joined'\n" +
                "    topic: 'Players online: %player_count%/%player_max%'\n" +
                "  '707215060744929280':\n" +
                "    filter: ''\n" +
                "    topic: '' \n" +
                "Only messages that contain 'joined' are going to be sent in the first channel. (Supports regex, if you don't know what regex is you should check it out at https://medium.com/factory-mind/regex-tutorial-a-simple-cheatsheet-by-examples-649dc1c3f285)\n" +
                "All messages are going to be sent in the second channel\nif you don't know how to get a channel id this is how to:\n" +
                "go to your discord settings, in the Appearance tab scroll down to Advanced and there enable Developer Mode,\n" +
                "Exit the settings and right click on the channel you want the logs to send to,\n" +
                "and click on \"Copy ID\" to copy the id, and paste it in here" + "\n\n ------ channelRefreshRate ------\n In seconds, every how often the logs should be sent to the channel (minimum is 1 second)\n\n ------ consoleCommandsEnabled ------\n Whether anything typed in the console channel should be sent to the server as a console command, can be either true or false\n\n ------ sendStartupMessages ------ \nWhether the server startup messages should be sent to the discord console\n\n ------ checkForUpdates ------\nWhether or not new updates should be checked automatically (you can always just )\n\n ------ prefix ------\nChange the plugin's prefix to anything you'd like!\n\n ------ botStatus ------ \nThe bot's status (can be online, dnd (do not disturb), idle or invisible)\n\n ------ botActivity ------\nCan be PLAYING or STREAMING or LISTENING, will be put before the bot's status text\n\n ------ botStatusText ------ \nThe bot's status's text (What you see under their name), added after Playing/Streaming/Listening (defined in botActivity) (example: Playing 'on Minecraft').\n\n ------ debug ------ \nWhether the debug messages should be sent in console\n\n\nPlaceholders: You can also use placeholders to be able to customize the status more.\n%player_count%: Sends the unvanished player count (supports SuperVanish, VanishNoPackets, PremiumVanish and a few more vanish plugins)\n%player_max%: Wends the maximum online players\n%date%: Sends the time\n%total_players%: Sends the number of players to have ever joined the server.\n%motd%: Sends the server motd\nYou can also use PlaceholderAPI placeholders");
        if (!getConfig().isSet("channels") && !getConfig().isSet("channelIds") && !getConfig().isSet("channelId")) {
            ConfigurationSection chanSecti = getConfig().createSection("channels");
            chanSecti.set("000000000000000000.filter", "");
            chanSecti.set("000000000000000000.topic", "Players online: %player_count%/%player_max%");
        }
        saveConfig();
        reloadConfig();
        chanSect = getConfig().getConfigurationSection("channels");
        ConfigurationSection finalChanSect = chanSect;
        chanSect.getKeys(false).forEach((String k) -> {
            if (k.contains("0000000000000000")) {
                finalChanSect.set(k, null);
            }
            if (!finalChanSect.isSet(k + ".filter")) {
                finalChanSect.set(k + ".filter", "");
            }
            if (!finalChanSect.isSet(k + ".topic")) {
                finalChanSect.set(k + ".topic", "Players online: %player_count%/%player_max%");
            }
        });
        workingChannels = new HashMap<>();
        for (Object cId : chanSect.getKeys(false).toArray()) {
            MemorySection config = (MemorySection) chanSect.get((String) cId);
            HashMap<String, Object> configMap = new HashMap<>();
            assert config != null;
            for (String key : config.getKeys(false)) {
                configMap.put(key, config.get(key));
            }
            if (!((String) cId).contains("0000000000000000")) workingChannels.put((String) cId, configMap);
        }
        if (!getConfig().getBoolean("sendStartupMessages")) sendDiscordMessage("Server is starting up!");
        if (chanSect.getKeys(false).size() < 1 || chanSect.getKeys(false).toArray()[0].toString().contains("00000000000000000")) {
            firstLoad = true;
            getLogger().severe(ChatColor.DARK_RED + "No channel id was provided! Go to the plugins folder, DiscordConsole, config.yml to set the channel id");
        } else if (Objects.equals(getConfig().getString("botToken"), "TOKEN")) {
            firstLoad = true;
            getLogger().severe(ChatColor.DARK_RED + "No bot token was provided! Go to the plugins folder, DiscordConsole, config.yml to set the bot token");
        }
    }

    public void updateChannelTopic() {
        OkHttpClient client = new OkHttpClient();
        for (Object cId : workingChannels.keySet().toArray()) {
            try {
                String channelId = (String) cId;
                String topic = (String) workingChannels.get(cId).get("topic");
                JSONObject json = new JSONObject();
                json.put("topic", ConRef.replaceExpressions(topic, false));
                RequestBody body = RequestBody.create(MediaType.get("application/json"), json.toString());
                Request request = new Request.Builder().url("https://discordapp.com/api/v6/channels/" + channelId).patch(body).addHeader("Authorization", "Bot " + getConfig().getString("botToken")).build();
                Response response = client.newCall(request).execute();
                int responseCode = response.code();
                if (responseCode == 404) {
                    getLogger().severe("Channel with id " + channelId + " doesn't exist!");
                    workingChannels.remove(channelId);
                } else if (responseCode == 403) {
                    getLogger().severe("The bot can't edit " + channelId + "'s channel topic!");
                    workingChannels.remove(channelId);
                }
                response.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendDiscordMessage(String message) {
        OkHttpClient client = new OkHttpClient();
        for (Object cId : workingChannels.keySet().toArray()) {
            try {
                String channelId = (String) cId;
                String filter = (String) workingChannels.get(cId).get("filter");
                Pattern pattern = Pattern.compile(filter);
                if (filter == null || filter.isEmpty()) {
                    JSONObject json = new JSONObject();
                    JSONObject json2 = new JSONObject();
                    JSONArray jsonArr = new JSONArray();
                    json2.put("parse", jsonArr);
                    json.put("content", message);
                    json.put("allowed_mentions", json2);
                    RequestBody body = RequestBody.create(MediaType.get("application/json"), json.toString());
                    Request request = new Request.Builder().url("https://discordapp.com/api/v6/channels/" + channelId + "/messages").post(body).addHeader("Authorization", "Bot " + getConfig().getString("botToken")).build();
                    Response response = client.newCall(request).execute();
                    int responseCode = response.code();
                    if (responseCode == 404) {
                        getLogger().severe("Channel with id " + channelId + " doesn't exist!");
                        workingChannels.remove(channelId);
                    } else if (responseCode == 403) {
                        getLogger().severe("The bot can't type in " + channelId + "!");
                        workingChannels.remove(channelId);
                    }
                    response.close();
                } else {
                    StringBuilder newB = new StringBuilder();
                    for (String line : message.split("\n")) {
                        if (pattern.matcher(line).find()) newB.append(line);
                    }
                    if (!newB.toString().isEmpty()) {
                        JSONObject json = new JSONObject();
                        JSONObject json2 = new JSONObject();
                        JSONArray jsonArr = new JSONArray();
                        json2.put("parse", jsonArr);
                        json.put("content", newB.toString());
                        json.put("allowed_mentions", json2);
                        RequestBody body = RequestBody.create(MediaType.get("application/json"), json.toString());
                        Request request = new Request.Builder().url("https://discordapp.com/api/v6/channels/" + channelId + "/messages").post(body).addHeader("Authorization", "Bot " + getConfig().getString("botToken")).build();
                        Response response = client.newCall(request).execute();
                        int responseCode = response.code();
                        if (responseCode == 404) {
                            getLogger().severe("Channel with id " + channelId + " doesn't exist!");
                            workingChannels.remove(channelId);
                        } else if (responseCode == 403) {
                            getLogger().severe("The bot can't type in " + channelId + "!");
                            workingChannels.remove(channelId);
                        }
                        response.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

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
                int type = 0;
                String botActivity = getConfig().getString("botActivity");
                assert botActivity != null;
                if (botActivity.equalsIgnoreCase("listening")) type = 1;
                else if (botActivity.equalsIgnoreCase("streaming")) type = 2;
                String botStatus = getConfig().getString("botStatus");
                ArrayList<String> statuses = new ArrayList<>(Arrays.asList("online", "dnd", "invisible", "idle"));
                if (!statuses.contains(botStatus)) botStatus = "online";
                JSONObject game = new JSONObject();
                game.put("name", ConRef.replaceExpressions(getConfig().getString("botStatusText"), true));
                game.put("type", type);
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
