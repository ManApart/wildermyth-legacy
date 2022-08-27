import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import pages.toAspect

/*
Stats to display
For Bio/history: Use text overrides + additional info overrides. If they upload a strings file, parse from that
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
    val legacyAspects: List<Aspect> = listOf(),
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

    private fun getFamily(): Family {
        val parents = legacyAspects.filter { it.name == "childOf" }.map { it.values.first() }
        val children = legacyAspects.filter { it.name == "parentOf" }.map { it.values.first() }
        val lover = legacyAspects.firstOrNull { it.name == "lockedRelationship" && it.values.first() == "lover" }?.values?.last()
        return Family(lover, parents, children)
    }

}
