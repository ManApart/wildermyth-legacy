package pages

import AdditionalInfo
import Aspect
import Character
import HistoryEntry
import LegacyCharacter
import clearSections
import getAdditionalInfo
import getCharacter
import getCompany
import getPicture
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.*
import kotlinx.html.js.div
import org.w3c.dom.*
import saveAdditionalInfo
import kotlin.js.Date

fun characterDetail(character: LegacyCharacter) {
    val additionalInfo = getAdditionalInfo(character.uuid)
    val section = document.getElementById("character-detail-section")!!
    val snapshot = character.snapshots.last()
    clearSections()
    document.title = snapshot.name
    document.documentElement?.scrollTop = 0.0
    window.history.pushState(null, "null", "#detail/" + character.uuid)
    section.append {
        div {
            button {
                +"Back"
                onClickFunction = {
                    window.location.hash = "#${character.uuid}"
                }
            }
        }
        characterCard(character, false)
        div("character-section") { id = "history-entries" }
            .historySection(snapshot, additionalInfo)
//        div("character-section") { id = "abilities-section" }
//            .abilitiesSection(snapshot)
//        div("character-section") { id = "gear-section" }
//            .gearSection(snapshot)
        div("character-section") { id = "stats-section" }
            .statsSection(snapshot)
//        div("character-section") { id = "combat-section" }
//            .combatSection(snapshot)
        div("character-section") { id = "relationships-section" }
            .relationshipsSection(character)
        div("character-section") { id = "aspects-section" }
            .aspectsSection(snapshot)
    }

}

fun Element.historySection(character: Character, additionalInfo: AdditionalInfo) {
    innerHTML = ""
    append {
        div {
            h2 { +"History" }
            additionalInfo.history.forEachIndexed { i, entry ->
                div {
                    textArea {
                        id = "history-$i"
                        +" ${entry.textOverride}"
                        onChangeFunction = {
                            val area = document.getElementById(id) as HTMLTextAreaElement
                            entry.textOverride = area.value
                            saveAdditionalInfo(additionalInfo)
                        }
                    }
                }
            }
            button {
                +"Add"
                onClickFunction = {
                    val entry = HistoryEntry("" + additionalInfo.history.size, Date().getTime().toLong(), "")
                    additionalInfo.history.add(entry)
                    historySection(character, additionalInfo)
                }
            }
        }
    }
}

fun Element.abilitiesSection(character: Character) {
}

fun Element.gearSection(character: Character) {
}

fun Element.statsSection(character: Character) {
    val rawStats = character.aspects.filter { it.name == "historyStat2" }
    innerHTML = ""
    append {
        div {
            id = "stat-bonuses"
            h2 { +"Stat Bonuses" }
            table {
                tbody {
                    rawStats.sortedBy { it.name }.forEach { stat ->
                        val statName = stat.values.first().format()
                        val statVal = stat.values[1]
                        tr {
                            td { +statName }
                            td { +statVal }
                        }
                    }
                }
            }
        }
        div {
            id = "personality"
            h2 { +"Personality" }
            table {
                tbody {
                    character.personality.entries.sortedByDescending { it.value }.forEach { (type, amount) ->
                        tr {
                            td { +type.format() }
                            td { +"$amount" }
                        }
                    }
                }
            }
        }
    }
}

fun Element.combatSection(character: Character) {
}

fun Element.relationshipsSection(character: LegacyCharacter) {
    val snapshot = character.snapshots.last()
    innerHTML = ""
    append {
        div {
            h2 { +"Family" }
            with(snapshot.family) {
                parents.forEach { relativeCard(it, "Parent") }
                soulMate?.let { relativeCard(it, "Soulmate") }
                children.forEach { relativeCard(it, "Child") }
            }
        }

        if (character.friendships.isNotEmpty()) {
            div {
                h2 { +"Relationships" }
                character.friendships.forEach { friendShip ->
                    relativeCard(friendShip.relativeId, friendShip.kind.getTitle(friendShip.level))
                }
            }
        }
        div {
            h2 { +"Companies" }
            character.companyIds.forEach { companyCard(it) }
        }
    }
}

private fun TagConsumer<HTMLElement>.relativeCard(relativeUuid: String, relationship: String) {
    val relative = getCharacter(relativeUuid)
    val snapshot = relative?.snapshots?.last()

    if (relative != null && snapshot != null) {
        div("relationship") {
            onClickFunction = { characterDetail(relative) }
            getPicture("$relativeUuid/head")?.let { picture ->
                img(classes = "relationship-pic") {
                    src = picture
                }
            }
            div("relationship-text") {
                h4 { +relationship }
                p { +snapshot.name }
            }

        }
    } else {
        div {
            h4 { +relationship }
            p { +relativeUuid }
        }
    }
}

private fun TagConsumer<HTMLElement>.companyCard(companyId: String) {
    val company = getCompany(companyId)

    div("company") {
        h4 { +company.name }
        company.characters.mapNotNull { getCharacter(it) }.forEach { relative ->
            getPicture("${relative.uuid}/head")?.let { picture ->
                div("company-friend-pic-wrapper") {
                    img(classes = "company-friend-pic") {
                        src = picture
                        alt = relative.snapshots.last().name
                        onClickFunction = { characterDetail(relative) }
                    }
                }
            }
        }

    }
}

fun Element.aspectsSection(character: Character) {
    innerHTML = ""
    append {
        div {
            h2 { +"Aspects" }
            table {
                tbody {
                    character.aspects.filterNot { it.hiddenAspect() }.sortedBy { it.name }.forEach { aspect ->
                        tr {
                            td { +aspect.name }
                            td { +aspect.values.joinToString(", ") }
                        }
                    }
                }
            }
        }
    }
}

private fun Aspect.hiddenAspect(): Boolean {
    return name in listOf("familyWith", "childOf", "parentOf", "historyStat2", "roleStats")
            || name.startsWith("human")
            || name.startsWith("relationship")
}

