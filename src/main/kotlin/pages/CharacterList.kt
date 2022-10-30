package pages

import Character
import LegacyCharacter
import characterCards
import clearSections
import el
import favicon
import getAdditionalInfo
import getCharacters
import getPicture
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
import kotlinx.dom.removeClass
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.div
import kotlinx.html.js.onClickFunction
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import saveAdditionalInfo
import saveSearch
import searchOptions


fun displayCharacters() {
    val section = el("character-cards-section")
    clearSections()
    document.title = "Wildermyth Legacy"
    setFavicon(getCharacters().random())
    buildNav()
    buildCharacters(section, getCharacters())
    scrollToCharacter()
}

fun filterCharacterDoms(characters: List<LegacyCharacter>) {
    characterCards.values.forEach {
        it.addClass("hidden")
        it.removeClass("visible-inline-block")
    }
    characters.forEach {
        characterCards[it.uuid]?.apply {
            addClass("visible-inline-block")
            removeClass("hidden")
        }
    }
}

fun buildCharacters(section: Element, characters: List<LegacyCharacter>) {
    section.innerHTML = ""
    section.append {
        characters.also { println("Building ${it.size} characters") }
            .sorted()
            .forEach { character ->
                if (searchOptions.listView) {
                    characterListItem(character, character.snapshots.last(), true)
                } else {
                    characterCard(character, character.snapshots.last(), true)
                }
            }
    }
    characterCards = characters.associate { it.uuid to document.getElementById(it.uuid) as HTMLElement }
}

fun List<LegacyCharacter>.sorted(): List<LegacyCharacter> {
    return sortedWith(compareBy<LegacyCharacter> { !getAdditionalInfo(it.uuid).favorite }
        .thenBy { it.snapshots.last().name.split(" ").last() }
        .thenBy { it.snapshots.last().name.split(" ").first() })
}

private fun scrollToCharacter() {
    val hashId = window.location.hash.replace("#", "")
    document.getElementById(hashId)?.scrollIntoView()
}

fun TagConsumer<HTMLElement>.characterCard(character: LegacyCharacter, snapshot: Character, clickable: Boolean) {
    with(snapshot) {
        val className = characterClass.name.lowercase()
        val animDelay = (0..10).random() / 10.0
        div("character-card") {
            id = character.uuid
            if (clickable) onClickFunction = { characterDetail(character) }
            val info = getAdditionalInfo(character.uuid)
            img {
                classes = setOf("favorite-image")
                id = character.uuid + "-star"
                src = if (info.favorite) "images/star-active.png" else "images/star.png"
                onClickFunction = { e ->
                    e.stopPropagation()
                    info.favorite = !info.favorite
                    saveAdditionalInfo(info)
                    (document.getElementById(character.uuid + "-star") as HTMLImageElement).src = if (info.favorite) "images/star-active.png" else "images/star.png"
                }
            }
            h1 {
                +name
            }
            div("character-personality") {
                +"${personalityFirst.format()} ${personalitySecond.format()}"
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
                val npc = if (character.npc) " (npc)" else ""
                +"${classLevel.format()} ${className.capitalize()} ${character.legacyTierLevel.format()}$npc "
                repeat(1 + character.legacyTierLevel.ordinal) {
                    img(classes = "legacy-tier-image") {
                        src = "images/star-active.png"
                    }
                }
            }
            div("character-bio") {
                +bio
            }
        }
    }
}

fun TagConsumer<HTMLElement>.characterListItem(character: LegacyCharacter, snapshot: Character, clickable: Boolean) {
    with(snapshot) {
        div("character-list-item") {
            id = character.uuid
            div("character-list-item-inner") {
                if (clickable) onClickFunction = { characterDetail(character) }
                div("character-list-item-head-wrapper") {
                    getPicture("$uuid/head")?.let { picture ->
                        img {
                            src = picture
                            classes = setOf("character-list-item-head")
                        }
                    }
                }

                div("character-list-info") {
                    h1 {
                        +name
                    }
                    if (character.npc) {
                        p("character-list-item-npc") { +"(npc)" }
                    }
                    div("character-list-summary") {
                        +"${characterClass.name.format()} ${character.legacyTierLevel.format()} "
                        repeat(1 + character.legacyTierLevel.ordinal) {
                            img(classes = "legacy-tier-image") {
                                src = "images/star-active.png"
                            }
                        }
                    }
                }
            }
            val info = getAdditionalInfo(character.uuid)
            img {
                classes = setOf("favorite-image")
                id = character.uuid + "-star"
                src = if (info.favorite) "images/star-active.png" else "images/star.png"
                onClickFunction = { e ->
                    e.stopPropagation()
                    info.favorite = !info.favorite
                    saveAdditionalInfo(info)
                    (document.getElementById(character.uuid + "-star") as HTMLImageElement).src = if (info.favorite) "images/star-active.png" else "images/star.png"
                }
            }
        }
    }
}

fun Enum<*>.format(): String {
    return if (this == undefined) "" else name.split("_").joinToString(" ") { it.lowercase().capitalize() }
}

fun String.format(): String {
    return lowercase().capitalize()
}

private val capitalSplitRegex = "(?=\\p{Upper})".toRegex()
fun String.splitByCapitals() : String {
    return split(capitalSplitRegex).joinToString(" ")
}

fun String.removeAll(vararg parts: String): String {
    return parts.fold(this){acc, s -> acc.replace(s, "") }
}