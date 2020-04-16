package io.github.deltarays.discordconsole;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.json.simple.JSONObject;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class LogAppender extends AbstractAppender {
    Main main;
    Queue<String> msgs = new LinkedList<String>();
    public LogAppender(Main main) {
        super("DiscordConsoleLogAppender", null, null);
        start();
        this.main = main;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    StringBuilder message = new StringBuilder();
                    for (String ln = msgs.poll(); ln != null; ln = msgs.poll()) {
                        if (message.length() + ln.length() > 1999) {
                            JSONObject json = new JSONObject();
                            json.put("content", message.toString());
                            URL endpoint = new URL(String.format("https://discordapp.com/api/v6/channels/%s/messages", main.getConfig().getString("ChannelId")));
                            String params = json.toString();
                            HttpURLConnection con = (HttpURLConnection) endpoint.openConnection();
                            con.setRequestMethod("POST");
                            con.setRequestProperty("Content-Type", "application/json");
                            con.setRequestProperty("user-agent", "");
                            con.setRequestProperty("Authorization", "Bot " + main.getConfig().getString("BotToken"));
                            con.setDoOutput(true);
                            OutputStream out = con.getOutputStream();
                            out.write(params.getBytes());
                            out.flush();
                            out.close();
                            Integer responsecode = con.getResponseCode();
                            if(responsecode == 404){
                                Bukkit.getScheduler().runTask(main, () -> {
                                    main.getLogger().severe("An incorrect channel id was provided!");
                                    main.getServer().getPluginManager().disablePlugin(main);
                                });
                            }
                            message = new StringBuilder();
                        }
                        message.append(ln).append("\n");
                    }
                    if(StringUtils.isNotBlank(message.toString().replace("\n", ""))){
                        JSONObject json = new JSONObject();
                        json.put("content", message.toString());
                        URL endpoint = new URL(String.format("https://discordapp.com/api/v6/channels/%s/messages", main.getConfig().getString("ChannelId")));
                        String params = json.toString();
                        HttpURLConnection con = (HttpURLConnection) endpoint.openConnection();
                        con.setRequestMethod("POST");
                        con.setRequestProperty("Content-Type", "application/json");
                        con.setRequestProperty("user-agent", "");
                        con.setRequestProperty("Authorization", "Bot " + main.getConfig().getString("BotToken"));
                        con.setDoOutput(true);
                        OutputStream out = con.getOutputStream();
                        out.write(params.getBytes());
                        out.flush();
                        out.close();
                        Integer responsecode = con.getResponseCode();
                        if(responsecode == 404){
                            Bukkit.getScheduler().runTask(main, () -> {
                                main.getLogger().severe("An incorrect channel id was provided!");
                                main.getServer().getPluginManager().disablePlugin(main);
                            });
                        }
                    };
                } catch(Exception e){
                    main.getLogger().warning(e.toString());
                }
            }
        }, 0, (main.getConfig().getInt("ChannelRefreshRate") >= 1 ? main.getConfig().getInt("ChannelRefreshRate") : 2) * 1000);
    }
    @Override
    public void append(LogEvent event) {
        LogEvent log = event.toImmutable();
        String message = log.getMessage().getFormattedMessage();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        message = String.format("[%s] [%s/%s] %s", formatter.format(new Date(event.getTimeMillis())), event.getThreadName(), event.getLevel().toString(), message);
        message = message.replaceAll("\\[m|\\[([0-9]{1,2}[;m]?){3}|\u001b+", "").replaceAll("\\x1b\\[[0-9;]*[A-Za-z]\\]*", "").replace("_", "\\_").replace("*", "\\*").replace("~", "\\~");;
        msgs.add(message);
    }
}