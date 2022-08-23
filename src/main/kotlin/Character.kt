import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.js.Json

@Serializable
data class Character(
    val name: String,
    val characterClass: CharacterClass,
    val age: Int,
    val aspects: List<Aspect> = listOf(),
    val temporal: Map<String, Int> = mapOf()
) {
    @Transient
    val fileName = name.replace(Regex.fromLiteral("[^a-zA-Z\\d\\s:]"), "").trim()
}

enum class CharacterClass { WARRIOR, HUNTER, MYSTIC }

@Serializable
data class Aspect(val name: String, val values: List<String> = listOf())

fun parseFromJson(json: Json): Character {
    val base = (json["entities"] as Array<Array<Json>>)[0][2]
    val name = base["name"] as String
    val aspects = parseAspects(base)
    val temporal = parseTemporal(base)

    return Character(name, determineClass(aspects), determineAge(temporal), aspects, temporal)
}

private fun parseAspects(base: Json): List<Aspect> {
    val aspectJson = (base["aspects"] as Json)["entries"] as Array<Array<Any>>
    val stringAspects = aspectJson.flatten().filterIsInstance<String>()
    return stringAspects.map { it.toAspect() }
}

private fun parseTemporal(base: Json): Map<String, Int> {
    val temporalJson = (base["temporal"] as Json)["entries"] as Array<Array<Any>>
    return temporalJson.associate { values -> values.first() as String to values.last() as Int }
}

fun String.toAspect(): Aspect {
    return if (contains("|")) {
        val parts = this.split("|")
        Aspect(parts.first(), parts.subList(1, parts.size))
    } else Aspect(this)
}

private fun determineClass(aspects: List<Aspect>): CharacterClass {
    val className = aspects.firstOrNull { it.name == "classLevel" }?.values?.firstOrNull()?.uppercase() ?: "WARRIOR"
    return CharacterClass.valueOf(className)
}

private fun determineAge(temporal: Map<String, Int>): Int {
    return temporal["AGE"] ?: 20
}