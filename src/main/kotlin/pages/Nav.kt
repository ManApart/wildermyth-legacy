package pages

import CharacterSort
import characterSearch
import format
import getAdditionalInfo
import getCharacters
import jsonMapper
import kotlinx.browser.document
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onKeyUpFunction
import kotlinx.serialization.encodeToString
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.xhr.XMLHttpRequest
import searchOptions

fun buildNav() {
    val nav = document.getElementById("nav")!!
    nav.append {
        div("row") {
            id = "top-nav"
            button(classes = "nav-button") {
                id = "upload-button"
                +"Upload"
                onClickFunction = { importMenu() }
            }
            button(classes = "nav-button") {
                id = "profile-button"
                +"Profile"
                onClickFunction = { profile() }
            }
            button(classes = "nav-button") {
                id = "export-button"
                +"Export"
                title = "Download Journals, Stars, etc"
                onClickFunction = { downloadAdditionalInfo() }
            }
        }
        div("row") {
            id = "search-check-boxes"
            checkBox("favorites-only", "Only Favorites", searchOptions.favoritesOnly) {
                searchOptions.favoritesOnly = it
                characterSearch()
            }
            checkBox("favorites-first", "Favorites First", searchOptions.favoritesFirst) {
                searchOptions.favoritesFirst = it
                val section = document.getElementById("character-cards-section")!!
                buildCharacters(section, getCharacters(), getAdditionalInfo())
                characterSearch()
            }
            checkBox("hide-npc", "Hide NPCs", searchOptions.hideNPC) {
                searchOptions.hideNPC = it
                characterSearch()
            }
            checkBox("list-view", "View as List", searchOptions.listView) {
                searchOptions.listView = it
                val section = document.getElementById("character-cards-section")!!
                buildCharacters(section, getCharacters(), getAdditionalInfo(), true)
                characterSearch()
            }
        }
        div {
            id = "search-span"
            input {
                id = "search"
                placeholder = "Filter: Name, Aspect etc. Comma separated"
                value = searchOptions.searchText
                onKeyUpFunction = {
                    searchOptions.searchText = (document.getElementById("search") as HTMLInputElement).value
                    characterSearch()
                }
            }
            img {
                id = "clear-search"
                src = "./images/no.png"
                onClickFunction = {
                    searchOptions.searchText = ""
                    (document.getElementById("search") as HTMLInputElement).value = ""
                    characterSearch()
                }
            }
        }
        div {
            id = "character-sort-span"
            label { +"Sort:" }
            select {
                id = "character-sort-select"
                CharacterSort.values().forEach {
                    option {
                        +it.format()
                        selected = searchOptions.sort == it
                    }
                }

                onChangeFunction = {
                    val characterSelect = (document.getElementById(id) as HTMLSelectElement)
                    searchOptions.sort = CharacterSort.values()[characterSelect.selectedIndex]
                    val section = document.getElementById("character-cards-section")!!
                    buildCharacters(section, getCharacters(), getAdditionalInfo())
                    characterSearch()
                }
            }
        }
    }

}

private fun TagConsumer<HTMLElement>.checkBox(id: String, text: String, startChecked: Boolean, onClick: (Boolean) -> Unit) {
    var checked = startChecked
    button {
        this.id = id
        classes = setOf("button")
        +(if (checked) "☑ $text" else "☐ $text")
        onClickFunction = {
            checked = !checked
            document.getElementById(id)!!.textContent = if (checked) "☑ $text" else "☐ $text"
            onClick(checked)
        }
    }
}

private fun downloadAdditionalInfo() {
    manualDownload()
    attemptAutoDownload()
}

private fun attemptAutoDownload() {
    XMLHttpRequest().apply {
        open("POST", "http://localhost:3333/wildermyth/additional-info")
        setRequestHeader("Content-Type", "application/json;charset=UTF-8")
        onerror = {
            println("Call to local failed")
        }
        onload = {
            println("Saved to local")
        }
        send(jsonMapper.encodeToString(getAdditionalInfo()))
    }
}

private fun manualDownload() {
    val download = document.createElement("a") as HTMLElement
    download.setAttribute("href", "data:text/plain;charset=utf-8," + jsonMapper.encodeToString(getAdditionalInfo()))
    download.setAttribute("download", "AdditionalInfo.json")
    document.body?.append(download)
    download.click()
    document.body?.removeChild(download)
}
