import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.input
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onKeyUpFunction
import kotlinx.serialization.encodeToString
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import pages.*

val jsonMapper = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
lateinit var favicon: HTMLElement
var searchOptions = CharacterSearch()

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

fun clearSections() {
    el("character-cards-section").innerHTML = ""
    el("import-section").innerHTML = ""
    el("character-detail-section").innerHTML = ""
    el("nav").innerHTML = ""
}


fun el(id: String) = document.getElementById(id)!!