//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tk.deltarays.discordconsole;

import okhttp3.*;
import okhttp3.Request.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class DiscordConsole extends JavaPlugin {
    private static final Logger logger = (Logger) LogManager.getRootLogger();
    private final Commands commands = new Commands(this);
    Boolean firstLoad = false;
    DiscordSocket socket = null;
    LogAppender appender;
    HashMap<String, HashMap<String, Object>> workingChannels = new HashMap<>();
    CommandMap commandMap;

    public DiscordConsole() {
    }

    private static String getDiscordWSUrl() {
        String resp = null;

        try {
            OkHttpClient client = new OkHttpClient();
            Request request = (new Builder()).url("https://discordapp.com/api/v6/gateway").build();
            Response response = client.newCall(request).execute();
            JSONParser parser = new JSONParser();

            assert response.body() != null;

            JSONObject obj = (JSONObject) parser.parse(response.body().string());
            resp = (String) obj.get("url");
            response.close();
        } catch (ParseException | IOException e) {
            getPlugin(DiscordConsole.class).getLogger().severe("[Discord WebSocket] Error finding URL!");
            e.printStackTrace();
        }

        return resp;
    }

    public void onLoad() {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            this.commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        this.loadConfig();

        try {
            if (!this.firstLoad) {
                if (this.hasInternetConnection()) {
                    this.appender = new LogAppender(this);
                    logger.addAppender(this.appender);
                    this.socketConnect();
                } else {
                    this.getLogger().severe("Disabling the plugin! No internet connection was found! Unable to interact with discord!");
                    this.getServer().getPluginManager().disablePlugin(this);
                }
            }
        } catch (Exception e) {
            this.getLogger().severe("Error encountered while connecting to the socket/adding a log appender");
            e.printStackTrace();
        }

    }

    public void onEnable() {
        try {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                Bukkit.getConsoleSender().sendMessage(ConRef.getPlPrefix() + ChatColor.GREEN + " PlaceholderAPI placeholders loaded!");
            }

            Bukkit.getServer().getConsoleSender().sendMessage(ConRef.getPlPrefix() + ChatColor.GREEN + " DiscordConsole has been enabled!");
            this.getServer().getPluginManager().registerEvents(new Events(this), this);
            requireNonNull(this.getCommand(this.commands.maincmd)).setExecutor(this.commands);
            if (this.socket == null || (this.socket.isClosed() || this.socket.isClosing()) && !this.socket.isOpening()) {
                if (this.appender == null) {
                    this.appender = new LogAppender(this);
                    logger.addAppender(this.appender);
                }

                this.appender.startupDone = true;
                this.socketConnect();
                if (this.getConfig().getBoolean("debug")) {
                    Bukkit.getServer().getConsoleSender().sendMessage(ConRef.getPlPrefix() + ConRef.tacc(" [&6Discord Websocket&7] &aRestarted the socket after a reload!"));
                }
            }
        } catch (Exception e) {
            this.getLogger().severe("Error encountered while enabling plugin!\n" + e.toString());
            e.printStackTrace();
        }

        if (!this.hasInternetConnection()) {
            this.getLogger().severe("Disabling the plugin! No internet connection was found! Unable to interact with discord!");
            this.getServer().getPluginManager().disablePlugin(this);
        } else {
            try {
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

                if (!versions.contains(this.getDescription().getVersion())) {
                    this.getLogger().warning("Apparently you have a plugin version that doesn't exist in the releases list. Either you're in an experimental build or something is wrong. If you're not in an experimental build then you should download the latest release here: https://www.spigotmc.org/resources/discordconsole.77503/");
                } else if (!this.getConfig().isSet("checkForUpdates") || this.getConfig().getBoolean("checkForUpdates")) {
                    if (!latestRelease.get("tag_name").toString().equalsIgnoreCase(this.getDescription().getVersion())) {
                        Bukkit.getServer().getConsoleSender().sendMessage(String.format(ConRef.getPlPrefix() + ConRef.tacc(" &7You're &6%s &7versions behind! (Latest version: &6%s&7) Download it here: &6%s"), versions.indexOf(this.getDescription().getVersion()), latestRelease.get("tag_name"), "https://www.spigotmc.org/resources/discordconsole.77503/"));
                    } else {
                        Bukkit.getConsoleSender().sendMessage(ConRef.getPlPrefix() + ConRef.tacc(" &7You're using the latest DiscordConsole version!"));
                    }
                }
            } catch (Exception e) {
                this.getLogger().warning("Error encountered while checking for version!");
                e.printStackTrace();
            }
        }

        if (!this.firstLoad && this.hasInternetConnection()) {
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                if (this.appender != null && this.appender.startupDone != null) {
                    this.appender.startupDone = true;
                }

                this.sendDiscordMessage("The server has started up!");
            });
        }

    }

    public void socketConnect() {
        if (!this.hasInternetConnection()) {
            this.getLogger().severe("Disabling the plugin! No internet connection was found! Unable to interact with discord!");
            this.getServer().getPluginManager().disablePlugin(this);
        } else {
            try {
                if (this.socket != null) {
                    this.socket.close(1002, "Reconnecting to the socket!");
                    if (this.socket.timer != null) {
                        this.socket.timer.cancel();
                        this.socket.timer.purge();
                        if (this.getConfig().getBoolean("debug")) {
                            this.getLogger().info("[Discord WebSocket] Stopping the old heartbeat before connecting to the socket again!");
                        }
                    }
                }

                this.socket = new DiscordSocket(URI.create(getDiscordWSUrl()), this);
                this.socket.connect();
            } catch (Exception e) {
                this.getLogger().severe("[Discord WebSocket] Error encountered while connecting to the socket!");
                e.printStackTrace();
            }
        }

    }

    public void socketConnect(final String sessionId) {
        if (!this.hasInternetConnection()) {
            this.getLogger().severe("Disabling the plugin! No internet connection was found! Unable to interact with discord!");
            this.getServer().getPluginManager().disablePlugin(this);
        } else {
            try {
                if (this.socket != null) {
                    this.socket.close(1002, "Reconnecting to the socket!");
                    if (this.socket.timer != null) {
                        this.socket.timer.cancel();
                        this.socket.timer.purge();
                        if (this.getConfig().getBoolean("debug")) {
                            this.getLogger().info("[Discord WebSocket] Stopping the old heartbeat before connecting to the socket again!");
                        }
                    }
                }

                this.socket = new DiscordSocket(URI.create(getDiscordWSUrl()), this, sessionId);
                this.socket.connect();
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    public void run() {
                        if (!DiscordConsole.this.socket.isConnected() && DiscordConsole.this.socket.isOpen() && !DiscordConsole.this.socket.isInvalid) {
                            DiscordConsole.this.socket.close(1002);
                            DiscordConsole.this.socketConnect(sessionId);
                        }

                    }
                }, 15000L);
            } catch (Exception e) {
                this.getLogger().severe("[Discord WebSocket] Error encountered while connecting to the socket!");
                e.printStackTrace();
            }
        }

    }

    public void onDisable() {
        try {
            this.appender.startupDone = false;
            if (this.socket != null) {
                if (this.socket.timer != null) {
                    this.socket.timer.purge();
                    this.socket.timer.cancel();
                }

                this.sendDiscordMessage("Server is shutting down...");
                if (this.socket.isOpen() && !this.socket.isClosing()) {
                    this.socket.close(1002, "DiscordConsole has been disabled!");
                }
            }
        } catch (Exception e) {
            this.getLogger().severe("Error while disabling plugin!");
            e.printStackTrace();
        }

        Bukkit.getServer().getConsoleSender().sendMessage(ConRef.getPlPrefix() + ConRef.tacc(" &cDiscordConsole has been successfully disabled!"));
    }

    public void loadConfig() {
        this.reloadConfig();
        if (this.getConfig().isSet("channelId") && !this.getConfig().isSet("channelIds")) {
            this.getConfig().set("channelIds", new String[]{this.getConfig().getString("channelId")});
            this.getConfig().set("channelId", null);
        }

        if (this.getConfig().isSet("channelIds")) {
            ArrayList list = (ArrayList) this.getConfig().getStringList("channelIds");
            this.getConfig().set("channelIds", null);
            ConfigurationSection chanSect = this.getConfig().createSection("channels");
            Object[] channelIds = list.toArray();

            for (Object k : channelIds) {
                if (!chanSect.isSet(k + ".filter")) {
                    chanSect.set(k + ".filter", "");
                }

                if (!chanSect.isSet(k + ".topic")) {
                    chanSect.set(k + ".topic", "Players online: %player_count%/%player_max%");
                }
            }

            this.saveConfig();
            this.reloadConfig();
        }

        if (!this.getDataFolder().exists()) {
            this.firstLoad = true;
            if (!this.getDataFolder().mkdir()) {
                this.getLogger().warning("Unable to create the DiscordConsole directory");
            }

            this.saveDefaultConfig();
        }

        if (requireNonNull(this.getConfig().getString("botStatus")).isEmpty()) {
            this.getConfig().set("botStatus", "online");
        }

        Object[] keys = this.getConfig().getKeys(false).toArray();

        String key;
        for (Object o : keys) {
            key = (String) o;
            if (key.charAt(0) == key.toUpperCase().charAt(0)) {
                String newK = key.substring(0, 1).toLowerCase() + key.substring(1);
                this.getConfig().set(newK, this.getConfig().get(key));
                this.getConfig().set(key, null);
            }
        }

        this.saveConfig();
        this.reloadConfig();
        this.getConfig().options().copyDefaults(true);
        ConfigurationSection chanSect = this.getConfig().getConfigurationSection("channels");
        if (chanSect != null) {
            ConfigurationSection finalChanSect = chanSect;
            chanSect.getKeys(false).forEach((kx) -> {
                if (kx.contains("0000000000000000")) {
                    finalChanSect.set(kx, null);
                }

                if (!finalChanSect.isSet(kx + ".filter")) {
                    finalChanSect.set(kx + ".filter", "");
                }

                if (!finalChanSect.isSet(kx + ".topic")) {
                    finalChanSect.set(kx + ".topic", "Players online: %player_count%/%player_max%");
                }

            });
        }

        if (!this.getConfig().isConfigurationSection("commands")) {
            this.getConfig().set("commands", null);
            this.getConfig().createSection("commands");
        }

        ConfigurationSection commands = this.getConfig().getConfigurationSection("commands");
        assert commands != null;
        if (commands.getKeys(false).size() < 1) {
            commands.set("discordlink.message", "https://discord.gg/WSaWztJ");
            commands.set("discordlink.enabled", false);
        }

        for (Iterator<String> iterator = commands.getKeys(false).iterator(); iterator.hasNext(); this.commandMap.register(key, "discordconsole", new CustomCommands(key, ChatColor.translateAlternateColorCodes('&', ConRef.replaceExpressions(commands.getString(key + ".message", "No command message found!"), true))))) {
            key = iterator.next();
            if (this.commandMap.getCommand(key) != null) {
                requireNonNull(this.commandMap.getCommand(key)).unregister(this.commandMap);
            }
        }

        this.getConfig().options().header("Configuration file for DiscordConsole\nMade by DeltaRays (DeltaRays#0054 on Discord)\n\n ------ botToken ------ \nThe discord bot's token, if you don't know how to get it this is how to:\nGo to https://discordapp.com/developers/applications/, if you haven't created the application yet create one, otherwise open the application\ngo to the Bot section and create a new bot, then copy the token and paste it in here\n\n ------ channels ------ \nThe channels the console logs are going to be sent to, this is a config example, in which messages are going to be sent to two channels:\nchannels:\n  '519911076691968006':\n    filter: 'joined'\n    topic: 'Players online: %player_count%/%player_max%'\n  '707215060744929280':\n    filter: ''\n    topic: '' \nOnly messages that contain 'joined' are going to be sent in the first channel. (Supports regex, if you don't know what regex is you should check it out at https://medium.com/factory-mind/regex-tutorial-a-simple-cheatsheet-by-examples-649dc1c3f285)\nAll messages are going to be sent in the second channel\nif you don't know how to get a channel id this is how to:\ngo to your discord settings, in the Appearance tab scroll down to Advanced and there enable Developer Mode,\nExit the settings and right click on the channel you want the logs to send to,\nand click on \"Copy ID\" to copy the id, and paste it in here\n\n ------ channelRefreshRate ------\n In seconds, every how often the logs should be sent to the channel (minimum is 1 second)\n\n ------ consoleCommandsEnabled ------\n Whether anything typed in the console channel should be sent to the server as a console command, can be either true or false\n\n ------ sendStartupMessages ------ \nWhether the server startup messages should be sent to the discord console\n\n ------ checkForUpdates ------\nWhether or not new updates should be checked automatically (you can always just )\n\n ------ prefix ------\nChange the plugin's prefix to anything you'd like!\n\n ------ botStatus ------ \nThe bot's status (can be online, dnd (do not disturb), idle or invisible)\n\n ------ botActivity ------\nCan be PLAYING or STREAMING or LISTENING, will be put before the bot's status text\n\n ------ botStatusText ------ \nThe bot's status's text (What you see under their name), added after Playing/Streaming/Listening (defined in botActivity) (example: Playing 'on Minecraft').\n\n ------ debug ------ \nWhether the debug messages should be sent in console\n\n------ commands ------\nYou'll be able to add custom commands that send a message to the player using this.\nExample:\ncommands:\n ping:\n   message: pong\n discord:\n   message: 'Join our discord server at https://discord.gg/WSaWztJ'\nThis will add two commands (/ping and /discord) that will send pong and Join our discord server at https://discord.gg/WSaWztJ respectively\nNote: if your message has : you need to surround it with \" or '\n\nPlaceholders: You can user placeholders to customize the messages, placeholders work for commands, botStatusText and for the channel topics\n%player_count%: Sends the unvanished player count (supports SuperVanish, VanishNoPackets, PremiumVanish and a few more vanish plugins)\n%player_max%: Wends the maximum online players\n%date%: Sends the time\n%total_players%: Sends the number of players to have ever joined the server.\n%motd%: Sends the server motd\nYou can also use all PlaceholderAPI placeholders");
        if (!this.getConfig().isSet("channels") && !this.getConfig().isSet("channelIds") && !this.getConfig().isSet("channelId")) {
            ConfigurationSection chanSecti = this.getConfig().createSection("channels");
            chanSecti.set("000000000000000000.filter", "");
            chanSecti.set("000000000000000000.topic", "Players online: %player_count%/%player_max%");
        }

        this.saveConfig();
        this.reloadConfig();
        chanSect = this.getConfig().getConfigurationSection("channels");
        ConfigurationSection finalChanSect1 = chanSect;
        assert chanSect != null;
        chanSect.getKeys(false).forEach((kx) -> {
            if (kx.contains("0000000000000000")) {
                finalChanSect1.set(kx, null);
            }

            if (!finalChanSect1.isSet(kx + ".filter")) {
                finalChanSect1.set(kx + ".filter", "");
            }

            if (!finalChanSect1.isSet(kx + ".topic")) {
                finalChanSect1.set(kx + ".topic", "Players online: %player_count%/%player_max%");
            }

        });
        this.workingChannels = new HashMap<>();
        Object[] channels = chanSect.getKeys(false).toArray();

        for (Object cId : channels) {
            MemorySection config = (MemorySection) chanSect.get((String) cId);
            HashMap<String, Object> configMap = new HashMap<>();

            assert config != null;

            for (String keyy : config.getKeys(false)) {
                configMap.put(keyy, config.get(keyy));
            }

            if (!((String) cId).contains("0000000000000000")) {
                this.workingChannels.put((String) cId, configMap);
            }
        }

        if (!this.getConfig().getBoolean("sendStartupMessages")) {
            this.sendDiscordMessage("Server is starting up!");
        }

        if (chanSect.getKeys(false).size() >= 1 && !chanSect.getKeys(false).toArray()[0].toString().contains("00000000000000000")) {
            if (Objects.equals(this.getConfig().getString("botToken"), "TOKEN")) {
                this.firstLoad = true;
                this.getLogger().severe(ChatColor.DARK_RED + "No bot token was provided! Go to the plugins folder, DiscordConsole, config.yml to set the bot token");
            }
        } else {
            this.firstLoad = true;
            this.getLogger().severe(ChatColor.DARK_RED + "No channel id was provided! Go to the plugins folder, DiscordConsole, config.yml to set the channel id");
        }

    }

    public void updateChannelTopic() {
        OkHttpClient client = new OkHttpClient();
        Object[] workingChannelsArr = this.workingChannels.keySet().toArray();

        for (Object cId : workingChannelsArr) {
            try {
                String channelId = (String) cId;
                String topic = (String) (this.workingChannels.get(cId)).get("topic");
                JSONObject json = new JSONObject();
                json.put("topic", ConRef.replaceExpressions(topic, false));
                RequestBody body = RequestBody.create(MediaType.get("application/json"), json.toString());
                Request request = (new Builder()).url("https://discordapp.com/api/v6/channels/" + channelId).patch(body).addHeader("Authorization", "Bot " + this.getConfig().getString("botToken")).build();
                Response response = client.newCall(request).execute();
                int responseCode = response.code();
                if (responseCode == 404) {
                    this.getLogger().severe("Channel with id " + channelId + " doesn't exist!");
                    this.workingChannels.remove(channelId);
                } else if (responseCode == 403) {
                    this.getLogger().severe("The bot can't edit " + channelId + "'s channel topic!");
                    this.workingChannels.remove(channelId);
                }

                response.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

    }

    public void sendDiscordMessage(String message) {
        OkHttpClient client = new OkHttpClient();
        Object[] workingChannelArr = this.workingChannels.keySet().toArray();

        for (Object cId : workingChannelArr) {
            try {
                String channelId = (String) cId;
                String filter = (String) (this.workingChannels.get(cId)).get("filter");
                Pattern pattern = Pattern.compile(filter);
                JSONObject json;
                if (!filter.isEmpty()) {
                    StringBuilder newB = new StringBuilder();
                    String[] messages = message.split("\n");

                    for (String line : messages) {
                        if (pattern.matcher(line).find()) {
                            newB.append(line);
                        }
                    }

                    if (!newB.toString().isEmpty()) {
                        json = new JSONObject();
                        JSONObject json2 = new JSONObject();
                        JSONArray jsonArr = new JSONArray();
                        json2.put("parse", jsonArr);
                        json.put("content", newB.toString());
                        json.put("allowed_mentions", json2);
                        RequestBody body = RequestBody.create(MediaType.get("application/json"), json.toString());
                        Request request = (new Builder()).url("https://discordapp.com/api/v6/channels/" + channelId + "/messages").post(body).addHeader("Authorization", "Bot " + this.getConfig().getString("botToken")).build();
                        Response response = client.newCall(request).execute();
                        int responseCode = response.code();
                        if (responseCode == 404) {
                            this.getLogger().severe("Channel with id " + channelId + " doesn't exist!");
                            this.workingChannels.remove(channelId);
                        } else if (responseCode == 403) {
                            this.getLogger().severe("The bot can't type in " + channelId + "!");
                            this.workingChannels.remove(channelId);
                        }

                        response.close();
                    }
                } else {
                    json = new JSONObject();
                    JSONArray jsonArr = new JSONArray();
                    json.put("parse", jsonArr);
                    json.put("content", message);
                    json.put("allowed_mentions", json);
                    RequestBody body = RequestBody.create(MediaType.get("application/json"), json.toString());
                    Request request = (new Builder()).url("https://discordapp.com/api/v6/channels/" + channelId + "/messages").post(body).addHeader("Authorization", "Bot " + this.getConfig().getString("botToken")).build();
                    Response response = client.newCall(request).execute();
                    int responseCode = response.code();
                    if (responseCode == 404) {
                        this.getLogger().severe("Channel with id " + channelId + " doesn't exist!");
                        this.workingChannels.remove(channelId);
                    } else if (responseCode == 403) {
                        this.getLogger().severe("The bot can't type in " + channelId + "!");
                        this.workingChannels.remove(channelId);
                    }

                    response.close();
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
                if (interf.isUp() && !interf.isLoopback()) {
                    return true;
                }
            }
        } catch (Exception exception) {
            this.getLogger().warning("Error in checking internet connection: " + exception.getMessage());
            if (this.getConfig().getBoolean("debug")) {
                exception.printStackTrace();
            }
        }

        return false;
    }

    public void updateBotStatus() {
        if (this.socket != null && this.socket.isOpen() && this.socket.isConnected()) {
            int type = 0;
            String botActivity = this.getConfig().getString("botActivity");

            assert botActivity != null;

            if (botActivity.equalsIgnoreCase("listening")) {
                type = 1;
            } else if (botActivity.equalsIgnoreCase("streaming")) {
                type = 2;
            } else if (botActivity.equalsIgnoreCase("watching")) {
                type = 3;
            }

            String botStatus = this.getConfig().getString("botStatus");
            ArrayList<String> statuses = new ArrayList<>(Arrays.asList("online", "dnd", "invisible", "idle"));
            if (!statuses.contains(botStatus)) {
                botStatus = "online";
            }

            JSONObject game = new JSONObject();
            game.put("name", ConRef.replaceExpressions(this.getConfig().getString("botStatusText"), true));
            game.put("type", Integer.valueOf(type));
            JSONObject presResp = new JSONObject();
            if (!requireNonNull(this.getConfig().getString("botStatusText")).isEmpty()) {
                presResp.put("game", game);
            }

            presResp.put("status", botStatus);
            presResp.put("afk", false);
            presResp.put("since", null);
            JSONObject resp = new JSONObject();
            resp.put("op", 3);
            resp.put("d", presResp);
            String update = resp.toJSONString();
            this.socket.send(update);
        }

    }
}
