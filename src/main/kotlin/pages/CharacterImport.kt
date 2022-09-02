package pages

import AdditionalInfo
import Aspect
import Character
import Company
import HistoryEntry
import HistoryEntryRaw
import JSZip
import JsonObject
import LegacyCharacter
import doRouting
import getCharacterList
import jsonMapper
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonNull.content
import org.khronos.webgl.ArrayBuffer
import org.w3c.files.Blob
import persistMemory
import saveAdditionalInfo
import saveCompanies
import kotlin.js.Json
import saveCharacter
import saveCharacterList
import savePicture
import saveStoryProps
import kotlin.js.Promise

private val companies = mutableMapOf<String, Company>()

fun importZip(data: ArrayBuffer, originalHash: String) {
    JSZip().loadAsync(data).then { zip ->
        val keys = JsonObject.keys(zip.files)
        handleAdditionalInfo(zip)
        handleStoryProps(zip)
        handleZipCharacterData(zip, keys, originalHash)
    }
}

private fun handleAdditionalInfo(zip: JSZip.ZipObject) {
    zip.file("AdditionalInfo.json")?.async<String>("string")?.then { content ->
        saveAdditionalInfo(jsonMapper.decodeFromString<MutableMap<String, AdditionalInfo>>(content))
    }
}

private fun handleStoryProps(zip: JSZip.ZipObject) {
    zip.file("story.properties")?.async<String>("string")?.then { content ->
        val props = content.split("\n").mapNotNull {
            val parts = it.split("=")
            val key = parts.firstOrNull()
            val prop = parts.getOrNull(1)
            if (key != null && prop != null) key to prop else null
        }.toMap()
        saveStoryProps(props)
    }
}

private fun handleZipCharacterData(zip: JSZip.ZipObject, keys: List<String>, originalHash: String) {
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
            Promise.all(characters.map { handleZipPictures(zip, it.snapshots.last()) }.toTypedArray())
        }.then {
            doRouting(originalHash)
            persistMemory()
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

fun parseLegacy(json: Json): List<LegacyCharacter> {
    val player = json["playerName"] as String
    println("Parsing $player's legacy")
    return (json["entries"] as Array<Json>)
        .map { parseLegacyCharacter(it) }
        .also {
            saveCompanies(companies)
        }
}

fun parseLegacyCharacter(json: Json): LegacyCharacter {
    val uuid = (json["id"] as Json)["value"] as String
    val snapshots = (json["snapshots"] as Array<Json>).map { parseCharacter(uuid, it) }.toTypedArray()
    val companyIds = (json["legacyCompanyInfo"] as Array<Json>).map { companyJson ->
        ((companyJson["companyId"] as Json)["value"] as String).also { companyId ->
            if (!companies.containsKey(companyId)) {
                val name = companyJson["companyName"] as String
                val date = companyJson["date"] as Double
                companies[companyId] = Company(companyId, date, name)
            }
            companies[companyId]?.characters?.add(uuid)
        }
    }
    return LegacyCharacter(uuid, snapshots, companyIds)
}

fun parseCharacter(uuid: String, json: Json): Character {
    val allEntities = (json["entities"] as Array<Array<Json>>)
    val characterEntities = allEntities.first { option ->
        option[0]["value"] == uuid
    }
    val base = characterEntities[2]
    val name = base["name"] as String
    val aspects = parseAspects(base)
    val temporal = parseTemporal(base)
    val historyNode = (characterEntities.first { it["legacyAchievementInfo"] != null })
    val rawHistory = historyNode["entries"] as Array<Json>
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