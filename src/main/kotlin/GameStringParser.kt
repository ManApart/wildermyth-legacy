import kotlinx.serialization.encodeToString

interface Chunk {
    fun interpolate(character: Character, entry: HistoryEntry): String
}

data class StringChunk(private val input: String) : Chunk {
    override fun interpolate(character: Character, entry: HistoryEntry) = input
}

data class Template(private val input: String, private val children: List<Chunk>) : Chunk {
    override fun interpolate(character: Character, entry: HistoryEntry): String {
        val rebuilt = if (children.isEmpty()) input else children.joinToString("") { it.interpolate(character, entry) }
        return character.replaceTemplate(rebuilt, entry)
    }
}

fun Character.interpolate(line: String, entry: HistoryEntry): String {
    return buildChunks(line).joinToString("") { it.interpolate(this, entry) }
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

private fun Character.replaceTemplate(template: String, entry: HistoryEntry): String {
    val templateClean = template.lowercase()
    val parts = template.split(":")
    val type = parts.first()
    val typeOptions = type.split("/")
    val resultOptionsInitial = parts.last().split("/")
    val resultOptions = if (typeOptions.size == resultOptionsInitial.size) resultOptionsInitial else resultOptionsInitial.flatMap { it.split(",") }
    return when {
        templateClean == "name" -> name
        templateClean == "fullname" -> name
        templateClean == "firstname" -> name.split(" ").first()
        templateClean == "site" -> entry.roleMatch("site")
        templateClean == "overlandtile" -> entry.roleMatch("overlandtile")
        templateClean == "site" -> entry.roleMatch("site")
        templateClean == "hero" -> entry.roleMatch("hero")
        templateClean == "company" -> entry.roleMatch("company")
        templateClean == "town" -> hometown
        templateClean == "hometown" -> hometown
        type == "awm" -> replaceAWM(resultOptions)
        type == "mf" -> replaceMF(resultOptions)
        type.contains(".") -> replaceRelationshipTemplate(type, resultOptions, entry)
        type.startsWith("npc") -> entry.roleMatch(type)
        type.startsWith("hook") -> entry.roleMatch(type)
        type == "personality" -> replacePersonality(typeOptions, resultOptions)
        typeOptions.any { it in personalityNames } -> replacePersonality(typeOptions, resultOptions)
        else -> {
            println("$name encountered unknown type: $type. Using ${resultOptions.last()}")
            println(jsonMapper.encodeToString(entry))
            resultOptions.last()
        }
    }
}

fun Character.replaceRelationshipTemplate(fullType: String, resultOptions: List<String>, entry: HistoryEntry): String {
    val parts = fullType.split(".")
    val roleName = parts.first()
    val type = parts.last()
    val relationship = entry.relationships.firstOrNull { it.role == roleName }
    return relationship
        ?.uuid
        ?.let { getCharacter(it) }
        ?.let { relative ->
            val secondHalf = if (resultOptions.joinToString("/").contains(type)) "" else ":" + resultOptions.joinToString("/")
            val template = "$type$secondHalf"
            relative.snapshots.last().replaceTemplate(template, entry)
        } ?: let {
        when {
            relationship != null && type == "mf" -> relationship.replaceMF(resultOptions)
            relationship != null && type == "fullname" -> (relationship.name?: "someone")
            else -> {
                println("Relationship Attributes not supported: $name ${entry.id} $fullType")
                println(jsonMapper.encodeToString(entry))
                resultOptions.last()
            }
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

private fun HistoryRelationship.replaceMF(resultOptions: List<String>): String {
    return when {
        gender == "male" -> resultOptions.first()
        gender == "female" -> resultOptions[1]
        resultOptions.size == 3 -> resultOptions.last()
        else -> resultOptions.first()
    }
}

private fun Character.replaceAWM(resultOptions: List<String>): String {
    return if (attractedToWomen) resultOptions.first() else resultOptions.last()
}

private fun Character.replacePersonality(typeOptions: List<String>, resultOptions: List<String>): String {
    val highest = typeOptions.maxByOrNull { personality[Personality.valueOf(it.uppercase())] ?: 0 }
        ?: typeOptions.firstOrNull()
    if (highest == null){
        println("Null Personality for $name")
        return "personality"
    }
    val resultIndex = typeOptions.indexOf(highest)
    return resultOptions[resultIndex]
}

private fun HistoryEntry.roleMatch(role: String): String {
    return relationships.firstOrNull { it.role == role }?.name ?: role
}