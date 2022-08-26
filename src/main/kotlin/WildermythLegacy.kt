import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.html.dom.append
import kotlinx.html.id
import kotlinx.html.js.button
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.decodeFromString
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.get
import org.w3c.dom.set
import org.w3c.files.Blob
import org.w3c.files.FileReader
import pages.*
import kotlin.js.Promise

val jsonMapper = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

fun main() {
    window.onload = {
        doRouting()
    }
}

fun doRouting() {
    if (window.location.hash.isNotBlank()) {
        val hash = window.location.hash.replace("#", "")
        localStorage[hash]?.let { data ->
            val character = jsonMapper.decodeFromString<Character>(data)
            characterDetail(character)
        } ?: loadExample()
    } else {
        loadExample()
    }
}

fun buildNav() {
    val nav = document.getElementById("nav")!!
    nav.append {
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
        button {
            id = "upload-button"
            +"Upload"
            onClickFunction = {
                importMenu()
            }
        }
    }
}

fun clearSections() {
    document.getElementById("character-cards-section")!!.innerHTML = ""
    document.getElementById("import-section")!!.innerHTML = ""
    document.getElementById("character-detail-section")!!.innerHTML = ""
    document.getElementById("nav")!!.innerHTML = ""
}

fun getCharacterList(): MutableSet<String> {
    return localStorage["character-list"]?.split(",")?.toMutableSet() ?: mutableSetOf()
}

fun saveCharacterList(list: Set<String>) {
    localStorage["character-list"] = list.joinToString(",")
}

fun savePicture(path: String, blob: Blob): Promise<Unit> {
    return Promise { resolve, reject ->
        val fr = FileReader()
        fr.onload = { _ ->
            localStorage[path] = fr.result as String
            resolve(Unit)
        }
        fr.readAsDataURL(blob)
    }
}

fun getPicture(path: String): String {
    return localStorage[path] ?: ""
}
