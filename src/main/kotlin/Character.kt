import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString

@Serializable
data class LegacyCharacter(
    val uuid: String,
    val snapshots: Array<Character>,
    val companyIds: List<String> = listOf(),
    val npc: Boolean = false,
    val legacyTierLevel: LegacyTierLevel = LegacyTierLevel.FOLK_HERO,
    val killCount: Int = 0,
    @Transient
    val rawJson: String? = null,
) {
    @Transient
    val hooks = getHooks()

    private fun getHooks(): List<Hook> {
        val allHooks = snapshots.flatMap { it.hooks }.toSet()
        return allHooks
            .filter { hook -> hook.resolved || allHooks.none { it.resolved && it.id == hook.id } }
            .sortedBy { it.id }
    }

    @Transient
    val friendships = getFriendships()

    private fun getFriendships(): List<Friendship> {
        return snapshots.flatMap { it.friendships }.groupBy { it.relativeId }.map { (_, options) -> options.maxBy { it.level } }
    }

    fun findAllFriends(maxDepth: Int): Set<LegacyCharacter> {
        val checked = mutableSetOf<LegacyCharacter>()
        val newOptions = ArrayDeque<Pair<LegacyCharacter, Int>>()
        newOptions.add(Pair(this, -1))
        while (newOptions.size > 0) {
            val (option, depth) = newOptions.removeFirst()
            checked.add(option)
            if (depth < maxDepth) {
                val deeper = depth + 1
                val friends = option.friendships.mapNotNull { getCharacter(it.relativeId) }
                newOptions.addAll(friends.filterNot { checked.contains(it) }.map { Pair(it, deeper) })
            }
        }

        return checked
    }

    fun findAllRelatives(maxDepth: Int): Set<LegacyCharacter> {
        val checked = mutableSetOf<LegacyCharacter>()
        val newOptions = ArrayDeque<Pair<LegacyCharacter, Int>>()
        newOptions.add(Pair(this, -1))
        while (newOptions.size > 0) {
            val (option, depth) = newOptions.removeFirst()
            checked.add(option)
            if (depth < maxDepth) {
                val deeper = depth + 1
                val family = option.snapshots.last().family
                val relatives = (family.parents + family.children + listOfNotNull(family.soulMate)).mapNotNull { getCharacter(it) }
                newOptions.addAll(relatives.filterNot { checked.contains(it) }.map { Pair(it, deeper) })
            }
        }

        return checked
    }
}

