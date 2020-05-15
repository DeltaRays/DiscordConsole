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
    String lastS;
    Boolean isInvalid = false;
    String botId = "";
    DiscordConsole main;
    String sessionId = "";
    Boolean resume;
    Boolean ackReceived = true;
    Timer timer;
    private Boolean isOpening;
    private Boolean isConnected = false;

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

    public Boolean isOpening() {
        return isOpening;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        if (main.getConfig().getBoolean("debug")) {
            main.getLogger().info("[WebSocket] Socket opened!");
        }
        isOpening = false;
    }

    @Override
    public void onMessage(String msg) {
        JSONParser parser = new JSONParser();
        JSONObject parsed = new JSONObject();
        try {
            parsed = (JSONObject) parser.parse(msg);
        } catch (ParseException e) {
            main.getLogger().severe("[Discord WebSocket] Error encountered in parsing json payload");
            e.printStackTrace();
        }
        long op = (long) parsed.get("op");
        String s = String.valueOf(parsed.get("s"));
        if (s != null) {
            lastS = s;
        }
        if (main.getConfig().getString("botToken") == null || !Objects.requireNonNull(main.getConfig().getString("botToken")).matches("^(?i)[a-z0-9.\\-_]{32,100}$")) {
            isInvalid = true;
            Bukkit.getScheduler().runTask(main, () -> main.getLogger().severe(ChatColor.DARK_RED + "An invalid bot token was provided! Go to the plugins folder, DiscordConsole, config.yml to modify the bot token"));
        }
        if (op == 10) {
            JSONObject d = (JSONObject) parsed.get("d");
            long heartbeat_interval = (long) d.get("heartbeat_interval");
            int hb_i = Math.toIntExact(Math.round(heartbeat_interval));
            Bukkit.getServer().getConsoleSender().sendMessage(ConRef.getPlPrefix() + " §eConnecting to the discord bot...");
            JSONObject response;
            if (!resume) {
                String botStatus = main.getConfig().getString("botStatus");
                ArrayList<String> statuses = new ArrayList<>(Arrays.asList("online", "dnd", "invisible", "idle"));
                if (!statuses.contains(botStatus)) botStatus = "online";
                JSONObject game = new JSONObject();
                game.put("name", ConRef.replaceExpressions(main.getConfig().getString("botStatusText"), true));
                game.put("type", 0);
                JSONObject presResp = new JSONObject();
                if (main.getConfig().getString("botStatusText") != null && !main.getConfig().getString("botStatusText").isEmpty()) presResp.put("game", game);
                presResp.put("status", botStatus);
                JSONObject dProps = new JSONObject();
                dProps.put("$os", "linux");
                dProps.put("$browser", "DiscordSocket");
                dProps.put("$device", "DiscordConsole");

                JSONObject dResp = new JSONObject();
                dResp.put("token", main.getConfig().getString("botToken"));
                dResp.put("properties", dProps);
                dResp.put("presence", presResp);
                response = new JSONObject();
                response.put("op", 2);
                response.put("d", dResp);
            } else {
                JSONObject dResp = new JSONObject();
                dResp.put("token", main.getConfig().getString("botToken"));
                dResp.put("session_id", sessionId);
                dResp.put("seq", lastS);
                response = new JSONObject();
                response.put("op", 6);
                response.put("d", dResp);
            }
            send(response.toJSONString());
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run() {
                    try {
                        if (!ackReceived) {
                            if (main.getConfig().getBoolean("debug")) {
                                main.getLogger().info("[Discord WebSocket] Hearbeat ack not received! Terminating the connection and reconnecting!");
                                main.socketConnect(lastS);
                                close(1002);
                                timer.cancel();
                                if (timer != null) timer.purge();
                            }
                        } else if (isInvalid || isClosed() || isClosing()) {
                            timer.cancel();
                            timer.purge();
                        } else {
                            if (main.getConfig().isSet("debug")) {
                                if (main.getConfig().getBoolean("debug"))
                                    main.getLogger().info("[Discord WebSocket] Heartbeat sent");
                            }
                            String s = String.format("{\"op\": 1, \"d\": %s}", lastS != null ? lastS : "null");
                            send(s);
                            ackReceived = false;
                        }
                    } catch (Exception e) {
                        main.getLogger().severe("[Discord WebSocket] Error in sending heartbeat!");
                        e.printStackTrace();
                        if (timer != null) {
                            timer.cancel();
                            timer.purge();
                        }
                    }
                }
            }, hb_i - 1, hb_i);
        } else if (op == 9) {
            boolean d = (boolean) parsed.get("d");
            Timer t = new Timer();
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!d) {
                        main.socketConnect();
                    } else {
                        main.socketConnect(sessionId);
                    }
                }
            }, (long) (Math.random() * 4000) + 1000);
        } else if (op == 11) {
            ackReceived = true;
        } else if (op == 0) {
            String t = (String) parsed.get("t");
            JSONObject d = (JSONObject) parsed.get("d");
            if (t.contains("READY")) {
                isConnected = true;
                Bukkit.getServer().getConsoleSender().sendMessage(ConRef.getPlPrefix() + " §aSuccessfully connected to the bot!");
                sessionId = (String) parsed.get("session_id");
                JSONObject user = (JSONObject) d.get("user");
                botId = (String) user.get("id");
            } else if (t.contains("RESUME")) {
                isConnected = true;
                resume = false;
            } else if (t.contains("MESSAGE_CREATE")) {
                if (main.getConfig().getBoolean("consoleCommandsEnabled")) {
                    String channelId = (String) d.get("channel_id");
                    if (main.workingChannels.containsKey(channelId)) {
                        JSONObject author = (JSONObject) d.get("author");
                        String senderId = (String) author.get("id");
                        if (!senderId.equalsIgnoreCase(botId)) {
                            String content = ((String) d.get("content")).replaceAll("\\\\\"", "\"").replaceAll("\\\\\\\\", "\\\\");
                            try {
                                Bukkit.getScheduler().runTask(main, () -> Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), content));
                            } catch (Exception e) {
                                main.getLogger().severe("[Discord WebSocket] Error in running command from console!");
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    public Boolean isConnected() {
        return isConnected;
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        isConnected = false;
        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
        if (main.getConfig().getBoolean("debug")) {
            main.getLogger().info(String.format("[Discord WebSocket] Closed! Code: %s Reason: %s", code, reason));
        }
        if (code == 4004) {
            isInvalid = true;
            main.getLogger().severe("An invalid discord bot token was provided!");
        } else if (code == 1001 || code == 1006) {
            String token = main.getConfig().getString("botToken");
            if (token != null && !token.matches("^(?i)[a-z0-9.\\-_]{32,100}$")) {
                main.getLogger().severe("Invalid token provided");
            }
            main.getLogger().info("[Discord WebSocket] Got disconnected, reconnecting...");
            try {
                main.socketConnect(sessionId);
            } catch (Exception e) {
                main.getLogger().severe("[Discord WebSocket] Failure to reconnect");
                e.printStackTrace();
            }
        } else if (code != 1002) {
            main.getLogger().severe(String.format("[Discord WebSocket] WebSocket closed!\nCode: %s\nReason: %s", code, reason));
            try {

                main.socketConnect(sessionId);
            } catch (Exception e) {
                main.getLogger().severe("[Discord WebSocket] Failure to reconnect!");
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onError(Exception e) {
        isConnected = false;
        main.getLogger().severe("[Discord Websocket] Error encountered!");
        e.printStackTrace();
    }
}