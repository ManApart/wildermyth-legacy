import kotlinx.serialization.Serializable
import pages.toAspect

enum class CharacterClass { WARRIOR, HUNTER, MYSTIC }
enum class ClassLevel { GREENHORN, BLOODHORN, BLUEHORN, BRONZEHORN, SILVERHORN, GOLDHORN, BLACKHORN }

fun classLevelFromInt(level: Int) = ClassLevel.values()[level]

enum class Personality { BOOKISH, COWARD, GOOFBALL, GREEDY, HEALER, HOTHEAD, LEADER, LONER, POET, ROMANTIC, SNARK }

@Serializable
data class Aspect(val name: String, val values: List<String> = listOf())

@Serializable
data class HistoryEntry(
    val id: String,
    val acquisitionTime: Long,
    var textOverride: String,
    val associatedAspects: List<Aspect> = listOf(),
    val forbiddenAspects: List<Aspect> = listOf(),
    val showInSummary: Boolean = false
)

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


@Serializable
data class AdditionalInfo(val uuid: String, val favorite: Boolean = false, val history: MutableList<HistoryEntry> = mutableListOf())

data class Family(val soulMate: String? = null, val parents: List<String> = listOf(), val children: List<String> = listOf())

@Serializable
data class LegacyCharacter(val uuid: String, val snapshots: Array<Character>, val companyIds: List<String> = listOf())

@Serializable
data class Company(val id: String, val date: Double, val name: String, val characters: MutableSet<String> = mutableSetOf())