@Serializable
data class Character(
    val uuid: String,
    val gameId: String,
    val date: Long = 0L,
    val name: String,
    val aspects: List<Aspect> = listOf(),
    val temporal: Map<String, Int> = mapOf(),
    val history: List<HistoryEntry> = listOf(),
    val gear: List<Gear> = listOf(),
) {

    fun reload() {
        //Since story props don't exist when parsing json from indexDB, reload them after the in-memory db is loaded
        bio = getBio()
        abilities = parseAbilities()
        gear.forEach { it.reload() }
    }

    @Transient
    var bio = getBio()

    val sex: Sex
        get() {
            if (sexBacking == undefined) {
                sexBacking = parseSex()
            }
            return sexBacking
        }

    @Transient
    private var sexBacking = parseSex()

    @Transient
    val attractedToWomen = getAttractedToWomen()

    @Transient
    val characterClass = getCharacterClass()

    val classLevel: ClassLevel
        get() {
            if (classLevelBacking == undefined) {
                classLevelBacking = parseClassLevel()
            }
            return classLevelBacking
        }

    @Transient
    private var classLevelBacking = parseClassLevel()

    @Transient
    val age = getAge()

    @Transient
    val primaryStats = parsePrimaryStats()

    val personality: Map<Personality, Int>
        get() {
            if (personalityBacking == undefined) {
                personalityBacking = parsePersonality()
            }
            return personalityBacking
        }

    @Transient
    private var personalityBacking = parsePersonality()

    val personalityFirst: Personality
        get() {
            if (personalityFirstBacking == undefined) {
                personalityFirstBacking = parsePersonalityFirst()
            }
            return personalityFirstBacking
        }

    @Transient
    private var personalityFirstBacking = parsePersonalityFirst()

    val personalitySecond: Personality
        get() {
            if (personalitySecondBacking == undefined) {
                personalitySecondBacking = parsePersonalitySecond()
            }
            return personalitySecondBacking
        }

    @Transient
    private var personalitySecondBacking = parsePersonalitySecond()

    val family: Family
        get() {
            if (familyBacking == undefined) {
                familyBacking = getFamily()
            }
            return familyBacking
        }

    @Transient
    private var familyBacking = getFamily()

    @Transient
    val friendships = getFriendships()

    val hometown: String
        get() {
            if (hometownBacking == undefined) {
                hometownBacking = parseHomeTown()
            }
            return hometownBacking
        }

    @Transient
    private var hometownBacking = parseHomeTown()

    @Transient
    var abilities = parseAbilities()

    @Transient
    val hooks = parseHooks()

    private fun getBio(): String {
        val historyOverrides = history.joinToString(" ") { it.textOverride }
        return historyOverrides.ifBlank {
            listOfNotNull(
                getBioString("origin"),
                getBioString("dote"),
                getBioString("mote"),
            ).joinToString(" ").ifBlank {
                getBioString("villain")
                    ?: getBioString("plot")
                    ?: getBioStringContains("custom")
                    ?: listOfNotNull(
                        getBioStringContains("origin"),
                        getBioStringContains("dote"),
                        getBioStringContains("mote"),
                    ).joinToString(" ")
            }
        }
    }

    private fun getBioString(startsWith: String): String? {
        val entry = history.firstOrNull { it.id.startsWith(startsWith) }
        return if (entry != null) {
            getStoryProp(entry.id)?.let { storyProp -> interpolate(storyProp, entry) }
        } else null
    }

    private fun getBioStringContains(contains: String): String? {
        val entry = history.firstOrNull { it.id.contains(contains, ignoreCase = true) }
        return if (entry != null) {
            getStoryProp(entry.id)?.let { storyProp -> interpolate(storyProp, entry) }
        } else null
    }

    private fun getCharacterClass(): CharacterClass {
        val className = aspects.firstOrNull { it.name == "classLevel" }?.values?.firstOrNull()?.uppercase() ?: "WARRIOR"
        return CharacterClass.valueOf(className)
    }

    private fun parseClassLevel(): ClassLevel {
        val level = aspects.firstOrNull { it.name == "classLevel" }?.values?.get(1)?.toIntOrNull() ?: 0
        return classLevelFromInt(level)
    }

    private fun getAge(): Int {
        return temporal["AGE"] ?: 20
    }

    private fun parsePersonality(): Map<Personality, Int> {
        val aspect = aspects.firstOrNull { it.name == "roleStats" }
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

    private fun parsePersonalityFirst() = personality.entries.maxBy { it.value }.key
    private fun parsePersonalitySecond() = personality.entries.filterNot { it.key == personalityFirst }.maxBy { it.value }.key

    private fun getFamily(): Family {
        val parents = aspects.filter { it.name == "childOf" }.map { it.values.first() }
        val children = aspects.filter { it.name == "parentOf" }.map { it.values.first() }
        val lover = aspects.firstOrNull { it.name == "lockedRelationship" && it.values.first() == "lover" }?.values?.get(1)
        return Family(lover, parents, children)
    }

    private fun getFriendships(): List<Friendship> {
        val relationships = aspects.filter { it.name.startsWith("relationship") }
        return relationships.mapNotNull { friendship ->
            val level = friendship.name.last().digitToIntOrNull()
            val kind = FriendshipKind.values().firstOrNull { friendship.name.contains(it.name.lowercase()) }
            val relativeId = friendship.values.first()
            if (level == null || kind == null) return@mapNotNull null
            Friendship(relativeId, kind, level)
        }
    }

    private fun parseSex(): Sex {
        return when {
            aspects.firstOrNull { it.name == "male" } != null -> Sex.MALE
            aspects.firstOrNull { it.name == "female" } != null -> Sex.FEMALE
            else -> Sex.UNKNOWN
        }
    }

    private fun getAttractedToWomen(): Boolean {
        return aspects.firstOrNull { it.name == "attractedToWomen" } != null
    }

    private fun parseHomeTown(): String {
        val aspect = history.firstOrNull { it.id == "hometown" }
        val relationship = aspect?.relationships?.firstOrNull()
        return relationship?.name ?: "hometown"
    }

    fun getCompatibility(other: Character): Int {
        val myStats = personality.entries.filter { it.value > 50 }.map { it.key }
        val otherStats = other.personality.entries.filter { it.value > 50 }.map { it.key }

        return myStats.sumOf { getCompatibility(it, otherStats) }
    }

    private fun parseAbilities(): List<Ability> {
        val themes = aspects.filter { it.name.startsWith("theme_") && it.name.count { letter -> letter == '_' } == 1 }

        val themeAspects = if (themes.isEmpty()) listOf() else aspects.filter { option ->
            themes.any { option.name.startsWith(it.name) }
        }

        val deckAspects = aspects.filter { it.name.contains("Deck") }
        return (themeAspects + deckAspects).map { aspect ->
            val name = getAspectProp("${aspect.name}.name")?.let { interpolate(it) } ?: aspect.name
            val description = getAspectProp("${aspect.name}.blurb")?.let { interpolate(it) } ?: aspect.name
            Ability(aspect.name, name, description)
        }
    }

    private fun parseHooks(): List<Hook> {
        val allHooks = aspects
            .filter { it.name.contains("hook_") && !it.name.endsWith("Resolved") && !it.name.contains("ThisCampaign") }
            .map { Hook(it.name.replace("hook_", "")) }

        val resolvedHooks = aspects
            .filter { it.name.contains("hook_") && it.name.endsWith("Resolved") && !it.name.contains("ThisCampaign") }
            .map { Hook(it.name.replace("hook_", "").replace("Resolved", ""), true) }

        val unresolved = allHooks.filter { option -> resolvedHooks.none { it.id == option.id } }

        return (resolvedHooks + unresolved).sortedBy { it.id }
    }

    private fun parsePrimaryStats(): Map<Stat, Float> {
        return aspects.filter { it.name == "historyStat2" }
            .mapNotNull { aspect ->
                aspect.values.first().toStat()?.let { stat ->
                    stat to (aspect.values[1].toFloatOrNull() ?: 0f)
                } ?: null.also{ println("Unknown Stat: ${aspect.values.first()}")}
            }
            .sortedByDescending { it.first }
            .groupBy { it.first }
            .mapValues { (_, nested) ->
                nested.sumOf { it.second.toDouble() }.toFloat()
            }

    }

}
