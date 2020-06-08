//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package tk.deltarays.discordconsole;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.URI;
import java.util.*;

public class DiscordSocket extends WebSocketClient {
    public Timer timer;
    String lastS;
    Boolean isInvalid = false;
    String botId = "";
    DiscordConsole main;
    String sessionId = "";
    Boolean resume;
    Boolean ackReceived = true;
    private Boolean isOpening = true;
    private Boolean isConnected = false;

    public DiscordSocket(URI serverUri, DiscordConsole main, String sessionId) {
        super(serverUri);
        this.main = main;
        this.sessionId = sessionId;
        this.resume = true;
    }

    public DiscordSocket(URI serverUri, DiscordConsole main) {
        super(serverUri);
        this.main = main;
        this.resume = false;
    }

    public Boolean isOpening() {
        return this.isOpening;
    }

    public void onOpen(ServerHandshake serverHandshake) {
        if (this.main.getConfig().getBoolean("debug")) {
            this.main.getLogger().info("[WebSocket] Socket opened!");
        }

        this.isOpening = false;
    }

    public void onMessage(String msg) {
        JSONParser parser = new JSONParser();
        JSONObject parsed = new JSONObject();

        try {
            parsed = (JSONObject) parser.parse(msg);
        } catch (ParseException e) {
            this.main.getLogger().severe("[Discord WebSocket] Error encountered in parsing json payload");
            e.printStackTrace();
        }

        long op = (Long) parsed.get("op");
        String s = String.valueOf(parsed.get("s"));
        if (s != null) {
            this.lastS = s;
        }

        if (this.main.getConfig().getString("botToken") == null || !Objects.requireNonNull(this.main.getConfig().getString("botToken")).matches("^(?i)[a-z0-9.\\-_]{32,100}$")) {
            this.isInvalid = true;
            Bukkit.getScheduler().runTask(this.main, () -> this.main.getLogger().severe(ChatColor.DARK_RED + "An invalid bot token was provided! Go to the plugins folder, DiscordConsole, config.yml to modify the bot token"));
        }
        System.out.println(op);
        String content;
        if (op == 10L) {
            JSONObject d = (JSONObject) parsed.get("d");
            long heartbeat_interval = (Long) d.get("heartbeat_interval");
            int hb_i = Math.toIntExact(Math.round((float) heartbeat_interval));
            Bukkit.getServer().getConsoleSender().sendMessage(ConRef.getPlPrefix() + ChatColor.YELLOW + " Connecting to the discord bot...");
            JSONObject response;
            if (!this.resume) {
                content = this.main.getConfig().getString("botStatus");
                ArrayList statuses = new ArrayList(Arrays.asList("online", "dnd", "invisible", "idle"));
                if (!statuses.contains(content)) {
                    content = "online";
                }

                JSONObject game = new JSONObject();
                game.put("name", ConRef.replaceExpressions(this.main.getConfig().getString("botStatusText"), true));
                game.put("type", 0);
                JSONObject presResp = new JSONObject();
                if (this.main.getConfig().getString("botStatusText") != null && !this.main.getConfig().getString("botStatusText").isEmpty()) {
                    presResp.put("game", game);
                }

                presResp.put("status", content);
                JSONObject dProps = new JSONObject();
                dProps.put("$os", "linux");
                dProps.put("$browser", "DiscordSocket");
                dProps.put("$device", "DiscordConsole");
                JSONObject dResp = new JSONObject();
                dResp.put("token", this.main.getConfig().getString("botToken"));
                dResp.put("properties", dProps);
                dResp.put("presence", presResp);
                response = new JSONObject();
                response.put("op", 2);
                response.put("d", dResp);
            } else {
                JSONObject dResp = new JSONObject();
                dResp.put("token", this.main.getConfig().getString("botToken"));
                dResp.put("session_id", this.sessionId);
                dResp.put("seq", this.lastS);
                response = new JSONObject();
                response.put("op", 6);
                response.put("d", dResp);
            }

            this.send(response.toJSONString());
            this.timer = new Timer();
            this.timer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    try {
                        if (!DiscordSocket.this.ackReceived) {
                            if (DiscordSocket.this.main.getConfig().getBoolean("debug")) {
                                DiscordSocket.this.main.getLogger().info("[Discord WebSocket] Hearbeat ack not received! Terminating the connection and reconnecting!");
                                DiscordSocket.this.main.socketConnect(DiscordSocket.this.lastS);
                                DiscordSocket.this.close(1002);
                                DiscordSocket.this.timer.cancel();
                                if (DiscordSocket.this.timer != null) {
                                    DiscordSocket.this.timer.purge();
                                }
                            }
                        } else if (!DiscordSocket.this.isInvalid && !DiscordSocket.this.isClosed() && !DiscordSocket.this.isClosing()) {
                            if (DiscordSocket.this.main.getConfig().isSet("debug") && DiscordSocket.this.main.getConfig().getBoolean("debug")) {
                                DiscordSocket.this.main.getLogger().info("[Discord WebSocket] Heartbeat sent");
                            }

                            String s = String.format("{\"op\": 1, \"d\": %s}", DiscordSocket.this.lastS != null ? DiscordSocket.this.lastS : "null");
                            DiscordSocket.this.send(s);
                            DiscordSocket.this.ackReceived = false;
                        } else {
                            DiscordSocket.this.timer.cancel();
                            DiscordSocket.this.timer.purge();
                        }
                    } catch (Exception e) {
                        DiscordSocket.this.main.getLogger().severe("[Discord WebSocket] Error in sending heartbeat!");
                        e.printStackTrace();
                        if (DiscordSocket.this.timer != null) {
                            DiscordSocket.this.timer.cancel();
                            DiscordSocket.this.timer.purge();
                        }
                    }

                }
            }, hb_i - 1, hb_i);
        } else if (op == 9L) {
            final boolean d = (Boolean) parsed.get("d");
            Timer t = new Timer();
            t.schedule(new TimerTask() {
                public void run() {
                    if (!d) {
                        DiscordSocket.this.main.socketConnect();
                    } else {
                        DiscordSocket.this.main.socketConnect(DiscordSocket.this.sessionId);
                    }

                }
            }, (long) (Math.random() * 4000.0D) + 1000L);
        } else if (op == 11L) {
            this.ackReceived = true;
        } else if (op == 0L) {
            String t = (String) parsed.get("t");
            JSONObject d = (JSONObject) parsed.get("d");
            if (t.contains("READY")) {
                this.isConnected = true;
                Bukkit.getServer().getConsoleSender().sendMessage(ConRef.getPlPrefix() + ChatColor.GREEN + " Successfully connected to the bot!");
                this.sessionId = (String) parsed.get("session_id");
                JSONObject user = (JSONObject) d.get("user");
                this.botId = (String) user.get("id");
            } else if (t.contains("RESUME")) {
                this.isConnected = true;
                this.resume = false;
            } else if (t.contains("MESSAGE_CREATE") && this.main.getConfig().getBoolean("consoleCommandsEnabled")) {
                String channelId = (String) d.get("channel_id");
                if (this.main.workingChannels.containsKey(channelId)) {
                    JSONObject author = (JSONObject) d.get("author");
                    String senderId = (String) author.get("id");
                    if (!senderId.equalsIgnoreCase(this.botId)) {
                        content = ((String) d.get("content")).replaceAll("\\\\\"", "\"").replaceAll("\\\\\\\\", "\\\\");

                        try {
                            String finalContent = content;
                            Bukkit.getScheduler().runTask(this.main, () -> Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), finalContent));
                        } catch (Exception e) {
                            this.main.getLogger().severe("[Discord WebSocket] Error in running command from console!");
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

    }

    public Boolean isConnected() {
        return this.isConnected;
    }

    public void onClose(int code, String reason, boolean remote) {
        this.isConnected = false;
        if (this.timer != null) {
            this.timer.cancel();
            this.timer.purge();
        }

        if (this.main.getConfig().getBoolean("debug")) {
            this.main.getLogger().info(String.format("[Discord WebSocket] Closed! Code: %s Reason: %s", code, reason));
        }

        if (code == 4004) {
            this.isInvalid = true;
            this.main.getLogger().severe("An invalid discord bot token was provided!");
        } else if (code != 1001 && code != 1006) {
            if (code != 1002) {
                this.main.getLogger().severe(String.format("[Discord WebSocket] WebSocket closed!\nCode: %s\nReason: %s", code, reason));

                try {
                    this.main.socketConnect(this.sessionId);
                } catch (Exception e) {
                    this.main.getLogger().severe("[Discord WebSocket] Failure to reconnect!");
                    e.printStackTrace();
                }
            }
        } else {
            String token = this.main.getConfig().getString("botToken");
            if (token != null && !token.matches("^(?i)[a-z0-9.\\-_]{32,100}$")) {
                this.main.getLogger().severe("Invalid token provided");
            }

            this.main.getLogger().info("[Discord WebSocket] Got disconnected, reconnecting...");
            this.main.socketConnect(this.sessionId);
        }

    }

    public void onError(Exception e) {
        this.isConnected = false;
        this.main.getLogger().severe("[Discord Websocket] Error encountered!");
        e.printStackTrace();
    }
}
