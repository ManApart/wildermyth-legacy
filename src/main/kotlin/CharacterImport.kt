import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.js.onChangeFunction
import kotlinx.serialization.decodeFromString
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.set
import org.w3c.files.Blob
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.js.Json
import kotlinx.serialization.encodeToString

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


fun parseFromJson(json: Json): Character {
    val entities = (json["entities"] as Array<Array<Json>>)[0]
    val base = entities[2]
    val uuid = entities[0]["value"] as String
    val name = base["name"] as String
    val aspects = parseAspects(base)
    val temporal = parseTemporal(base)
    val rawHistory = entities[12]["entries"] as Array<Json>
    val history = rawHistory.map { parseHistoryEntry(it) }

    return Character(uuid, name, determineClass(aspects), determineAge(temporal), aspects, temporal, history)
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

private fun determineClass(aspects: List<Aspect>): CharacterClass {
    val className = aspects.firstOrNull { it.name == "classLevel" }?.values?.firstOrNull()?.uppercase() ?: "WARRIOR"
    return CharacterClass.valueOf(className)
}

private fun determineAge(temporal: Map<String, Int>): Int {
    return temporal["AGE"] ?: 20
}

private fun parseHistoryEntry(base: Json): HistoryEntry {
    val raw: HistoryEntryRaw = jsonMapper.decodeFromString(JSON.stringify(base))
    return raw.toHistoryEntry()
}