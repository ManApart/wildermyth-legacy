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
    favicon.setAttribute("href", "favicon.png")
    buildNav()
    buildCharacters(section, getCharacters())
    scrollToCharacter()
}

fun characterSearch() {
    val characters = getCharacters()
        .filterFavorites(searchOptions.favoritesOnly)
        .hideNPC(searchOptions.hideNPC)
        .filterSearch(searchOptions.searchText)
    filterCharacterDoms(characters)
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
        characterFilter(character, searchText)
                || latestFilter(character.snapshots.last(), searchText)
                || snapshotsFilter(character.snapshots, searchText)
    }
}

private fun characterFilter(character: LegacyCharacter, searchText: String): Boolean {
    return character.legacyTierLevel.format().lowercase().contains(searchText)
}

private fun snapshotsFilter(snapshots: Array<Character>, searchText: String): Boolean {
    return snapshots.any { snapshot ->
        snapshot.name.lowercase().contains(searchText)
                || snapshot.aspects.any { it.name.lowercase().contains(searchText) }
    }
}

private fun latestFilter(latest: Character, searchText: String): Boolean {
    return latest.classLevel.takeIf { it != undefined }?.name?.lowercase()?.contains(searchText) ?: false ||
            latest.personalityFirst.name.lowercase().contains(searchText) ||
            latest.personalitySecond.name.lowercase().contains(searchText)
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
                +"$age year old ${classLevel.format()} ${className.capitalize()} ${character.legacyTierLevel.format()}$npc"
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

                h1 {
                    +name
                }
                if (character.npc) {
                    p("character-list-item-npc") { +"(npc)" }
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