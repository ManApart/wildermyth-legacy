import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import org.w3c.dom.Image
import org.w3c.dom.get
import org.w3c.dom.set
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.FileReader
import org.w3c.xhr.BLOB
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.js.Json

fun main() {
    window.onload = {
        loadExample()
        displayCharacters()
        importMenu()
    }
}

private fun loadExample() {
    val json = JSON.parse<Json>(defaultData)
    val example = parseFromJson(json)
//    println(JSON.stringify(example))
    localStorage[example.fileName] = JSON.stringify(example) { key, value ->
        when {
            key == "characterClass" && value is CharacterClass -> value.name
            else -> value
        }
    }
    val characters = getCharacterList()
    characters.add(example.fileName)
    saveCharacterList(characters)

    loadBlob("./example/body.png") {
        savePicture(example.fileName + "/body", it)
    }
    loadBlob("./example/default.png") {
        savePicture(example.fileName + "/head", it)
    }

}

private fun loadBlob(url: String, callBack: (Blob) -> Unit) {
    XMLHttpRequest().apply {
        open("GET", url)
        responseType = XMLHttpRequestResponseType.BLOB
        onerror = { println("Failed to get image") }
        onload = {
            callBack(response as Blob)
        }
        send()
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
    fr.onload = { e ->
        localStorage[path] = fr.result as String
        Unit
    }
    fr.readAsDataURL(blob)

}

fun getPicture(path: String): String {
    return localStorage[path] ?: ""
}

private fun displayCharacters() {
    val section = document.getElementById("character-cards-section")!!
    section.innerHTML = ""
    section.append {
        getCharacterList()
            .mapNotNull { localStorage[it] }
            .map { JSON.parse<Character>(it) }
            .forEach { character ->
                println("Building ${character.name}")
                println(JSON.stringify(character))
                div("character-card") {
                    h1 {
                        +character.name
                    }
                    div("character-portrait") {
                        img {
                            src = getPicture(character.fileName +"/body")
                            classes = setOf("character-body")
                        }
                        img {
                            src = getPicture(character.fileName +"/head")
                            classes = setOf("character-head")
                        }
                    }
                    div("character-summary") {
                        +"${character.age} year old bronzehorn ${character.characterClass}"
                    }
                    div("character-bio") {
                        +"Interested in killing gorgons and eating cheese."
                    }
                }
            }
    }
}
