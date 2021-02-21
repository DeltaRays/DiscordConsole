package me.deltarays.discordconsole

import org.bukkit.configuration.file.YamlConfiguration


/**
 * @author DeltaRays
 * A custom configuration implementation
 * Created to add comment support to snakeyaml
 */
class CustomConfig : YamlConfiguration() {
    var comments: Int = 0

    /**
     * Saves the yaml (with comments) to a string
     * @return The yaml  string
     */
    override fun saveToString(): String {
        val str = super.saveToString()
        val builder = StringBuilder()
        str.split("\n").forEach { line ->
            builder.append(line.replaceFirst(Regex("cmt_\\d+!\\s*:"), "#") + "\n")
        }
        return builder.toString()
    }

    /**
     * Set a path to something and add comments before said path
     * @param comments A list of comments
     * @param path the path
     * @param value The value at the path
     */
    fun set(path: String, value: Any?, comments: Array<String>) {
        comments.forEach { comment ->
            super.set(String.format("cmt_%s!", this.comments), comment)
            this.comments++
        }
        super.set(path, value)
    }

    /**
     * Set a path to something and add a comment before said path
     * @param comment A comment
     * @param path the path
     * @param value The value at the path
     */
    fun set(path: String, value: Any?, comment: String) {
        super.set(String.format("cmt_%s!", this.comments), comment)
        this.comments++
        super.set(path, value)
    }

    /**
     * @param contents The unparsed yaml
     * Parses the comments of the yaml (by converting them to yaml objects)
     */
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

    /**
     * Loads the file from the unparsed yaml
     */
    override fun loadFromString(contents: String) {
        super.loadFromString(parseComments(contents))
    }

}