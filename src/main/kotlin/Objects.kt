import kotlinx.serialization.Serializable
import pages.toAspect
import wildermyth.interpolate

enum class CharacterClass { WARRIOR, HUNTER, MYSTIC }
enum class ClassLevel { GREENHORN, BLOODHORN, BLUEHORN, BRONZEHORN, SILVERHORN, GOLDHORN, BLACKHORN }

fun classLevelFromInt(level: Int) = ClassLevel.values()[level]

enum class Personality { BOOKISH, COWARD, GOOFBALL, GREEDY, HEALER, HOTHEAD, LEADER, LONER, POET, ROMANTIC, SNARK }

val personalityNames =  Personality.values().map { it.name.lowercase() }

@Serializable
data class Aspect(val name: String, val values: List<String> = listOf())

@Serializable
data class HistoryEntry(
    val id: String,
    val acquisitionTime: Long,
    var textOverride: String,
    val associatedAspects: List<Aspect> = listOf(),
    val forbiddenAspects: List<Aspect> = listOf(),
    val showInSummary: Boolean = true,
    val relationships: List<HistoryRelationship> = listOf()
) {
    fun getText(character: Character): String {
        return textOverride.ifBlank {
            getStoryProp(id)?.let { character.interpolate(it) } ?: id
        }
    }
}

@Serializable
data class HistoryEntryRaw(
    val id: String = "",
    val acquisitionTime: Long = 0,
    val textOverride: String = "",
    val associatedAspects: List<String> = listOf(),
    val forbiddenAspects: List<String> = listOf(),
    val showInSummary: Boolean = true,
    val relationships: List<HistoryRelationship> = listOf()
) {
    fun toHistoryEntry(): HistoryEntry {
        return HistoryEntry(id, acquisitionTime, textOverride, associatedAspects.map { it.toAspect() }, forbiddenAspects.map { it.toAspect() }, showInSummary, relationships)
    }
}

@Serializable
data class HistoryRelationship(val name: String? = null)


@Serializable
data class AdditionalInfo(val uuid: String, var favorite: Boolean = false, val history: MutableList<HistoryEntry> = mutableListOf())

data class Family(val soulMate: String? = null, val parents: List<String> = listOf(), val children: List<String> = listOf())

@Serializable
data class Company(val id: String, val date: Double, val name: String, val characters: MutableSet<String> = mutableSetOf())

data class Friendship(val relativeId: String, val kind: FriendshipKind, val level: Int)

enum class FriendshipKind(val titles: List<String>) {
    FRIEND(listOf("Crony ", "Confidant", "Comrade", "Companion", "Bloodbond")),
    LOVER(listOf("Crush", "Flame", "Sweetheart", "Lover", "Soulmate")),
    RIVAL(listOf("Peer", "Irritant", "Frenemy", "Antagonist", "Rival"));

    fun getTitle(i: Int): String {
        return titles[i - 1]
    }
}

enum class Sex { MALE, FEMALE, UNKNOWN }

data class CharacterSearch(
    var searchText: String = "",
    var favoritesOnly: Boolean = false,
    var hideNPC: Boolean = false,
)