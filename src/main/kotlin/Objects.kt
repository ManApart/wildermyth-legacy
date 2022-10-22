import kotlinx.serialization.Serializable
import pages.toAspect

enum class CharacterClass { WARRIOR, HUNTER, MYSTIC }
enum class ClassLevel { GREENHORN, BLOODHORN, BLUEHORN, BRONZEHORN, SILVERHORN, GOLDHORN, BLACKHORN }

fun classLevelFromInt(level: Int) = ClassLevel.values()[level]

enum class Personality { BOOKISH, COWARD, GOOFBALL, HEALER, HOTHEAD, LEADER, LONER, GREEDY, POET, ROMANTIC, SNARK }

val personalityNames = Personality.values().map { it.name.lowercase() }

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
            getStoryProp(id)?.let { character.interpolate(it, this) } ?: id
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
    val relationships: List<HistoryRelationshipRaw> = listOf()
) {
    fun toHistoryEntry(): HistoryEntry {
        return HistoryEntry(id, acquisitionTime, textOverride, associatedAspects.map { it.toAspect() }, forbiddenAspects.map { it.toAspect() }, showInSummary, relationships.map { it.parse() })
    }
}

@Serializable
data class HistoryRelationship(
    val name: String? = null,
    val uuid: String? = null,
    val role: String? = null,
    val gender: String? = null,
)


@Serializable
data class HistoryRelationshipRawInner(val value: String? = null)

@Serializable
data class HistoryRelationshipRaw(
    val name: String? = null,
    val other: HistoryRelationshipRawInner? = null,
    val role: String? = null,
    val gender: String? = null,
) {
    fun parse() = HistoryRelationship(name, other?.value, role, gender)
}


@Serializable
data class AdditionalInfo(val uuid: String, var favorite: Boolean = false, val history: MutableList<HistoryEntry> = mutableListOf())

data class Family(val soulMate: String? = null, val parents: List<String> = listOf(), val children: List<String> = listOf()) {
    val all = listOfNotNull(soulMate) + parents + children
}

@Serializable
data class Company(
    val id: String,
    val gameId: String,
    val date: Double,
    val name: String,
    val mainThreat: String = "Unknown",
    val characters: MutableSet<String> = mutableSetOf()
)

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

@Serializable
data class CharacterSearchOptions(
    var searchText: String = "",
    var favoritesOnly: Boolean = false,
    var hideNPC: Boolean = false,
    var listView: Boolean = false,
)

@Serializable
data class Gear(
    val uuid: String,
    val name: String,
    val itemId: String,
    val category: String,
    val tier: Int,
    val subCategory: String? = null,
    val artifact: Boolean = false,
    val isEquipped: Boolean = false,
    val slots: List<String> = listOf(),
    val ownerAspects: List<Aspect> = listOf()
)

@Serializable
data class GearRaw(
    val itemId: String,
    val category: String,
    val tier: Int,
    val subCategory: String? = null,
    val uniqueCategory: String? = null,
    val artifact: Boolean = false,
    val isEquipped: Boolean = false,
    val slots: List<String> = listOf(),
    val ownerAspects: List<String> = listOf()
) {
    fun toGear(uuid: String, name: String) = Gear(uuid, name, itemId, category, tier, uniqueCategory ?: subCategory, artifact, isEquipped, slots, ownerAspects.map { it.toAspect() })
}