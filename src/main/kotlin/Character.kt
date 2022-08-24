import kotlinx.serialization.Serializable

/*
Stats to display
Is bio possible? Seems like I'd need to do interpolation which I'd like to avoid. Maybe use Text override
Personality stats
Class Stats
Relationships
Hometown
 */

@Serializable
data class Character(
    val uuid: String,
    val name: String,
    val aspects: List<Aspect> = listOf(),
    val temporal: Map<String, Int> = mapOf(),
    val history: List<HistoryEntry> = listOf(),
) {

    fun getBio(): String {
        val historyOverrides = history.joinToString(" ") { it.textOverride }
        return historyOverrides.ifBlank {
            listOfNotNull(
                history.firstOrNull { it.id.startsWith("origin") }?.id,
                history.firstOrNull { it.id.startsWith("dote") }?.id,
                history.firstOrNull { it.id.startsWith("mote") }?.id
            ).joinToString(", ")
        }
    }

    fun getCharacterClass(): CharacterClass {
        val className = aspects.firstOrNull { it.name == "classLevel" }?.values?.firstOrNull()?.uppercase() ?: "WARRIOR"
        return CharacterClass.valueOf(className)
    }

    fun getClassLevel(): ClassLevel {
        val level = aspects.firstOrNull { it.name == "classLevel" }?.values?.get(1)?.toIntOrNull() ?: 0
        return classLevelFromInt(level)
    }

    fun getAge(): Int {
        return temporal["AGE"] ?: 20
    }

    fun getPersonality(): Map<Personality, Int>{
        val aspect = history.firstOrNull { it.id == "humanPersonalityStats" }?.associatedAspects?.firstOrNull { it.name == "roleStats" }
        return if (aspect != null) {
            val personality = mutableMapOf<Personality, Int>()
            Personality.values().forEachIndexed { i, p ->
                personality[p] = aspect.values[i].toDouble().toInt()
            }
            personality
        } else {
            Personality.values().associateWith { 0 }
        }
    }

}

enum class CharacterClass { WARRIOR, HUNTER, MYSTIC }
enum class ClassLevel { GREENHORN, BLOODHORN, BLUEHORN, BRONZEHORN, SILVERHORN, GOLDHORN, BLACKHORN }

fun classLevelFromInt(level: Int) = ClassLevel.values()[level]

enum class Personality { BOOKISH, COWARD, GOOFBALL, GREEDY, HEALER, HOTHEAD, LEADER, LONER, POET, ROMANTIC, SNARK }

@Serializable
data class Aspect(val name: String, val values: List<String> = listOf())

@Serializable
data class HistoryEntry(val id: String, val acquisitionTime: Long, val textOverride: String, val associatedAspects: List<Aspect>, val forbiddenAspects: List<Aspect>, val showInSummary: Boolean)

@Serializable
data class HistoryEntryRaw(
    val id: String = "",
    val acquisitionTime: Long = 0,
    val textOverride: String = "",
    val associatedAspects: List<String> = listOf(),
    val forbiddenAspects: List<String> = listOf(),
    val showInSummary: Boolean = false
) {
    fun toHistoryEntry(): HistoryEntry {
        return HistoryEntry(id, acquisitionTime, textOverride, associatedAspects.map { it.toAspect() }, forbiddenAspects.map { it.toAspect() }, showInSummary)
    }
}
