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
    DiscordConsole main;
    String sessionId = "";
    Boolean resume;
    Boolean ackReceived = true;
    Timer timer;
    private Boolean isOpening;

    public Boolean isOpening() {
        return isOpening;
    }

    public DiscordSocket(URI serverUri, DiscordConsole main, String sessionId) {
        super(serverUri);
        this.isOpening = true;
        this.main = main;
        this.sessionId = sessionId;
        this.resume = true;
    }

    public DiscordSocket(URI serverUri, DiscordConsole main) {
        super(serverUri);
        this.isOpening = true;
        this.main = main;
        this.resume = false;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        if (main.getConfig().getBoolean("Debug")) {
            main.getLogger().info("[WebSocket] Socket opened!");
        }
        isOpening = false;
        //System.out.println("Connected to the WebSocket!");
    }

    @Override
    public void onMessage(String msg) {
        double op = Double.valueOf(msg.replaceAll(".*\"op\":(.+?)(,|\\{).*", "$1"));
        String s = msg.replaceAll(".*\"s\":(.+?)(,|\\{).*", "$1");
        if (s != "null") {
            lastS = s;
        }
        if (!main.getConfig().getString("BotToken").matches("^(?i)[a-z0-9.\\-_]{32,100}$")) {
            isInvalid = true;
            Bukkit.getScheduler().runTask(main, () -> {
                main.getLogger().severe(ChatColor.DARK_RED + "An invalid bot token was provided! Go to the plugins folder, DiscordConsole, config.yml to modify the bot token");
            });
        }
        String t = msg.replaceAll(".*\"t\":(.+?)(,|\\{).*", "$1");
        if (t.contains("READY")) {
            main.getLogger().info("Successfully connected to the bot!");
            if (!resume) sessionId = msg.replaceAll(".*\"session_id\":(.+?)(,|\\{).*", "$1");
            botId = msg.replaceFirst(".*\"user\":\\{.*\"id\":\"(.+?)\"(,|\\{).*", "$1");
        } else if (op == 10) {
            double heartbeat_interval = Double.valueOf(msg.replaceAll(".*\"heartbeat_interval\":(.+?)(,|\\{).*", "$1"));
            int hb_i = Math.toIntExact(Math.round(heartbeat_interval));
            main.getLogger().info("Connecting to the discord bot");
            if (!resume) {
                String resp = String.format("{\"op\":2, \"d\": {\"token\":\"%s\", \"properties\": {\"$os\": \"linux\", \"$browser\": \"DiscordSocket\", \"$device\":\"DiscordSocket\",\"$referrer\":\"\",\"$referring_domain\":\"\"},\"compress\":false}}", main.getConfig().getString("BotToken"));
                send(resp);
            } else {
                String resp = String.format("{\"op\":6,\"d\":{\"token\":\"%s\",\"session_id\":%s,\"seq\":%s}}", main.getConfig().getString("BotToken"), sessionId, lastS);
                send(resp);
            }
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run() {
                    try {
                        if (!ackReceived) {
                            if (main.getConfig().getBoolean("Debug")) {
                                main.getLogger().info("[Discord WebSocket] Hearbeat ack not received! Terminating the connection and reconnecting!");
                                main.socketConnect(lastS);
                                close(1002);
                                timer.cancel();
                                timer.purge();
                            }
                        } else if (isInvalid) {
                            timer.cancel();
                            timer.purge();
                        } else {
                            if (main.getConfig().getBoolean("Debug")) {
                                main.getLogger().info("[Discord WebSocket] Heartbeat sent");
                            }
                            String s = String.format("{\"op\": 1, \"d\": %s}", lastS);
                            send(s);
                            ackReceived = false;

                        }
                    } catch (Exception e) {
                        timer.cancel();
                        timer.purge();
                        main.getLogger().severe("[Discord WebSocket] Error in sending heartbeat!\n" + e.toString());
                    }
                }
            }, hb_i - 1, hb_i);
        } else if (t.contains("MESSAGE_CREATE")) {
            if (main.getConfig().getBoolean("ConsoleCommandsEnabled")) {
                String channelId = msg.replaceFirst(".*\"channel_id\":\"(.+?)\"(,|\\{).*", "$1");
                if (channelId.equals(main.getConfig().getString("ChannelId"))) {
                    String senderId = msg.replaceFirst(".*\"author\":\\{.*\"id\":\"(.+?)\"(,|\\{).*", "$1");
                    if (!senderId.equals(botId)) {
                        final String content = msg.replaceAll(".*\"content\":\"(.+?)\"(,|\\{).*", "$1").replaceAll("\\\\\"", "\"").replaceAll("\\\\\\\\", "\\\\");
                        try {
                            Bukkit.getScheduler().runTask(main, new Runnable() {
                                @Override
                                public void run() {
                                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), content);
                                }
                            });
                        } catch (Exception e) {
                            main.getLogger().severe("[Discord WebSocket] Error in running command from console!\n" + e.toString());
                        }
                    }
                }
            }
        } else if (op == 11) {
            ackReceived = true;
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (main.getConfig().getBoolean("Debug")) {
            main.getLogger().info(String.format("[Discord WebSocket] Closed! Code: %s Reason: %s", code, reason));
        }
        if (code == 4004) {
            isInvalid = true;
            main.getLogger().severe("An invalid discord bot token was provided!");
        } else if (code == 1001 || code == 1006) {
            String token = main.getConfig().getString("BotToken");
            if (!token.matches("^(?i)[a-z0-9.\\-_]{32,100}$")) {
                main.getLogger().severe("Invalid token provided");
            }
            main.getLogger().info("[Discord WebSocket] Got disconnected, reconnecting...");
            try {
                main.socketConnect(sessionId);
            } catch (Exception e) {
                main.getLogger().severe("[Discord WebSocket] Failure to reconnect!\n" + e.toString());
            }
        } else if (code != 1002) {
            main.getLogger().severe(String.format("[Discord WebSocket] WebSocket closed!\nCode: %s\nReason: %s", code, reason));

            try {

                main.socketConnect(sessionId);
            } catch (Exception e) {
                main.getLogger().severe("[Discord WebSocket] Failure to reconnect!\n" + e.toString());
            }
        } else {
            timer.cancel();
            timer.purge();
        }

    }

    @Override
    public void onError(Exception e) {
        if (main.getConfig().getBoolean("Debug")) {
            main.getLogger().severe(String.format("WebSocket error!\n%s", e.toString()));
        } else if (isInvalid) {
            main.getLogger().severe(e.toString());
        }
    }
}