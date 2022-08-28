import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.table
import kotlinx.serialization.encodeToString
import org.w3c.dom.HTMLElement
import pages.characterDetail
import pages.displayCharacters
import pages.importMenu
import pages.loadExample

val jsonMapper = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

fun main() {
    window.onload = {
        loadExample()
    }
    window.addEventListener("popstate", { e ->
        doRouting()
    })
}

fun doRouting() {
    doRouting(window.location.hash)
}

fun doRouting(windowHash: String) {
    if (windowHash.startsWith("#detail/")) {
        val hash = windowHash.replace("#detail/", "")
        getCharacter(hash)?.let { character ->
            characterDetail(character.snapshots.last())
        } ?: displayCharacters()
    } else {
        displayCharacters()
    }
}

fun buildNav() {
    val nav = document.getElementById("nav")!!
    nav.append {
        table {
            tbody {
                tr {
                    td {
                        button {
                            id = "upload-button"
                            +"Upload"
                            onClickFunction = {
                                importMenu()
                            }
                        }
                    }
                    td {
                        button {
                            id = "export-button"
                            +"Export"
                            onClickFunction = {
                                downloadAdditionalInfo()
                            }
                        }
                    }
                    td {
                        button {
                            id = "clear-button"
                            +"Clear"
                            onClickFunction = {
                                if (window.confirm("This will delete all your uploaded characters. You'll need to re-upload them. Are you sure?")) {
                                    localStorage.clear()
                                    loadExample()
                                }
                            }
                        }
                    }
                }
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

fun clearSections() {
    el("character-cards-section").innerHTML = ""
    el("import-section").innerHTML = ""
    el("character-detail-section").innerHTML = ""
    el("nav").innerHTML = ""
}


fun el(id: String) = document.getElementById(id)!!