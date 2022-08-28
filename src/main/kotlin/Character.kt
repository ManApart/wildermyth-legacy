import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import pages.toAspect

/*
Stats to display
For Bio/history: Use text overrides + additional info overrides. If they upload a strings file, parse from that
Hometown
 */

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

    @Transient
    val characterClass = getCharacterClass()

    @Transient
    val classLevel = getClassLevel()

    @Transient
    val age = getAge()

    @Transient
    val personality = getPersonality()

    @Transient
    val family = getFamily()

    @Transient
    val friendships = getFriendships()

    private fun getBio(): String {
        val historyOverrides = history.joinToString(" ") { it.textOverride }
        return historyOverrides.ifBlank {
            listOfNotNull(
                history.firstOrNull { it.id.startsWith("origin") }?.id,
                history.firstOrNull { it.id.startsWith("dote") }?.id,
                history.firstOrNull { it.id.startsWith("mote") }?.id
            ).joinToString(", ")
        }
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

    private fun getPersonality(): Map<Personality, Int> {
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

//    private fun getHomeTown(): String {
//        history.firstOrNull { it.id == "hometown" }?.relationships.first().name
//    }

}
