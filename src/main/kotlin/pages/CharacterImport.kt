package pages

import AdditionalInfo
import Aspect
import Character
import Company
import Gear
import GearRaw
import HistoryEntry
import HistoryEntryRaw
import JSZip
import JsonObject
import LegacyCharacter
import doRouting
import getCharacters
import getCroppedHead
import jsonMapper
import kotlinx.serialization.decodeFromString
import org.khronos.webgl.ArrayBuffer
import org.w3c.files.Blob
import persistMemory
import saveAdditionalInfo
import saveCompanies
import kotlin.js.Json
import saveCharacter
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
    val snapshots = (json["snapshots"] as Array<Json>).mapNotNull { parseCharacter(uuid, it) }.toTypedArray()
    val companyIds = parseCompanies(json, uuid)
    val isNPC = (json["usage"] as String? == "background")

    val killCount = parseKillCount(json)
    return LegacyCharacter(uuid, snapshots, companyIds, isNPC, killCount)
}

fun parseCharacter(uuid: String, json: Json): Character? {
    val allEntities = (json["entities"] as Array<Array<Json>>)
    val characterEntities = allEntities.firstOrNull { it[0]["value"] == uuid }
        ?: return null.also { println("No character entities found for $uuid") }
    val base = characterEntities.firstOrNull { it["name"] != null }
        ?: return null.also { println("No character base found for $uuid") }
    val name = base["name"] as String
    val aspects = parseAspects(base)
    val temporal = parseTemporal(base)
    val historyNode = characterEntities.firstOrNull { it["legacyAchievementInfo"] != null || it["legacyCompanyInfo"] != null }
    val rawHistory = historyNode?.let { it["entries"] as Array<Json> }
        ?: arrayOf<Json>().also { println("No history for $name: $uuid") }
    val history = rawHistory.map { parseHistoryEntry(it) }
    val gear = parseGear(allEntities)

    return Character(uuid, name, aspects, temporal, history, gear)
}

private fun parseCompanies(json: Json, uuid: String): List<String> {
    val companyIds = (json["legacyCompanyInfo"] as Array<Json>).map { companyJson ->
        ((companyJson["companyId"] as Json)["value"] as String).also { companyId ->
            if (!companies.containsKey(companyId)) {
                val name = companyJson["companyName"] as String
                val date = companyJson["date"] as Double
                val mainThreat = companyJson["mainThreat"] as String
                companies[companyId] = Company(companyId, date, name, mainThreat)
            }
            companies[companyId]?.characters?.add(uuid)
        }
    }
    return companyIds
}

private fun parseKillCount(json: Json): Int {
    val achievementInfo = json["legacyAchievementInfo"] as Json?
    val achievementEntries = achievementInfo?.let { (it["entries"] as Array<Array<Json>>).flatten() }
    val killCounter = achievementEntries?.firstOrNull { it["entryId"] == "killCounter" }
    return killCounter?.let { (it["value"] as Double?)?.toInt() } ?: 0
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

private fun parseGear(entities: Array<Array<Json>>): List<Gear> {
    return entities.filter { outerArray -> outerArray.any { it["itemId"] != null } }.map { parseGearItem(it) }
}

fun parseGearItem(entity: Array<Json>): Gear {
    val uuid = entity[0]["value"] as String
    val name = entity.firstNotNullOfOrNull { it["name"] as String? } ?: "Unknown"
    val rawJson = entity.first { it["itemId"] != null }
    return jsonMapper.decodeFromString<GearRaw>(JSON.stringify(rawJson)).toGear(uuid, name)
}
