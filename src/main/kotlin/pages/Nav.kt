package pages

import characterSearch
import getAdditionalInfo
import getCharacters
import jsonMapper
import kotlinx.browser.document
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onKeyUpFunction
import kotlinx.serialization.encodeToString
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import saveSearch
import searchOptions

fun buildNav() {
    val nav = document.getElementById("nav")!!
    nav.append {
        div("row") {
            id="top-nav"
            button {
                id = "upload-button"
                +"Upload"
                onClickFunction = { importMenu() }
            }
            input {
                id = "search"
                placeholder = "Filter: Name, Aspect etc. Comma separated"
                value = searchOptions.searchText
                onKeyUpFunction = {
                    searchOptions.searchText = (document.getElementById("search") as HTMLInputElement).value
                    characterSearch()
                }
            }
            button {
                id = "export-button"
                +"Export"
                onClickFunction = { downloadAdditionalInfo() }
            }
        }
        div("row") {
            checkBox("favorites-only", "Only Favorites", searchOptions.favoritesOnly) {
                searchOptions.favoritesOnly = it
                characterSearch()
            }
            checkBox("hide-npc", "Hide NPCs", searchOptions.hideNPC){
                searchOptions.hideNPC = it
                characterSearch()
            }
            checkBox("list-view", "View as List", searchOptions.listView) {
                searchOptions.listView = it
                val section = document.getElementById("character-cards-section")!!
                saveSearch(searchOptions)
                buildCharacters(section, getCharacters())
                characterSearch()
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
    val download = document.createElement("a") as HTMLElement
    download.setAttribute("href", "data:text/plain;charset=utf-8," + jsonMapper.encodeToString(getAdditionalInfo()))
    download.setAttribute("download", "AdditionalInfo.json")
    document.body?.append(download)
    download.click()
    document.body?.removeChild(download)
}
