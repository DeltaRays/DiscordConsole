package me.deltarays.discordconsole.logging

import me.deltarays.discordconsole.DiscordConsole
import me.deltarays.discordconsole.discord.DiscordChannel
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import java.text.SimpleDateFormat
import java.util.*

class LogAppender(private var plugin: DiscordConsole) :
    AbstractAppender("DiscordConsoleAppender", null, null, false, Property.EMPTY_ARRAY) {
    var sendStartupMessages = plugin.config.getBoolean("send-startup-messages", false)
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
        if (sendStartupMessages || DiscordConsole.serverHasStartedUp) {
            for (channel in DiscordChannel.channels) {
                if (channel.types.contains("CONSOLE")) {
                    val fmt: String = channel.getMessageFormat(LogType.CONSOLE)
                    val formatted =
                        parse(fmt, logEvt.level.name(), message, logEvt.threadName, timeMillis)
                    channel.enqueueMessage(formatted)
                }
            }


        }
    }

    private fun parse(str: String, levelName: String, message: String, threadName: String, time: Long): String {
        str.apply {
            replace(Regex("\\{message}", RegexOption.IGNORE_CASE), message)
            replace(Regex("\\{level}", RegexOption.IGNORE_CASE), levelName)
            replace(Regex("\\{thread}", RegexOption.IGNORE_CASE), threadName)
            replace(Regex("\\{date\\[(.*?)]}", RegexOption.IGNORE_CASE)) { e ->
                val dateFormat = SimpleDateFormat(e.groupValues.getOrElse(0) { "HH:mm:ss" });
                dateFormat.format(Date(time))
            }
        }
        return str;
    }
}