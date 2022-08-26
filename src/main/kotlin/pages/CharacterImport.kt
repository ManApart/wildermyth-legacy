package pages

import Aspect
import Character
import HistoryEntry
import HistoryEntryRaw
import JSZip
import JsonObject
import getCharacterList
import jsonMapper
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.input
import kotlinx.html.js.onChangeFunction
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.decodeFromString
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.set
import org.w3c.files.Blob
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.js.Json
import kotlinx.serialization.encodeToString
import org.w3c.dom.HTMLButtonElement
import saveCharacterList
import savePicture
import kotlin.js.Promise

/*
Make import process a promise so we don't reload everything constantly
 */

fun importButton() {
    val button = document.getElementById("upload-button") as HTMLButtonElement
    button.onclick = {
        importMenu()
    }

}

fun importMenu() {
    val section = document.getElementById("import-section")!!
    section.innerHTML = ""
    document.getElementById("character-detail-section")!!.innerHTML = ""
    section.append {
        div {
            id = "upload-instructions"
            p { +"View your own characters from Wildermyth!" }
            ol {
                li { +"In game, go to My Legacy -> Heroes -> Hero List." }
                li {
                    +"For each hero you want to view."
                    ul {
                        li { +"Click their picture to view their character sheet." }
                        li { +"Click 'Customize' (or press 7)" }
                        li { +"Click 'Export Character'" }
                        li { +"This should open a new window, which you can ignore until you've exported each character." }
                    }
                }
                li { +"Navigate to your '<Game Install>/out' folder." }
                ul {
                    li { +"If you just exported characters, it should be one folder up from the character export." }
                    li { +"Otherwise it should be 'steamapps/common/Wildermyth/out' or the GOG equivalent." }
                }
                li { +"Select all the character folders and create a zip file." }
                ul {
                    li { a("https://www.7-zip.org/", target = "_blank") { +"7Zip is a good tool for this." } }
//                    li { +"${a("https://www.7-zip.org/", target = "_blank") { +"7Zip" }} is a good tool for this." }
                }
                li { +"Upload the zip using the button below. Your characters should all load and be locally saved!" }
            }
        }
        button {
            +"Cancel"
            onClickFunction = { section.innerHTML = "" }
        }
        input(InputType.file) {
            id = "import-input"
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
    val promises = keys.filter { fileName ->
        fileName.endsWith("data.json")
    }.map { fileName ->
        zip.file(fileName).async<String>("string").then { contents ->
            val json = JSON.parse<Json>(contents)
            val character = parseFromJson(json)
            println(character.uuid)
            localStorage[character.uuid] = jsonMapper.encodeToString(character)
            characters.add(character.uuid)
            saveCharacterList(characters)
            Promise.all(handleZipPictures(zip, character))
        }
    }.toTypedArray()
    Promise.all(promises).then {
        displayCharacters()
    }
}

private fun handleZipPictures(zip: JSZip.ZipObject, character: Character): Array<Promise<*>> {
    val headPath = "${character.name}/default.png"
    val bodyPath = "${character.name}/body.png"

    return arrayOf(
        zip.file(headPath).async<Blob>("Blob").then { contents ->
            savePicture("${character.uuid}/head", contents)
        },
        zip.file(bodyPath).async<Blob>("Blob").then { contents ->
            savePicture("${character.uuid}/body", contents)
        })

}

fun parseFromJson(json: Json): Character {
    val entities = (json["entities"] as Array<Array<Json>>)[0]
    val base = entities[2]
    val uuid = entities[0]["value"] as String
    val name = base["name"] as String
    val aspects = parseAspects(base)
    val temporal = parseTemporal(base)
    val rawHistory = entities[12]["entries"] as Array<Json>
    val history = rawHistory.map { parseHistoryEntry(it) }

    return Character(uuid, name, aspects, temporal, history)
}

private fun parseAspects(base: Json): List<Aspect> {
    val aspectJson = (base["aspects"] as Json)["entries"] as Array<Array<Any>>
    val stringAspects = aspectJson.flatten().filterIsInstance<String>()
    return stringAspects.map { it.toAspect() }
}

private fun parseTemporal(base: Json): Map<String, Int> {
    val temporalJson = (base["temporal"] as Json)["entries"] as Array<Array<Any>>
    return temporalJson.associate { values -> values.first() as String to values.last() as Int }
}

fun String.toAspect(): Aspect {
    return if (contains("|")) {
        val parts = this.split("|")
        Aspect(parts.first(), parts.subList(1, parts.size))
    } else Aspect(this)
}

private fun parseHistoryEntry(base: Json): HistoryEntry {
    val raw: HistoryEntryRaw = jsonMapper.decodeFromString(JSON.stringify(base))
    return raw.toHistoryEntry()
}