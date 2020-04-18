package io.github.deltarays.discordconsole;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

public class DiscordSocket extends WebSocketClient {
    String lastS;
    Boolean isInvalid = false;
    String botId = "";
    Main main;
    public DiscordSocket(URI serverUri, Main main) {
        super(serverUri);
        this.main = main;
    }


    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        if(main.getConfig().getBoolean("Debug")) {
        main.getLogger().info("[WebSocket] Socket opened!");
        }
        //System.out.println("Connected to the WebSocket!");
    }

    @Override
    public void onMessage(String msg) {
        Double op = Double.valueOf(msg.replaceAll(".*\"op\":(.+?)(,|\\{).*", "$1"));
        String s = msg.replaceAll(".*\"s\":(.+?)(,|\\{).*", "$1");
        if(s != "null"){
            lastS = s;
        }
        if(main.getConfig().getString("BotToken").equals("TOKEN")){
            isInvalid = true;
            Bukkit.getScheduler().runTask(main, () -> {
                main.getLogger().severe(ChatColor.DARK_RED +"No bot token was provided! Go to the plugins folder, DiscordConsole, config.yml to set the bot token");
                main.getServer().getPluginManager().disablePlugin(main);
            });
        }
        if(!main.getConfig().getString("BotToken").matches("^(?i)[a-z0-9.\\-_]{32,100}$")){
            isInvalid = true;
            Bukkit.getScheduler().runTask(main, () -> {
                main.getLogger().severe(ChatColor.DARK_RED +"An invalid bot token was provided! Go to the plugins folder, DiscordConsole, config.yml to modify the bot token");
                main.getServer().getPluginManager().disablePlugin(main);
            });
        }
        String t =  msg.replaceAll(".*\"t\":(.+?)(,|\\{).*", "$1");
        if(t.contains("READY")){
            main.getLogger().info("Successfully connected to the bot!");
            botId =  msg.replaceFirst(".*\"user\":\\{.*\"id\":\"(.+?)\"(,|\\{).*", "$1");
        } else if(op== 10){
            Double heartbeat_interval = Double.valueOf(msg.replaceAll(".*\"heartbeat_interval\":(.+?)(,|\\{).*", "$1"));
            Integer hb_i = Math.toIntExact(Math.round(heartbeat_interval));
            main.getLogger().info("Connecting to the discord bot");
            String resp = String.format("{\"op\":2, \"d\": {\"token\":\"%s\", \"properties\": {\"$os\": \"linux\", \"$browser\": \"my_library\", \"$device\":\"my_library\"}}}", main.getConfig().getString("BotToken"));
            send(resp);
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run() {
                    try {
                        if(isInvalid) {
                            timer.cancel();
                            timer.purge();
                        } else {
                            if(main.getConfig().getBoolean("Debug")) {
                                main.getLogger().info("[Discord WebSocket] Heartbeat sent");
                            }
                            String s = String.format("{\"op\": 1, \"d\": %s}", lastS);
                            send(s);

                        }
                    } catch(Exception e){
                        if(main.getConfig().getBoolean("Debug")){
                            main.getLogger().severe("[WebSocket] Error in sending heartbeat!\n"+ e.toString());
                        }
                    }
                }
            },hb_i -1,hb_i);
        } else if(t.contains("MESSAGE_CREATE")){
            if(main.getConfig().getBoolean("ConsoleCommandsEnabled")){
                String channelId = msg.replaceFirst(".*\"channel_id\":\"(.+?)\"(,|\\{).*", "$1");
                if(channelId.equals(main.getConfig().getString("ChannelId"))) {
                    String senderId =  msg.replaceFirst(".*\"author\":\\{.*\"id\":\"(.+?)\"(,|\\{).*", "$1");
                    if(!senderId.equals(botId)) {
                        final String content = msg.replaceAll(".*\"content\":\"(.+?)\"(,|\\{).*", "$1").replaceAll("\\\\\"", "\"").replaceAll("\\\\\\\\", "\\\\");
                        try {
                            Bukkit.getScheduler().runTask(main, new Runnable() {
                                @Override
                                public void run() {
                                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), content);
                                }
                            });
                        } catch(Exception e){
                            main.getLogger().severe(e.toString());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if(main.getConfig().getBoolean("Debug")) {
            main.getLogger().info(String.format("WebSocket closed! Code: %s Reason: %s", code, reason));
        }
        if(code == 4004){
            isInvalid = true;
            main.getLogger().severe("An invalid discord bot token was provided!");
            return;
        } else if(code == 1001){
            String token = main.getConfig().getString("BotToken");
            if(!token.matches("^(?i)[a-z0-9.\\-_]{32,100}$")){
                main.getLogger().severe("Invalid token provided");
                main.getServer().getPluginManager().disablePlugin(main);
            }
            main.getLogger().info("Disconnected from WebSocket, reconnecting...");
            String resp = String.format("{\"op\":2, \"d\": {\"token\":\"%s\", \"properties\": {\"$os\": \"linux\", \"$browser\": \"my_library\", \"$device\":\"my_library\"}}}", token);
            send(resp);
        } else {
            main.getLogger().severe(String.format("[Discord WebSocket] Disconnected\nCode: %s\nReason: %s", code, reason));
        }

    }

    @Override
    public void onError(Exception e) {
        if(main.getConfig().getBoolean("Debug")) {
            main.getLogger().severe(String.format("WebSocket error!\n%s", e.toString()));
        } else if(isInvalid){
            main.getLogger().severe(e.toString());
        }
    }
}