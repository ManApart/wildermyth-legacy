package pages

import LegacyCharacter
import clearSections
import favicon
import getAdditionalInfo
import getCharacter
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
import org.w3c.dom.HTMLImageElement
import saveAdditionalInfo
import saveSearch
import searchOptions


fun displayCharacters() {
    val section = document.getElementById("character-cards-section")!!
    clearSections()
    document.title = "Wildermyth Legacy"
    favicon.setAttribute("href", "favicon.png")
    buildNav()
    buildCharacters(section, getCharacters())
    scrollToCharacter()
}

fun characterSearch() {
    val section = document.getElementById("character-cards-section")!!
    val characters = getCharacters()
        .filterFavorites(searchOptions.favoritesOnly)
        .hideNPC(searchOptions.hideNPC)
        .filterSearch(searchOptions.searchText)
    buildCharacters(section, characters)
    saveSearch(searchOptions)
}

private fun List<LegacyCharacter>.filterFavorites(doFilter: Boolean): List<LegacyCharacter> {
    return if (doFilter) filter { getAdditionalInfo(it.uuid).favorite } else this
}

private fun List<LegacyCharacter>.hideNPC(doFilter: Boolean): List<LegacyCharacter> {
    return if (doFilter) filter { !it.npc } else this
}

private fun List<LegacyCharacter>.filterSearch(searchText: String): List<LegacyCharacter> {
    return if (searchText.isBlank()) this else {
        searchText.lowercase().split(",").fold(this) { acc, s -> filterCharacters(acc, s) }
    }
}

private fun filterCharacters(initial: List<LegacyCharacter>, searchText: String): List<LegacyCharacter> {
    return initial.filter { character ->
        val latest = character.snapshots.last()
        character.snapshots.any { it.name.lowercase().contains(searchText) } ||
                character.snapshots.flatMap { it.aspects }.any { it.name.lowercase().contains(searchText) } ||
                latest.classLevel.name.lowercase().contains(searchText) ||
                latest.personalityFirst.name.lowercase().contains(searchText) ||
                latest.personalitySecond.name.lowercase().contains(searchText)

    }
}

private fun buildCharacters(section: Element, characters: List<LegacyCharacter>) {
    if (searchOptions.listView) {
        buildCharacterListView(section, characters)
    } else {
        buildCharacterCards(section, characters)
    }
}

private fun buildCharacterCards(section: Element, characters: List<LegacyCharacter>) {
    section.innerHTML = ""
    section.append {
        characters.also { println("Building cards for ${it.size} characters") }
            .sorted()
            .forEach { character ->
                characterCard(character, true)
            }
    }
}

private fun buildCharacterListView(section: Element, characters: List<LegacyCharacter>) {
    section.innerHTML = ""
    section.append {
        characters.also { println("Building List of ${it.size} characters") }
            .sorted()
            .forEach { character ->
                characterListItem(character, true)
            }
    }
}

private fun List<LegacyCharacter>.sorted(): List<LegacyCharacter> {
    return sortedWith(compareBy<LegacyCharacter> { !getAdditionalInfo(it.uuid).favorite }
        .thenBy { it.snapshots.last().name.split(" ").last() }
        .thenBy { it.snapshots.last().name.split(" ").first() })
}

private fun scrollToCharacter() {
    val hashId = window.location.hash.replace("#", "")
    document.getElementById(hashId)?.scrollIntoView()
}

fun TagConsumer<HTMLElement>.characterCard(character: LegacyCharacter, clickable: Boolean) {
    with(character.snapshots.last()) {
        val className = characterClass.name.lowercase()
        val animDelay = (0..10).random() / 10.0
        div("character-card") {
            id = character.uuid
            if (clickable) onClickFunction = { characterDetail(character) }
            val info = getAdditionalInfo(character.uuid)
            img {
                classes = setOf("favorite-image")
                id = character.uuid + "-star"
                src = if (info.favorite) "./star-active.png" else "./star.png"
                onClickFunction = { e ->
                    e.stopPropagation()
                    info.favorite = !info.favorite
                    saveAdditionalInfo(info)
                    (document.getElementById(character.uuid + "-star") as HTMLImageElement).src = if (info.favorite) "./star-active.png" else "./star.png"
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
                +"$age year old ${classLevel.format()} ${className.capitalize()}$npc"
            }
            div("character-bio") {
                +bio
            }
        }
    }
}

fun TagConsumer<HTMLElement>.characterListItem(character: LegacyCharacter, clickable: Boolean) {
    with(character.snapshots.last()) {
        div("character-list-item") {
            id = character.uuid
            if (clickable) onClickFunction = { characterDetail(character) }
            val info = getAdditionalInfo(character.uuid)
            div("character-list-item-head-wrapper") {
                getPicture("$uuid/head")?.let { picture ->
                    img {
                        src = picture
                        classes = setOf("character-list-item-head")
                    }
                }
            }
            img {
                classes = setOf("favorite-image")
                id = character.uuid + "-star"
                src = if (info.favorite) "./star-active.png" else "./star.png"
                onClickFunction = { e ->
                    e.stopPropagation()
                    info.favorite = !info.favorite
                    saveAdditionalInfo(info)
                    (document.getElementById(character.uuid + "-star") as HTMLImageElement).src = if (info.favorite) "./star-active.png" else "./star.png"
                }
            }
            h1 {
                +name
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