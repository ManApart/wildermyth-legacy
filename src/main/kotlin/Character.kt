import kotlinx.serialization.Serializable

/*
Stats to display
Is bio possible? Seems like I'd need to do interpolation which I'd like to avoid. Maybe use Text override
Personality stats
Class Level
Class Stats
Relationships
Hometown
 */

@Serializable
data class Character(
    val uuid: String,
    val name: String,
    val characterClass: CharacterClass,
    val age: Int,
    val aspects: List<Aspect> = listOf(),
    val temporal: Map<String, Int> = mapOf(),
    val history: List<HistoryEntry> = listOf(),
) {
}

enum class CharacterClass { WARRIOR, HUNTER, MYSTIC }
enum class Personality { BOOKISH, COWARD, GOOFBALL, GREEDY, HEALER, HOTHEAD, LEADER, LONER, POET, ROMANTIC, SNARK }

@Serializable
data class Aspect(val name: String, val values: List<String> = listOf())

@Serializable
data class HistoryEntry(val id: String, val acquisitionTime: Long, val associatedAspects: List<Aspect>, val forbiddenAspects: List<Aspect>, val showInSummary: Boolean)

@Serializable
data class HistoryEntryRaw(
    val id: String = "",
    val acquisitionTime: Long = 0,
    val associatedAspects: List<String> = listOf(),
    val forbiddenAspects: List<String> = listOf(),
    val showInSummary: Boolean = false
) {
    fun toHistoryEntry(): HistoryEntry {
        return HistoryEntry(id, acquisitionTime, associatedAspects.map { it.toAspect() }, forbiddenAspects.map { it.toAspect() }, showInSummary)
    }
}
