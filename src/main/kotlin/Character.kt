import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import wildermyth.interpolate

@Serializable
data class LegacyCharacter(val uuid: String, val snapshots: Array<Character>, val companyIds: List<String> = listOf()){
    @Transient
    val friendships = getFriendships()

    private fun getFriendships(): List<Friendship>{
        return snapshots.flatMap { it.friendships }.groupBy { it.relativeId }.map { (_, options) -> options.maxBy { it.level } }
    }
}

@Serializable
data class Character(
    val uuid: String,
    val name: String,
    val aspects: List<Aspect> = listOf(),
    val temporal: Map<String, Int> = mapOf(),
    val history: List<HistoryEntry> = listOf(),
) {

    @Transient
    val bio = getBio()

    //For some reason these can be undefined unless the getter is called elsewhere. Make a var so they can be updated
    @Transient
    var sex = getSex()

    @Transient
    val attractedToWomen = getAttractedToWomen()

    @Transient
    val characterClass = getCharacterClass()

    @Transient
    val classLevel = getClassLevel()

    @Transient
    val age = getAge()

    @Transient
    var personality = getPersonality()

    @Transient
    val family = getFamily()

    @Transient
    val friendships = getFriendships()

    @Transient
    val hometown = getHomeTown()

    private fun getBio(): String {
        val historyOverrides = history.joinToString(" ") { it.textOverride }
        return historyOverrides.ifBlank {
            listOfNotNull(
                getBioString("origin"),
                getBioString("dote"),
                getBioString("mote"),
            ).joinToString(" ")
        }
    }

    private fun getBioString(startsWith: String): String? {
        return history.firstOrNull { it.id.startsWith(startsWith) }?.id?.let { propId -> getStoryProp(propId) }?.let { storyProp -> interpolate(storyProp) }
    }

    private fun getCharacterClass(): CharacterClass {
        val className = aspects.firstOrNull { it.name == "classLevel" }?.values?.firstOrNull()?.uppercase() ?: "WARRIOR"
        return CharacterClass.valueOf(className)
    }

    private fun getClassLevel(): ClassLevel {
        val level = aspects.firstOrNull { it.name == "classLevel" }?.values?.get(1)?.toIntOrNull() ?: 0
        return classLevelFromInt(level)
    }

    private fun getAge(): Int {
        return temporal["AGE"] ?: 20
    }

    fun getPersonality(): Map<Personality, Int> {
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

    private fun getFamily(): Family {
        val parents = aspects.filter { it.name == "childOf" }.map { it.values.first() }
        val children = aspects.filter { it.name == "parentOf" }.map { it.values.first() }
        val lover = aspects.firstOrNull { it.name == "lockedRelationship" && it.values.first() == "lover" }?.values?.last()
        return Family(lover, parents, children)
    }

    private fun getFriendships(): List<Friendship> {
        val relationships = aspects.filter { it.name.startsWith("relationship") }
        return relationships.mapNotNull { friendship ->
            val level = friendship.name.last().digitToIntOrNull()
            val kind = FriendshipKind.values().firstOrNull { friendship.name.contains(it.name.lowercase()) }
            val relativeId = friendship.values.last()
            if (level == null || kind == null) return@mapNotNull null
            Friendship(relativeId, kind, level)
        }
    }

    fun getSex(): Sex {
        return when {
            aspects.firstOrNull { it.name == "male" } != null -> Sex.MALE
            aspects.firstOrNull { it.name == "female" } != null -> Sex.FEMALE
            else -> Sex.UNKNOWN
        }
    }

    private fun getAttractedToWomen(): Boolean {
        return aspects.firstOrNull { it.name == "attractedToWomen" } != null
    }

    private fun getHomeTown(): String {
        return history.firstOrNull { it.id == "hometown" }?.relationships?.firstOrNull()?.name ?: "hometown"
    }

}
