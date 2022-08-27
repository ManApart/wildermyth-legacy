package pages

import Character
import buildNav
import clearSections
import getCharacter
import getCharacterList
import getPicture
import jsonMapper
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.html.TagConsumer
import kotlinx.html.classes
import kotlinx.html.dom.append
import kotlinx.html.js.div
import kotlinx.html.js.h1
import kotlinx.html.js.img
import kotlinx.html.js.onClickFunction
import kotlinx.html.style
import kotlinx.serialization.decodeFromString
import org.w3c.dom.HTMLElement
import org.w3c.dom.get


fun displayCharacters() {
    val section = document.getElementById("character-cards-section")!!
    clearSections()
    document.title = "Wildermyth Legacy"
    window.location.hash = ""
    buildNav()
    document.documentElement?.scrollTop = 0.0
    section.append {
        getCharacterList().also { println("Building ${it.size} characters.") }
            .mapNotNull { getCharacter(it) }
            .sortedWith(compareBy<Character> { it.name.split(" ").last() }.thenBy { it.name.split(" ").first() })
            .forEach { character ->
                characterCard(character)
            }
    }
}

fun TagConsumer<HTMLElement>.characterCard(character: Character) {
    with(character) {
//                    println("Building ${character.name}")
        val className = characterClass.name.lowercase()
        val topTrait = personality.entries.maxBy { it.value }.key
        val secondTrait = personality.entries.filterNot { it.key == topTrait }.maxBy { it.value }.key
        val animDelay = (0..10).random() / 10.0
        div("character-card") {
            onClickFunction = { characterDetail(character) }
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
    return name.lowercase().capitalize()
}