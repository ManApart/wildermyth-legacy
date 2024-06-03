import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import pages.toAspect

enum class CharacterClass { WARRIOR, HUNTER, MYSTIC }
enum class ClassLevel { GREENHORN, BLOODHORN, BLUEHORN, BRONZEHORN, SILVERHORN, GOLDHORN, BLACKHORN }

fun classLevelFromInt(level: Int) = ClassLevel.values()[level]

enum class LegacyTierLevel { FOLK_HERO, LOCAL_LEGEND, FABLED_ADVENTURER, BALLADSUNG_HERO, MYTHWALKER }

fun legacyTierLevelFromInt(level: Int) = LegacyTierLevel.values()[level]

enum class Personality { BOOKISH, COWARD, GOOFBALL, HEALER, HOTHEAD, LEADER, LONER, GREEDY, POET, ROMANTIC, SNARK }

val personalityNames = Personality.values().map { it.name.lowercase() }

enum class Stat {
    ARMOR,
    BLOCK,
    CHARISMA,
    DODGE,
    HEALTH,
    MELEE_ACCURACY,
    PHYSICAL_DAMAGE_BONUS,
    POTENCY,
    RANGE_ACCURACY,
    RECOVERY_RATE,
    RETIREMENT_AGE,
    SPEED,
    STUNT_CHANCE,
    TENACITY,
    WARDING,
}

fun String.toStat() = Stat.values().firstOrNull { it.name == this }

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
data class AdditionalInfo(val uuid: String, var favorite: Boolean = false, val tags: MutableSet<String> = mutableSetOf(), val history: MutableList<HistoryEntry> = mutableListOf())

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

enum class CharacterSort { ALPHABETICAL, RANK, ACQUIRED }

@Serializable
data class CharacterSearchOptions(
    var searchText: String = "",
    var favoritesOnly: Boolean = false,
    var favoritesFirst: Boolean = true,
    var hideNPC: Boolean = false,
    var listView: Boolean = true,
    var sort: CharacterSort = CharacterSort.ALPHABETICAL,
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
) {
    @Transient
    var description = parseDescription()

    private fun parseDescription(): String {
        val propId = if (itemId.endsWith("_t1")) itemId.substring(0, itemId.indexOf("_")) else itemId
        return (getDynamicProp("itemArtifactBlurb.${propId}")?.let { "$it. " } ?: "") +
                (getDynamicProp("itemSummary.${propId}") ?: "")
    }

    fun reload() {
        description = parseDescription()
    }
}

data class Ability(
    val id: String,
    val name: String,
    val description: String,
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

@Serializable
data class Hook(val id: String, val resolved: Boolean = false)

enum class Element { FIRE, LEAF, STONE, WATER }

@Serializable
data class Profile(
    val name: String,
    val unlocks: List<Unlock> = listOf()
) {
    val weaponUnlocks: Map<String, Map<Element, Boolean>>
        get() {
            if (weaponUnlocksBacking === undefined) {
                weaponUnlocksBacking = parseWeaponUnlocks()
            }
            return weaponUnlocksBacking
        }

    @Transient
    private var weaponUnlocksBacking = parseWeaponUnlocks()

    private fun parseWeaponUnlocks(): Map<String, Map<Element, Boolean>> {
        return unlocks.filter { it.id.startsWith("weaponUnlock") }.map {
            val parts = it.id.split("|")
            parts[1] to Element.valueOf(parts[2].uppercase())
        }.groupBy { it.first }
            .mapValues { (_, namePairs:  List<Pair<String, Element>>) ->  namePairs.map { it.second } }
            .mapValues { (_, elements:  List<Element>) ->
                Element.values().associateWith { element -> elements.contains(element) }
            }
    }
}

@Serializable
data class Unlock(val id: String, val name: String, val progress: Int)

data class GraphDataEntry(val rowName: String, val amount: Float, val picUrl: String? = null, val color: String? = null, val rowSearch: String? = null) {
    constructor(rowName: String, amount: Int) : this(rowName, amount.toFloat())
    constructor(rowName: String, amount: Int, rowSearch: String?) : this(rowName, amount.toFloat(), rowSearch = rowSearch)
}