package io.github.deltarays.discordconsole;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.json.simple.JSONObject;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class LogAppender extends AbstractAppender  {
    DiscordConsole main;
    Boolean isInvalid = false;
    Boolean startupDone = false;
    Queue<String> msgs = new LinkedList<String>();
    public LogAppender(DiscordConsole main) {
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
                                    if(!isInvalid){
                                        main.getLogger().severe("An incorrect channel id was provided!");
                                    }
                                    isInvalid = true;

                                });
                            }
                            message = new StringBuilder();
                        }
                        message.append(ln).append("\n");
                    }
                    if(StringUtils.isNotBlank(message.toString().replace("\n", ""))){
                        HttpURLConnection con = main.sendDiscordMessage(message.toString());
                        int responseCode = con.getResponseCode();
                        if(responseCode == 404){
                            Bukkit.getScheduler().runTask(main, () -> {
                                if(!isInvalid){
                                    main.getLogger().severe("An incorrect channel id was provided!");
                                }
                                isInvalid = true;
                            });
                        }
                    };
                } catch(Exception e){
                    main.getLogger().severe("Error in sending console logs to channel!\n" +e.toString());
                }
            }
        }, 0, (main.getConfig().getInt("ChannelRefreshRate") >= 1 ? main.getConfig().getInt("ChannelRefreshRate") : 2) * 1000);
    }
    @Override
    public void append(LogEvent ev) {
        Method m = null;
        try {
            m = LogEvent.class.getMethod("toImmutable");

        } catch (NoSuchMethodException e) {}
        if(m != null){
            try {
                ev = (LogEvent) m.invoke(ev);
            } catch (IllegalAccessException | InvocationTargetException e) {}
        }
        Long timeMillis = null;
        m = null;
        try {
            m = LogEvent.class.getMethod("getTimeMillis");
        } catch (NoSuchMethodException e) {
            try {
                timeMillis = (Long) LogEvent.class.getMethod("getMillis").invoke(ev);
            } catch (Exception ee){}
        }
        if(m != null){
            try {
                timeMillis = (Long) m.invoke(ev);
            } catch (IllegalAccessException | InvocationTargetException e) {}
        }
        final LogEvent event = ev;
        String message = event.getMessage().getFormattedMessage();
        Boolean sendStartupMessages = !main.getConfig().isSet("sendStartupMessages") || main.getConfig().getBoolean("sendStartupMessages");
        if(message.toLowerCase().contains("reload") && message.toLowerCase().contains("complete")) startupDone = true;
        if(startupDone  || sendStartupMessages) {
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            message = String.format("[%s] [%s/%s] %s", formatter.format(new Date(timeMillis)), event.getThreadName(), event.getLevel().toString(), message);
            message = message.replaceAll("\\[m|\\[([0-9]{1,2}[;m]?){3}|\u001b+", "").replaceAll("\\x1b\\[[0-9;]*[A-Za-z]\\]*", "").replace("_", "\\_").replace("*", "\\*").replace("~", "\\~").replaceAll("(&|§)[0-9a-fklmnor]", "");
            ;
            msgs.add(message);
        }
    }
}