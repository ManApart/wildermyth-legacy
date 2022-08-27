package pages

import Aspect
import Character
import HistoryEntry
import HistoryEntryRaw
import JSZip
import JsonObject
import LegacyCharacter
import getCharacterList
import jsonMapper
import kotlinx.serialization.decodeFromString
import org.khronos.webgl.ArrayBuffer
import org.w3c.files.Blob
import kotlin.js.Json
import saveCharacter
import saveCharacterList
import savePicture
import kotlin.js.Promise

fun importZip(data: ArrayBuffer, refreshCharacters: Boolean = true) {
    JSZip().loadAsync(data).then { zip ->
        val keys = JsonObject.keys(zip.files)
        handleZipCharacterData(zip, keys, refreshCharacters)
    }
}

private fun handleZipCharacterData(zip: JSZip.ZipObject, keys: List<String>, refreshCharacters: Boolean) {
    val legacyJson = keys.first { fileName ->
        fileName.endsWith("legacy.json")
    }
    zip.file(legacyJson)!!.async<String>("string")
        .then { contents ->
            val json = JSON.parse<Json>(contents)
            val characters = parseLegacy(json)
            characters.forEach { saveCharacter(it) }
            val characterList = getCharacterList()
            characterList.addAll(characters.map { it.uuid })
            saveCharacterList(characterList)
            Promise.all(characters.map { handleZipPictures(zip, it) }.toTypedArray())
        }.then {
            if (refreshCharacters) {
                displayCharacters()
            }
        }
}

private fun handleZipPictures(zip: JSZip.ZipObject, character: Character): Promise<*> {
    return Promise.all(
        listOfNotNull(
            handleSinglePicture(zip, character, "default", "head"),
            handleSinglePicture(zip, character, "body", "body"),
        ).toTypedArray()
    )

}

private fun handleSinglePicture(zip: JSZip.ZipObject, character: Character, zipName: String, saveName: String): Promise<*>? {
    val filePath = "${character.name}/$zipName.png"
    val file = zip.file(filePath)
    return if (file != null && file != undefined) {
        file.async<Blob>("Blob").then { contents ->
            savePicture("${character.uuid}/$saveName", contents)
        }
    } else null

}

fun parseLegacy(json: Json): List<Character> {
    val player = json["playerName"] as String
    println("Parsing $player's legacy")
    return (json["entries"] as Array<Json>)
        .map { parseLegacyCharacter(it) }
        .map { it.snapshots.first() }
}

fun parseLegacyCharacter(json: Json): LegacyCharacter {
    val uuid = (json["id"] as Json)["value"] as String
    val snapshots = (json["snapshots"] as Array<Json>).map { parseCharacter(uuid, it) }
    return LegacyCharacter(uuid, snapshots)
}

fun parseCharacter(uuid: String, json: Json): Character {
    val allEntities = (json["entities"] as Array<Array<Json>>)
    val characterEntities = allEntities.first { option ->
        option[0]["value"] == uuid
    }
    val base = characterEntities[2]
//    println(JSON.stringify(base))
    val name = base["name"] as String
    val aspects = parseAspects(base)
//    val legacyAspects = parseLegacyAspects(characterEntities[12]["legacyAspects"] as Json?)
    val legacyAspects = listOf<Aspect>()
    val temporal = parseTemporal(base)
//    val rawHistory = characterEntities[12]["entries"] as Array<Json>
//    val history = rawHistory.map { parseHistoryEntry(it) }
    val history = listOf<HistoryEntry>()

    return Character(uuid, name, aspects, legacyAspects, temporal, history)
}

fun parseFromJson(json: Json): Character {
    val entities = (json["entities"] as Array<Array<Json>>)[0]
    val base = entities[2]
    val uuid = entities[0]["value"] as String
    val name = base["name"] as String
    val aspects = parseAspects(base)
    val legacyAspects = parseLegacyAspects(entities[12]["legacyAspects"] as Json?)
    val temporal = parseTemporal(base)
    val rawHistory = entities[12]["entries"] as Array<Json>
    val history = rawHistory.map { parseHistoryEntry(it) }

    return Character(uuid, name, aspects, legacyAspects, temporal, history)
}

private fun parseAspects(base: Json): List<Aspect> {
    val aspectJson = (base["aspects"] as Json)["entries"] as Array<Array<Any>>
    val stringAspects = aspectJson.flatten().filterIsInstance<String>()
    return stringAspects.map { it.toAspect() }
}

private fun parseLegacyAspects(optionalBase: Json?): List<Aspect> {
    return optionalBase?.let { base ->
        val aspectJson = base["entries"] as Array<Array<Any>>
        val stringAspects = aspectJson.flatten().filterIsInstance<String>()
        stringAspects.map { it.toAspect() }
    } ?: listOf()
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