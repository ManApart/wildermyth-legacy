import kotlinx.browser.*
import kotlinx.html.*
import kotlinx.html.dom.*

fun main() {
    displayExample()
}

fun displayExample() {
    val example = JSON.parse<Character>("{\"name\": \"Cob\"}")

    document.body!!.append.div {
        h1 {
            +example.name
        }
        img {
            src = "./example/default.png"
        }
        img {
            src = "./example/body.png"
        }

    }
}

/*
Upload character process
Export character in game
Upload Face
Upload Body
Upload data
Save character in local storage
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