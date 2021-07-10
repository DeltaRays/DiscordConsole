package me.deltarays.discordconsole.logging

import me.deltarays.discordconsole.DiscordConsole
import me.deltarays.discordconsole.Utils
import me.deltarays.discordconsole.discord.DiscordChannel
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern

class LogAppender(private var plugin: DiscordConsole) :
    AbstractAppender("DiscordConsoleAppender", null, null, false) {
    init {
        isAttached = true
        this.start()
    }

    override fun append(event: LogEvent?) {
        val logEvt: LogEvent?
        val method = try {
            LogEvent::class.java.getMethod("toImmutable")
        } catch (e: NoSuchMethodException) {
            null
        }
        logEvt = if (method != null) {
            try {
                method.invoke(event) as LogEvent?
            } catch (exc: Exception) {
                event
            }
        } else {
            event
        }
        val timeMillis: Long = try {
            LogEvent::class.java.getMethod("getTimeMillis").invoke(logEvt)
        } catch (e: Exception) {
            try {
                LogEvent::class.java.getMethod("getMillis").invoke(logEvt)
            } catch (exc :Exception){
                Date().time
            }
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

    companion object {
        var isAttached = false
    }

    private fun parse(str: String, levelName: String, message: String, threadName: String, time: Long): String {
        return Utils.convertPlaceholders(str).replace(Regex("\\{message}", RegexOption.IGNORE_CASE), message)
            .replace(Regex("\\{level}", RegexOption.IGNORE_CASE), levelName)
            .replace(Regex("\\{thread}", RegexOption.IGNORE_CASE), threadName)
            .replace(Regex("\\{date\\[(.*?)]}", RegexOption.IGNORE_CASE)) { e ->
                val dateFormat = DateTimeFormatter.ofPattern(e.groupValues.getOrElse(1) { "hh:mm:ss" })
                dateFormat.format(Instant.ofEpochMilli(time).atOffset(ZoneOffset.UTC))
            }
            // Replaces special characters
            .replace("\\[m|\\[([0-9]{1,2}[;m]?){3}|\u001b+".toRegex(), "")
            .replace("\\x1b\\[[0-9;]*[A-Za-z]]*".toRegex(), "")
            .replace("([&§])[0-9a-fklmnor]".toRegex(), "")
            .replace("_", "\\_").replace("*", "\\*")
            .replace("~", "\\~")
            .replace("|", "\\|")
            .replace(">", "\\>")
            .replace("`", "\\`")
    }
}
