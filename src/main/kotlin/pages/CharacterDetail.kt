package pages

import AdditionalInfo
import Character
import HistoryEntry
import clearSections
import doRouting
import el
import getAdditionalInfo
import getCharacter
import getPicture
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.*
import kotlinx.html.js.button
import kotlinx.html.js.div
import kotlinx.html.js.textArea
import org.w3c.dom.*
import saveAdditionalInfo
import kotlin.js.Date

fun characterDetail(character: Character) {
    val additionalInfo = getAdditionalInfo(character.uuid)
    val section = document.getElementById("character-cards-section")!!
    clearSections()
    document.title = character.name
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
        characterCard(character)
        div("character-section") { id = "history-entries" }
            .historySection(character, additionalInfo)
        div("character-section") { id = "abilities-section" }
            .abilitiesSection(character, additionalInfo)
        div("character-section") { id = "gear-section" }
            .gearSection(character, additionalInfo)
        div("character-section") { id = "stats-section" }
            .statsSection(character, additionalInfo)
        div("character-section") { id = "combat-section" }
            .combatSection(character, additionalInfo)
        div("character-section") { id = "relationships-section" }
            .relationshipsSection(character, additionalInfo)
        div("character-section") { id = "aspects-section" }
            .aspectsSection(character, additionalInfo)
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

fun Element.abilitiesSection(character: Character, additionalInfo: AdditionalInfo) {
    innerHTML = ""
    append {
        div {
            h2 { +"Abilities" }
        }
    }
}

fun Element.gearSection(character: Character, additionalInfo: AdditionalInfo) {
    innerHTML = ""
    append {
        div {
            h2 { +"Gear" }
        }
    }
}

fun Element.statsSection(character: Character, additionalInfo: AdditionalInfo) {
    innerHTML = ""
    append {
        div {
            h2 { +"Stats" }
        }
    }
}

fun Element.combatSection(character: Character, additionalInfo: AdditionalInfo) {
    innerHTML = ""
    append {
        div {
            h2 { +"Combat" }
        }
    }
}

fun Element.relationshipsSection(character: Character, additionalInfo: AdditionalInfo) {
    innerHTML = ""
    append {
        div {
            h2 { +"Relationships" }
            with(character.family) {
                parents.forEach { relativeCard(it, "Parent") }
                soulMate?.let { relativeCard(it, "Soulmate") }
                children.forEach { relativeCard(it, "Child") }
            }
        }
    }
}

private fun TagConsumer<HTMLElement>.relativeCard(relativeUuid: String, relationship: String) {
    val relative = getCharacter(relativeUuid)

    if (relative != null) {
        div("relationship") {
            onClickFunction = { characterDetail(relative) }
            getPicture("$relativeUuid/head")?.let { picture ->
                img(classes = "relationship-pic") {
                    src = picture
                }
            }
            div("relationship-text") {
                h4 { +relationship }
                p { +relative.name }
            }

        }
    } else {
        div {
            h4 { +relationship }
            p { +relativeUuid }
        }
    }
}

fun Element.aspectsSection(character: Character, additionalInfo: AdditionalInfo) {
    innerHTML = ""
    append {
        div {
            h2 { +"Aspects" }
        }
    }
}

