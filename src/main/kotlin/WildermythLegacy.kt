import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.w3c.dom.get
import org.w3c.dom.set
import org.w3c.files.Blob
import org.w3c.files.FileReader
import pages.displayCharacters
import pages.importMenu
import pages.loadExample

val jsonMapper = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

fun main() {
    window.onload = {
        loadExample()
        displayCharacters()
        importMenu()
    }
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
