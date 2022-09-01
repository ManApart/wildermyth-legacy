package wildermyth

import Character
import personalityNames

interface Chunk {
    fun interpolate(character: Character): String
}

data class StringChunk(private val input: String) : Chunk {
    override fun interpolate(character: Character) = input
}

data class Template(private val input: String, private val children: List<Chunk>) : Chunk {
    override fun interpolate(character: Character): String {
        val rebuilt = if (children.isEmpty()) input else children.joinToString("") { it.interpolate(character) }
        return character.replaceTemplate(rebuilt)
    }
}

fun Character.interpolate(line: String): String {
    return buildChunks(line).joinToString("") { it.interpolate(this) }
}

private fun buildChunks(line: String): List<Chunk> {
    return buildChunks(0, line)
}

private fun buildChunks(from: Int, line: String): List<Chunk> {
    val templateText = getTemplate(from, line) ?: return listOf(StringChunk(line.substring(from, line.length)))

    val children = buildChunks(0, templateText)

    val templateStart = line.indexOf("<$templateText", from)
    return listOfNotNull(
        if (templateStart > from) StringChunk(line.substring(from, templateStart)) else null,
        Template(templateText, children),
    ) + buildChunks(templateStart + templateText.length + 2, line)
}

fun getTemplate(from: Int, line: String): String? {
    val start = line.indexOf("<", from)
    if (start == -1) return null
    var i = start
    var depth = 1
    while (depth > 0 && i < line.length - 1) {
        i++
        val char = line[i]
        if (char == '<') depth++
        if (char == '>') depth--
    }
    return if (depth == 0) line.substring(start + 1, i) else null
}

private fun Character.replaceTemplate(template: String): String {
    val parts = template.split(":")
    val type = parts.first()
    val typeOptions = type.split("/")
    val resultOptions = parts.last().split("/")
    return when {
        template == "name" -> name
        type == "mf" -> replaceMF(resultOptions)
        typeOptions.any { it in personalityNames } -> replacePersonality(typeOptions, resultOptions)
        else -> {
            println("Unknown type: $type")
            resultOptions.last()
        }
    }
}

private fun Character.replaceMF(resultOptions: List<String>): String {
    return when {
        sex == Sex.MALE -> resultOptions.first()
        sex == Sex.FEMALE -> resultOptions[1]
        resultOptions.size == 3 -> resultOptions.last()
        else -> resultOptions.first()
    }
}

private fun Character.replacePersonality(typeOptions: List<String>, resultOptions: List<String>): String {
    //For some reason personality may be undefined here
    if (personality == undefined) {
        personality = getPersonality()
    }
    val highest = typeOptions.maxByOrNull { personality[Personality.valueOf(it.uppercase())] ?: 0 }
        ?: typeOptions.first()
    val resultIndex = typeOptions.indexOf(highest)
    return resultOptions[resultIndex]
}