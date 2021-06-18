package me.deltarays.discordconsole.logging

/**
 * The possible DiscordConsole log types.
 * @author DeltaRays
 */
enum class LogType(format: String) {
    CHAT("{player}: {message}"),
    JOINS("{player} joined the server"),
    QUITS("{player} left the server"),
    CONSOLE("[{date[HH:mm:ss]}] [{thread}/{level}] {message}"),
    DEATH("{message}"),
    STARTUP(TODO()),
    SHUTDOWN(TODO());

    val defaultFormat: String = format;

}
