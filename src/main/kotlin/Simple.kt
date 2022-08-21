import kotlinx.browser.document
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.h1
import kotlinx.html.img
import kotlin.js.Json

fun main() {
    displayExample()
    importMenu()
}

private fun displayExample() {
    val json = JSON.parse<Json>(defaultData)
    val example = parseFromJson(json)
//    println(JSON.stringify(example))

    document.body!!.append.div {
        h1 {
            +example.name
        }
        div {
            classes = setOf("portrait")
            img {
                src = "./example/body.png"
                classes = setOf("character-body")
            }
            img {
                src = "./example/default.png"
                classes = setOf("character-head")
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