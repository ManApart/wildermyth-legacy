import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.decodeFromString
import org.w3c.dom.get
import org.w3c.dom.set
import org.w3c.files.Blob
import org.w3c.files.FileReader

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
                with(character) {
                    println("Building ${character.name}")
                    val className = characterClass.name.lowercase()
                    div("character-card") {
                        h1 {
                            +name
                        }
                        div("character-portrait ${className}-portrait") {
                            img {
                                src = getPicture("$uuid/body")
                                classes = setOf("character-body", "${className}-body")
                            }
                            img {
                                src = getPicture("$uuid/head")
                                classes = setOf("character-head", "${className}-head")
                            }
                        }
                        div("character-summary") {
                            +"${character.age} year old bronzehorn ${className.capitalize()}"
                        }
                        div("character-bio") {
                            +character.getBio()
                        }
                    }
                }
            }
    }
}
