package pages

import AdditionalInfo
import Character
import HistoryEntry
import clearSections
import getAdditionalInfo
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.dom.append
import kotlinx.html.id
import kotlinx.html.js.*
import org.w3c.dom.Element
import org.w3c.dom.HTMLAreaElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import saveAdditionalInfo
import kotlin.js.Date

fun characterDetail(character: Character) {
    println("Detail for ${character.name}")
    val additionalInfo = getAdditionalInfo(character.uuid)
    val section = document.getElementById("character-cards-section")!!
    clearSections()
    window.location.hash = character.uuid
    document.title = character.name
    section.append {
        div {
            button {
                +"Back"
                onClickFunction = { displayCharacters() }
            }
        }
        characterCard(character)
        div {
            id = "history-entries"
        }
    }

    historySection(document.getElementById("history-entries")!!, character, additionalInfo)
}

fun historySection(parent: Element, character: Character, additionalInfo: AdditionalInfo) {
    parent.innerHTML = ""
    parent.append {
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
                    historySection(parent, character, additionalInfo)
                }
            }
        }
    }
}