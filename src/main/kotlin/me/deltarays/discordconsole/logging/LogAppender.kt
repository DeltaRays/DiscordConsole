package me.deltarays.discordconsole.logging

import me.deltarays.discordconsole.DiscordConsole
import me.deltarays.discordconsole.Utils
import me.deltarays.discordconsole.discord.DiscordChannel
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class LogAppender(private var plugin: DiscordConsole) :
    AbstractAppender("DiscordConsoleAppender", null, null, false, Property.EMPTY_ARRAY) {
    override fun append(event: LogEvent?) {
        val logEvt: LogEvent?;
        val method = try {
            LogEvent::class.java.getMethod("toImmutable")
        } catch (e: NoSuchMethodException) {
            null
        }
        logEvt = try {
            method?.invoke(event) as LogEvent?
        } catch (e: Exception) {
            event

        }
        val timeMillis: Long = try {
            LogEvent::class.java.getMethod("getTimeMillis").invoke(logEvt)
        } catch (e: Exception) {
            LogEvent::class.java.getMethod("getMillis").invoke(logEvt)
        } as Long
        val message = logEvt?.message?.formattedMessage as String
        for (channel in DiscordChannel.channels) {
            if (channel.types.contains(LogType.CONSOLE)) {
                val channelSection = plugin.getConfigManager().getChannel(channel.id)
                val consoleSection =
                    channelSection.getConfigurationSection("console") ?: channelSection.createSection("console")
                if (consoleSection.getBoolean("send-startup", false) || DiscordConsole.serverHasStartedUp) {
                    val fmt: String = channel.getMessageFormat(LogType.CONSOLE)
                    val formatted =
                        parse(fmt, logEvt.level.name(), message, logEvt.threadName, timeMillis)
                    val filterStr = consoleSection.getString("filter") ?: ""
                    if (filterStr.isNotEmpty()) {
                        val filter = Pattern.compile(filterStr)
                        if (!filter.matcher(formatted).find()) continue
                    }
                    channel.enqueueMessage(formatted)
                }
            }


        }
    }

    private fun parse(str: String, levelName: String, message: String, threadName: String, time: Long): String {
        return Utils.convertPlaceholders(str).replace(Regex("\\{message}", RegexOption.IGNORE_CASE), message)
            .replace(Regex("%log_level%", RegexOption.IGNORE_CASE), levelName)
            .replace(Regex("%log_thread%", RegexOption.IGNORE_CASE), threadName)
            .replace(Regex("%date\\[(.*?)]%", RegexOption.IGNORE_CASE)) { e ->
                val dateFormat = SimpleDateFormat(e.groupValues.getOrElse(0) { "HH:mm:ss" });
                dateFormat.format(Date(time))
            }
    }
}
