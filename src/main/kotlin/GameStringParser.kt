import kotlinx.serialization.encodeToString
import kotlin.math.min

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

private val noHistory = HistoryEntry("None", 0L, "")

private val useLastResult = listOf("test", "/")
private val useTypeItself = listOf("mysticDeck_indignance_upgrade")

fun Character.interpolate(line: String, entry: HistoryEntry = noHistory): String {
    return line.cleanInitial().buildChunks().joinToString("") { it.interpolate(this, entry) }
}

private fun String.cleanInitial(): String {
    return removeAll("[b]", "[i]", "[]", "[upgrade]")
}

private fun String.buildChunks(from: Int = 0): List<Chunk> {
    val templateText = getTemplate(from, this) ?: return listOf(StringChunk(this.substring(from, length)))

    val children = templateText.buildChunks()

    val templateStart = indexOf("<$templateText", from)
    return listOfNotNull(
        if (templateStart > from) StringChunk(substring(from, templateStart)) else null,
        Template(templateText, children),
    ) + this.buildChunks(templateStart + templateText.length + 2)
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
    val resultOptions = if (typeOptions.size <= resultOptionsInitial.size) resultOptionsInitial else resultOptionsInitial.flatMap { it.split(",") }
    return when {
        templateClean in listOf("name", "fullname", "self") -> name
        templateClean == "firstname" -> name.split(" ").first()
        templateClean == "lastname" -> name.split(" ").last()
        templateClean == "town" -> hometown
        templateClean == "hometown" -> hometown
        templateClean == "company" -> getCompany(uuid).name
        type == "awm" -> replaceAWM(resultOptions)
        type == "mf" -> replaceMF(resultOptions)
        type == "int" -> parts.last()
        type.contains(".") -> replaceRelationshipTemplate(type, resultOptions, entry)
        entry.relationships.any { it.role == templateClean } -> entry.roleMatch(templateClean)
        entry.relationships.any { it.role == template } -> entry.roleMatch(template)
        type.startsWith("npc") -> entry.roleMatch(type)
        type.startsWith("hook") -> entry.roleMatch(type)
        type == "personality" -> replacePersonality(typeOptions, resultOptions)
        type == "personality2" -> replacePersonality(typeOptions, resultOptions, 1)
        typeOptions.any { it in personalityNames } -> replacePersonality(typeOptions, resultOptions)
        type == "cvawn_waterlingParent/cvawn_fallbackParent" -> cvawnParent(resultOptions)
        type in useLastResult -> resultOptions.last()
        else -> {
            println("$name encountered unknown type: $type. Using ${resultOptions.last()}")
            println(resultOptions)
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
            relationship != null && type == "fullname" -> (relationship.name ?: "someone")
            roleName == "self" -> replaceSelf(type, resultOptions)
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

private fun Character.replacePersonality(typeOptions: List<String>, resultOptions: List<String>, level: Int = 0): String {
    val highest = typeOptions.sortedByDescending { personality[Personality.valueOf(it.uppercase())] ?: 0 }.getOrNull(level) ?: typeOptions.firstOrNull()
    if (highest == null) {
        println("Null Personality for $name")
        return "personality"
    }
    val resultIndex = min(resultOptions.size - 1, typeOptions.indexOf(highest))
    return resultOptions[resultIndex]
}

private fun HistoryEntry.roleMatch(role: String): String {
    return relationships.firstOrNull { it.role == role }?.name ?: role
}

private fun Character.cvawnParent(resultOptions: List<String>): String {
    return if (family.parents.firstOrNull() != null) {
        resultOptions.last()
    } else resultOptions[1]
}

private fun Character.replaceSelf(type: String, resultOptions: List<String>): String {
    val aspectReference = type.contains("upgrade") || type.contains("themeAbility")
    return when {
        type == "mf" -> replaceMF(resultOptions)
        aspectReference && resultOptions.size == 2 -> replaceSelfAspect(type, resultOptions)
        aspectReference && resultOptions.size == 1 -> replaceSelfAspect(type, listOf(resultOptions.first(), ""))
        aspectReference && type.contains("/") -> replaceAspectList(type, resultOptions)
        type in useTypeItself -> type
        else -> type.also {
            println("Self type not found for $name $type: $resultOptions")
        }
    }
}

private fun Character.replaceSelfAspect(type: String, resultOptions: List<String>): String {
    return if (aspects.any { it.name == type }) resultOptions.first() else resultOptions.last()
}

private fun Character.replaceAspectList(type: String, resultOptions: List<String>): String {
    val typeOptions = type.split("/")
    val selected = typeOptions.firstOrNull { option -> aspects.any { it.name == option } }
    return selected?.let { resultOptions[typeOptions.indexOf(it)] } ?: resultOptions.last()
}