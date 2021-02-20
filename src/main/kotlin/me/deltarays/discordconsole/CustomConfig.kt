package me.deltarays.discordconsole

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.file.YamlConstructor
import org.yaml.snakeyaml.Yaml


class CustomConfig : YamlConfiguration() {
    var comments: Int = 0
    private val yaml = Yaml(YamlConstructor())
    override fun saveToString(): String {
        var str = super.saveToString()
        var builder = StringBuilder()
        str.split("\n").forEach { line ->
            builder.append(line.replaceFirst(Regex("cmt_\\d+!\\s*:"), "#") + "\n")
        }
        return builder.toString()
    }

    fun set(path: String, value: Any?, comments: Array<String>) {
        comments.forEach { comment ->
            super.set(String.format("cmt_%s!", this.comments), comment)
            this.comments++
        }
        super.set(path, value)
    }

    fun set(path: String, value: Any?, comment: String) {
        super.set(String.format("cmt_%s!", this.comments), comment)
        this.comments++
        super.set(path, value)
    }

    private fun parseComments(contents: String): String {
        val builder = StringBuilder()
        for (line in contents.split("\n")) {
            if (line.trimStart().startsWith("#")) {
                builder.append(
                    line.replaceFirst(
                        Regex("(\\s*)#"),
                        String.format("$1cmt_%s!:", comments)
                    )
                )
                comments++
            } else builder.append(line)
            builder.append("\n")
        }
        println(builder.toString())
        comments = 0
        return builder.toString()
    }

    override fun loadFromString(contents: String) {
        super.loadFromString(parseComments(contents))
    }

}