package me.deltarays.discordconsole.logging

/**
 * The possible DiscordConsole log types.
 * @author DeltaRays
 */
enum class LogType(format: String) {
    CHAT("{player}: {message}"),
    JOINS("{player} joined the server"),
    QUITS("{player} left the server"),
    CONSOLE("[{date[HH:mm:ss]}] [{thread}/{level}] {message}");

    val defaultFormat: String = format;

}


/*
object LogType {
    object CHAT {
        fun format(sender: Player, message: String): String {
            return "**${sender.name}:** $message"
        }
    }
    object JOINS {
        fun format(player: Player): String {
            return "$player has joined the server!"
        }
    }
    object QUITS {
        fun format(player: Player): String {
            return "$player has left the server!"
        }
    }
    object CONSOLE {
        fun format(level: Level, message: String, threadName: String, time: Long): String {
            val formatter = SimpleDateFormat("HH:mm:ss")
            return "[${formatter.format(Date(time))}] [$threadName/${level.name}] $message"
        }
    }


}


*/