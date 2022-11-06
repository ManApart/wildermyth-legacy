package pages

import AdditionalInfo
import Character
import CharacterSort
import LegacyCharacter
import characterCards
import clearSections
import el
import format
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
import log
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import previousSearch
import saveAdditionalInfo
import searchOptions
import sorted


fun displayCharacters() {
    val section = el("character-cards-section")
    clearSections()
    document.title = "Wildermyth Legacy"
    setFavicon(getCharacters().random())
    buildNav()
    buildCharacters(section, getCharacters(), getAdditionalInfo())
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

fun buildCharacters(section: Element, characters: List<LegacyCharacter>, info: Map<String, AdditionalInfo>, forceRebuild: Boolean = false) {
    section.innerHTML = ""
    section.append {
        div {
            id = "all-characters"

        }
    }
    val characterDoms = el("all-characters")
    if (forceRebuild || characterCards.keys.size < 2) {
        val sortedCharacters = characters
            .also { println("Building ${it.size} characters") }
            .sorted(searchOptions.sort, searchOptions.favoritesFirst, info)
        characterDoms.append {
            sortedCharacters.forEach { character ->
                val characterInfo = info[character.uuid] ?: AdditionalInfo(character.uuid)
                if (searchOptions.listView) {
                    characterListItem(character, character.snapshots.last(), characterInfo, true)
                } else {
                    characterCard(character, character.snapshots.last(), characterInfo, true)
                }
            }
        }
        characterCards = sortedCharacters.associate { it.uuid to document.getElementById(it.uuid) as HTMLElement }
    } else if (searchOptions.sort != previousSearch?.sort || searchOptions.favoritesFirst != previousSearch?.favoritesFirst) {
        val sortedCharacters = characters.sorted(searchOptions.sort, searchOptions.favoritesFirst, info)
        sortedCharacters.forEach { character ->
            characterCards[character.uuid]?.let { characterDom ->
                characterDoms.appendChild(characterDom)
            }
        }
        //resort character doms for next search
        characterCards = sortedCharacters.associate { it.uuid to characterCards[it.uuid]!! }
    } else {
        characterCards.values.forEach { characterDoms.appendChild(it) }
    }
}

private fun scrollToCharacter() {
    val hashId = window.location.hash.replace("#", "")
    document.getElementById(hashId)?.scrollIntoView()
}

fun TagConsumer<HTMLElement>.characterCard(character: LegacyCharacter, snapshot: Character, info: AdditionalInfo, clickable: Boolean) {
    with(snapshot) {
        val className = characterClass.name.lowercase()
        val animDelay = (0..10).random() / 10.0
        div("character-card") {
            id = character.uuid
            if (clickable) onClickFunction = { characterDetail(character) }
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

fun TagConsumer<HTMLElement>.characterListItem(character: LegacyCharacter, snapshot: Character, info: AdditionalInfo, clickable: Boolean) {
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