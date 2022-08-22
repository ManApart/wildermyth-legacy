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
    localStorage[example.fileName] = JSON.stringify(example)
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

private fun getCharacterList(): MutableSet<String> {
    return localStorage["character-list"]?.split(",")?.toMutableSet() ?: mutableSetOf()
}

private fun saveCharacterList(list: Set<String>) {
    localStorage["character-list"] = list.joinToString()
}

private fun savePicture(path: String, blob: Blob) {
    val fr = FileReader()
    fr.onload = { e ->
        localStorage[path] = fr.result as String
        Unit
    }
    fr.readAsDataURL(blob)

}

private fun getPicture(path: String): String {
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
                        +"33 year old bronzehorn warrior"
                    }
                    div("character-bio") {
                        +"Interested in killing gorgons and eating cheese."
                    }
                }
            }
    }
}

/*
Upload character process
Export character in game
Upload Face
Upload Body
Upload data
Save parsed character in local storage
Export thin legacy data?
 */

/*
 var hero;

  if ( localStorage.getItem('heroImg')) {
    hero = localStorage.getItem('heroImg');
  }
  else {
    hero = '/9j/4AAQSkZJRgABAgAAZABkAAD/7    /.../    6p+3dIR//9k=';
    localStorage.setItem('heroImg',hero);
  }

  document.getElementById("hero-graphic").src='data:image/png;base64,' + hero;
 */