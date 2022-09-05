package pages

import LegacyCharacter
import buildNav
import clearSections
import favicon
import getCharacters
import getPicture
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.TagConsumer
import kotlinx.html.classes
import kotlinx.html.dom.append
import kotlinx.html.id
import kotlinx.html.js.div
import kotlinx.html.js.h1
import kotlinx.html.js.img
import kotlinx.html.js.onClickFunction
import kotlinx.html.style
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement


fun displayCharacters() {
    val section = document.getElementById("character-cards-section")!!
    clearSections()
    document.title = "Wildermyth Legacy"
    favicon.setAttribute("href", "favicon.png")
    buildNav()
    val characters = getCharacters()
    buildCharacters(section, characters)
    scrollToCharacter()
}

private fun buildCharacters(section: Element, characters: List<LegacyCharacter>) {
    section.append {
        characters.also { println("Building ${it.size} characters") }
            .sortedWith(compareBy<LegacyCharacter> { it.snapshots.last().name.split(" ").last() }.thenBy { it.snapshots.last().name.split(" ").first() })
            .forEach { character ->
                characterCard(character, true)
            }
    }
}

private fun scrollToCharacter() {
    val hashId = window.location.hash.replace("#", "")
    document.getElementById(hashId)?.scrollIntoView()
}

fun TagConsumer<HTMLElement>.characterCard(character: LegacyCharacter, clickable: Boolean) {
    with(character.snapshots.last()) {
        val className = characterClass.name.lowercase()
        val topTrait = personality.entries.maxBy { it.value }.key
        val secondTrait = personality.entries.filterNot { it.key == topTrait }.maxBy { it.value }.key
        val animDelay = (0..10).random() / 10.0
        div("character-card") {
            id = character.uuid
            if (clickable) onClickFunction = { characterDetail(character) }
            h1 {
                +name
            }
            div("character-personality") {
                +"${topTrait.format()} ${secondTrait.format()}"
            }
            div("character-portrait ${className}-portrait") {
                getPicture("$uuid/body")?.let { picture ->
                    img {
                        src = picture
                        classes = setOf("character-body", "${className}-body")
                        style = "animation-delay: ${animDelay}s"
                    }
                }
                getPicture("$uuid/head")?.let { picture ->
                    img {
                        src = picture
                        classes = setOf("character-head", "${className}-head")
                        style = "animation-delay: ${animDelay + .05}s"
                    }
                }
            }
            div("character-summary") {
                +"$age year old ${classLevel.format()} ${className.capitalize()}"
            }
            div("character-bio") {
                +bio
            }
        }
    }
}

fun Enum<*>.format(): String {
    return if (this == undefined) "" else name.lowercase().capitalize()
}

fun String.format(): String {
    return lowercase().capitalize()
}