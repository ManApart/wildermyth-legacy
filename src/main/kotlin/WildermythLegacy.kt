import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.h1
import kotlinx.html.img
import org.w3c.dom.get
import org.w3c.dom.set
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
    println("[${example.fileName}]")
    localStorage[example.fileName] = JSON.stringify(example)
    val characters = getCharacterList()
    characters.add(example.fileName)
    saveCharacterList(characters)
    println(getCharacterList())
}

private fun getCharacterList(): MutableSet<String> {
    return localStorage["character-list"]?.split(",")?.toMutableSet() ?: mutableSetOf()
}

private fun saveCharacterList(list: Set<String>) {
    localStorage["character-list"] = list.joinToString()
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
                            src = "./example/body.png"
                            classes = setOf("character-body")
                        }
                        img {
                            src = "./example/default.png"
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