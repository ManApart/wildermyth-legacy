import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.get
import org.w3c.dom.set
import org.w3c.files.Blob
import org.w3c.files.FileReader
import pages.displayCharacters
import pages.importButton
import pages.loadExample

val jsonMapper = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

fun main() {
    window.onload = {
        clearButton()
        importButton()
        loadExample()
        displayCharacters()
    }
}

fun clearButton() {
    val button = document.getElementById("clear-button") as HTMLButtonElement
    button.onclick = {
        println("clicked")
        if (window.confirm("This will delete all your uploaded characters. You'll need to re-upload them. Are you sure?")) {
            localStorage.clear()
            loadExample()
            displayCharacters()
        }
    }
}

fun clearSections() {
    document.getElementById("character-cards-section")!!.innerHTML = ""
    document.getElementById("import-section")!!.innerHTML = ""
    document.getElementById("character-detail-section")!!.innerHTML = ""
}

fun getCharacterList(): MutableSet<String> {
    return localStorage["character-list"]?.split(",")?.toMutableSet() ?: mutableSetOf()
}

fun saveCharacterList(list: Set<String>) {
    println("Saving $list")
    localStorage["character-list"] = list.joinToString(",")
}

fun savePicture(path: String, blob: Blob) {
    val fr = FileReader()
    fr.onload = { _ ->
        localStorage[path] = fr.result as String
        displayCharacters()
    }
    fr.readAsDataURL(blob)
}

fun getPicture(path: String): String {
    return localStorage[path] ?: ""
}
