package pages

import getAdditionalInfo
import jsonMapper
import kotlinx.browser.document
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.input
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onKeyUpFunction
import kotlinx.serialization.encodeToString
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import searchOptions

fun buildNav() {
    val nav = document.getElementById("nav")!!
    nav.append {
        button {
            id = "upload-button"
            +"Upload"
            onClickFunction = { importMenu() }
        }

        input {
            id = "search"
            placeholder = "Filter: Name, Aspect etc. Comma separated"
            onKeyUpFunction = {
                searchOptions.searchText = (document.getElementById("search") as HTMLInputElement).value
                characterSearch()
            }
        }
        div {
            div {
                id = "search-checks"
                div {
                    input(InputType.checkBox) {
                        id = "favorites-only"
                    }
                    label {
                        +"Only Favorites"
                        onClickFunction = {
                            val box = (document.getElementById("favorites-only") as HTMLInputElement)
                            box.checked = !box.checked
                        }
                    }
                    onClickFunction = {
                        searchOptions.favoritesOnly = (document.getElementById("favorites-only") as HTMLInputElement).checked
                        characterSearch()
                    }
                }
                div {
                    input(InputType.checkBox) {
                        id = "hide-npc"

                    }
                    label {
                        +"Hide NPCs"
                        onClickFunction = {
                            val box = (document.getElementById("hide-npc") as HTMLInputElement)
                            box.checked = !box.checked
                        }
                    }
                    onClickFunction = {
                        searchOptions.hideNPC = (document.getElementById("hide-npc") as HTMLInputElement).checked
                        characterSearch()
                    }
                }
            }
            button {
                id = "export-button"
                +"Export"
                onClickFunction = { downloadAdditionalInfo() }
            }
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
