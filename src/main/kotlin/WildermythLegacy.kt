import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.h1
import kotlinx.html.img
import kotlinx.serialization.decodeFromString
import org.w3c.dom.get
import org.w3c.dom.set
import org.w3c.files.Blob
import org.w3c.files.FileReader
import kotlinx.serialization.json.Json as jsonMapper

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
    displayCharacters()
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

fun displayCharacters() {
    val section = document.getElementById("character-cards-section")!!
    section.innerHTML = ""
    section.append {
        getCharacterList()
            .mapNotNull { localStorage[it] }
            .map { jsonMapper.decodeFromString<Character>(it) }
            .forEach { character ->
                println("Building ${character.name}")
//                println(JSON.stringify(character))
                div("character-card") {
                    h1 {
                        +character.name
                    }
                    div("character-portrait") {
                        img {
                            src = getPicture(character.uuid +"/body")
                            classes = setOf("character-body")
                        }
                        img {
                            src = getPicture(character.uuid +"/head")
                            classes = setOf("character-head")
                        }
                    }
                    div("character-summary") {
                        +"${character.age} year old bronzehorn ${character.characterClass.name.lowercase()}"
                    }
                    div("character-bio") {
                        +"Interested in killing gorgons and eating cheese."
                    }
                }
            }
    }
}
