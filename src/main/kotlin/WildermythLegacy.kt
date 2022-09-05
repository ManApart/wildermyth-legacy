import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onKeyUpFunction
import kotlinx.serialization.encodeToString
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import pages.*

val jsonMapper = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
lateinit var favicon: HTMLElement

fun main() {
    window.onload = {
        favicon = document.getElementById("favicon") as HTMLElement
        createDB()
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
            characterDetail(character)
        } ?: displayCharacters()
    } else {
        displayCharacters()
    }
}

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
                val text = (document.getElementById("search") as HTMLInputElement).value
                characterSearch(text)
            }
        }
        button {
            id = "export-button"
            +"Export"
            onClickFunction = { downloadAdditionalInfo() }
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