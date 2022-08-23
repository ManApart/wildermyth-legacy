import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onChangeFunction
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.set
import org.w3c.files.Blob
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.js.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json as jsonMapper

fun importMenu() {
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

fun importZip(data: ArrayBuffer) {
    JSZip().loadAsync(data).then { zip ->
        val keys = JsonObject.keys(zip.files)
        handleZipCharacterData(zip, keys)
    }
}

private fun handleZipCharacterData(zip: JSZip.ZipObject, keys: List<String>) {
    val characters = getCharacterList()
    keys.filter { fileName ->
        fileName.endsWith("data.json")
    }.forEach { fileName ->
        zip.file(fileName).async<String>("string").then { contents ->
            val json = JSON.parse<Json>(contents)
            val character = parseFromJson(json)
            println(character.uuid)
            localStorage[character.uuid] = jsonMapper.encodeToString(character)
            characters.add(character.uuid)
            saveCharacterList(characters)
            handleZipPictures(zip, character)
        }
    }
}

private fun handleZipPictures(zip: JSZip.ZipObject, character: Character) {
    val headPath = "${character.name}/default.png"
    zip.file(headPath).async<Blob>("Blob").then { contents ->
        savePicture("${character.uuid}/head", contents)
    }

    val bodyPath = "${character.name}/body.png"
    zip.file(bodyPath).async<Blob>("Blob").then { contents ->
        savePicture("${character.uuid}/body", contents)
    }
}