package pages

import Character
import clearSections
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.dom.append
import kotlinx.html.js.button
import kotlinx.html.js.div
import kotlinx.html.js.onClickFunction

fun characterDetail(character: Character) {
    println("Detail for ${character.name}")
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
    }

}