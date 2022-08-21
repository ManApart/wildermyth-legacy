import kotlinx.browser.*
import kotlinx.html.*
import kotlinx.html.dom.*
import kotlinx.html.js.onChangeFunction
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.js.Json

fun main() {
    displayExample()
    importMenu()
}

private fun importMenu() {
    document.body!!.append.div {
        fileInput {
            id = "importInput"
            type = InputType.file
            onChangeFunction = {
                val element = document.getElementById(id) as HTMLInputElement
                if (element.files != undefined) {
                    val reader = FileReader()
                    reader.onload = {
                        importZip(reader.result as ArrayBuffer)
                    }
                    reader.onerror = { error ->
                        console.error("Failed to read File $error")
                    }
                    reader.readAsArrayBuffer(element.files!![0]!!)
                }
            }
        }
    }
}

//var jsZip = require('jszip')
//jsZip.loadAsync(file).then(function (zip) {
//    Object.keys(zip.files).forEach(function (filename) {
//        zip.files[filename].async('string').then(function (fileData) {
//            console.log(fileData) // These are your file contents
//        })
//    })
//})

fun importZip(data: ArrayBuffer) {
    JSZip().loadAsync(data).then { zip ->
        val entries = JsonObject.entries(zip.files)
        handleZipCharacterData(zip, entries)
        handleZipPictures(entries)
    }
}

private fun handleZipCharacterData(zipObject: JSZip.ZipObject, entries: List<Pair<Any, Any>>) {
    entries.filter { (fileName, _) ->
        val name = (fileName as String)
        name.endsWith("data.json")
    }.forEach { (fileName, file) ->
        val f2 = zipObject.file(fileName as String)
        println(JSON.stringify(f2))
        println("here")

        f2.async<String>("string").then { contents ->
            println(contents)
        }
    }
}

private fun handleZipPictures(entries: List<Pair<Any, Any>>) {
    entries.filter { (fileName, _) ->
        val name = (fileName as String)
        name.endsWith("default.png")
    }.forEach { (fileName, file) ->
        println(fileName)
    }
}

private fun displayExample() {
    val json = JSON.parse<Json>(defaultData)
    val example = parseFromJson(json)
//    println(JSON.stringify(example))

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