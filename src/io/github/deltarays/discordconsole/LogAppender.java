package io.github.deltarays.discordconsole;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

public class LogAppender extends AbstractAppender {
    DiscordConsole main;
    Boolean startupDone = false;
    Queue<String> msgs = new LinkedList<>();

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
                            main.sendDiscordMessage(message.toString());
                            message = new StringBuilder();
                        }
                        message.append(ln).append("\n");
                    }

                    if (StringUtils.isNotBlank(message.toString().replace("\n", ""))) {
                        main.sendDiscordMessage(message.toString());
                    }
                    main.updateBotStatus();
                } catch (Exception e) {
                    main.getLogger().severe("Error in sending console logs to channel!");
                    e.printStackTrace();
                }
            }
        }, 0, (Math.max(main.getConfig().getInt("channelRefreshRate"), 1)) * 1100);
    }

    @Override
    public void append(LogEvent ev) {
        Method method = null;
        try {
            method = LogEvent.class.getMethod("toImmutable");

        } catch (NoSuchMethodException ignored) {
        }
        if (method != null) {
            try {
                ev = (LogEvent) method.invoke(ev);
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }
        Long timeMillis = null;
        method = null;
        try {
            method = LogEvent.class.getMethod("getTimeMillis");
        } catch (NoSuchMethodException e) {
            try {
                timeMillis = (Long) LogEvent.class.getMethod("getMillis").invoke(ev);
            } catch (Exception ignored) {
            }
        }
        if (method != null) {
            try {
                timeMillis = (Long) method.invoke(ev);
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }
        if (timeMillis == null) timeMillis = new Date().getTime();
        LogEvent event = ev;
        String message = event.getMessage().getFormattedMessage();
        boolean sendStartupMessages = !main.getConfig().isSet("sendStartupMessages") || main.getConfig().getBoolean("sendStartupMessages");
        if (message.toLowerCase().contains("reload") && message.toLowerCase().contains("complete")) startupDone = true;
        if (startupDone || sendStartupMessages) {
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            message = String.format("[%s] [%s/%s] %s", formatter.format(new Date(timeMillis)), event.getThreadName(), event.getLevel().toString(), message);
            message = message.replaceAll("\\[m|\\[([0-9]{1,2}[;m]?){3}|\u001b+", "").replaceAll("\\x1b\\[[0-9;]*[A-Za-z]]*", "").replace("_", "\\_").replace("*", "\\*").replace("~", "\\~").replaceAll("([&§])[0-9a-fklmnor]", "");
            msgs.add(message);
        }
    }
}