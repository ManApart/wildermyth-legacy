package pages

import AdditionalInfo
import Aspect
import Character
import Company
import Gear
import GearRaw
import HistoryEntry
import HistoryEntryRaw
import InMemoryStorage
import JSZip
import JsonObject
import LegacyCharacter
import Profile
import Unlock
import characterCards
import clearSections
import clearStorage
import doRouting
import el
import jsonMapper
import kotlinx.html.dom.append
import kotlinx.html.id
import kotlinx.html.js.div
import kotlinx.html.js.p
import kotlinx.serialization.decodeFromString
import legacyTierLevelFromInt
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLParagraphElement
import org.w3c.files.Blob
import persistMemory
import saveAdditionalInfo
import saveAspectProps
import saveCompanies
import kotlin.js.Json
import saveCharacter
import saveDynamicProps
import savePicture
import saveProfile
import saveStoryProps
import splitByCapitals
import kotlin.js.Promise
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json.Default.encodeToString

private val companies = mutableMapOf<String, Company>()

fun importZip(data: ArrayBuffer, originalHash: String) {
    val status = initialLoading()
    clearStorage()

    JSZip().loadAsync(data).then { zip ->
        status.updateStatus("Loaded Zip")
        val keys = JsonObject.keys(zip.files)
        handleAdditionalInfo(zip, status)
        handlePropertyFiles(zip, status)
        handleZipCharacterData(zip, keys, status, originalHash)
    }
}

private fun initialLoading(): HTMLParagraphElement {
    val importSection = el("import-section")
    val characterListSection = el("character-cards-section")

    importSection.innerHTML = ""
    characterListSection.innerHTML = ""
    importSection.append {
        div {
            p {
                id = "loading-status"
                +"Starting Import"
            }
        }
    }
    return el<HTMLParagraphElement>("loading-status")
}

private fun HTMLParagraphElement.updateStatus(status: String) {
    innerText = "Loading: $status"
}


private fun handleAdditionalInfo(zip: JSZip.ZipObject, status: HTMLParagraphElement) {
    zip.file("AdditionalInfo.json")?.async<String>("string")?.then { content ->
        try {
            saveAdditionalInfo(jsonMapper.decodeFromString<MutableMap<String, AdditionalInfo>>(content))
            status.updateStatus("Parsed Additional Info")
        } catch (e: Exception) {
            status.updateStatus("Unable to load Additional Info")
        }
    }
}

private fun handlePropertyFiles(zip: JSZip.ZipObject, status: HTMLParagraphElement) {
    try {
        handlePropsFile(zip, "story.properties", ::saveStoryProps, status)
        handlePropsFile(zip, "dynamic.properties", ::saveDynamicProps, status)
        handlePropsFile(zip, "aspects.properties", ::saveAspectProps, status)
    } catch (e: Exception) {
        status.updateStatus("Unable to load Props Files")
    }
}

private fun handlePropsFile(zip: JSZip.ZipObject, fileName: String, save: (Map<String, String>) -> Unit, status: HTMLParagraphElement) {
    zip.file(fileName)?.async<String>("string")?.then { content ->
        val props = content.split("\n").mapNotNull {
            val parts = it.split("=")
            val key = parts.firstOrNull()
            val prop = parts.getOrNull(1)
            if (key != null && prop != null) key to prop else null
        }.toMap()
        save(props)
        status.updateStatus("Saved $fileName")
    }
}

private fun handleZipCharacterData(zip: JSZip.ZipObject, keys: List<String>, status: HTMLParagraphElement, originalHash: String) {
    val legacyJson = keys.first { fileName ->
        fileName.endsWith("legacy.json")
    }
    var handled = 0
    zip.file(legacyJson)!!.async<String>("string")
        .then { contents ->
            try {
                val json = JSON.parse<Json>(contents.replace(",Infinity", ",0"))
                val profile = parseProfile(json)
                saveProfile(profile)
                status.updateStatus("Saved Profile")

                val characters = parseLegacy(json, status)
                characters.forEach { saveCharacter(it) }
                status.updateStatus("Saved All Characters")
                Promise.all(characters.map {
                    handleZipPictures(zip, it.snapshots.last()).then {
                        handled++
                        status.updateStatus("Parsed $handled pictures")
                    }
                }.toTypedArray())
            } catch (e: Throwable) {
                status.updateStatus("Unable to Parse Legacy File!")
                throw e
            }
        }.then {
            status.updateStatus("Parsed All Characters")
            characterCards = mapOf()
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

fun parseLegacy(json: Json, status: HTMLParagraphElement): List<LegacyCharacter> {
    var parsedCount = 0
    var totalCount = 0
    return (json["entries"] as Array<Json>)
        .mapNotNull {
            parseLegacyCharacter(it).also { character ->
                if (character != null) parsedCount++
                totalCount++
                status.updateStatus("Parsed $parsedCount/$totalCount Characters")
            }
        }
        .also {
            saveCompanies(companies)
        }
}

fun parseLegacyCharacter(json: Json): LegacyCharacter? {
    val uuid = (json["id"] as Json)["value"] as String
    return try {
        val snapshots = (json["snapshots"] as Array<Json>).mapNotNull { parseCharacter(uuid, it) }.toTypedArray()
        val companyIds = parseCompanies(json, uuid)
        val isNPC = (json["usage"] as String? == "background")
        val tier = legacyTierLevelFromInt(json["tier"] as Int? ?: 0)

        val killCount = parseKillCount(json)
        LegacyCharacter(uuid, snapshots, companyIds, isNPC, tier, killCount, JSON.stringify(json))
    } catch (e: Exception) {
        println("Failed to parse character $uuid! ${e.message}")
        null
    }
}

fun parseCharacter(uuid: String, json: Json): Character? {
    val gameId = (json["gameId"] as Json)["value"] as String
    val date = (json["date"] as Number).toLong()
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

    return Character(uuid, gameId, date, name, aspects, temporal, history, gear)
}

private fun parseCompanies(json: Json, uuid: String): Set<String> {
    val companyIds = (json["legacyCompanyInfo"] as Array<Json>).map { companyJson ->
        ((companyJson["companyId"] as Json)["value"] as String).also { companyId ->
            if (!companies.containsKey(companyId)) {
                try {
                    val name = companyJson["companyName"] as String
                    val date = companyJson["date"] as Double
                    val mainThreat = companyJson["mainThreat"] as String
                    val gameId = ((companyJson["gameId"] as Json?)?.get("value") as String?) ?: "NA"
                    companies[companyId] = Company(companyId, gameId, date, name, mainThreat)
                } catch (e: Exception) {
                    println("Failed to parse company $companyId")
                    println(JSON.stringify(companyJson))
                }
            }
            companies[companyId]?.characters?.add(uuid)
        }
    }
    return companyIds.toSet()
}

private fun parseKillCount(json: Json): Int {
    val achievementInfo = json["legacyAchievementInfo"] as Json?
    val achievementEntries = achievementInfo?.let { (it["entries"] as Array<Array<Json>>).flatten() }
    val killCounter = achievementEntries?.firstOrNull { it["entryId"] == "killCounter" }
    return killCounter?.let { (it["value"] as Double?)?.toInt() } ?: 0
}

private fun parseAspects(base: Json): List<Aspect> {
    val aspectJson = (base["aspects"] as Json)["entries"] as Array<Array<Any>>
    val stringAspects = aspectJson.map { it.last() as Json }.filter { it["aspect"] != null }
    return stringAspects.map { it.toAspect() }
}

private fun parseTemporal(base: Json): Map<String, Int> {
    val temporalJson = (base["temporal"] as Json)["entries"] as Array<Array<Any>>
    return temporalJson.associate { values -> values.first() as String to values.last() as Int }
}

fun Json.toAspect(): Aspect {
    val id = (this["aspect"] as String?) ?: ""
    val value = listOfNotNull((this["value"] as Int?)?.toString())
    return when {
        id.contains("|") -> {
            val parts = id.split("|")
            Aspect(parts.first(), parts.subList(1, parts.size) + value)
        }

        id.contains(":") -> {
            val parts = id.split(":")
            Aspect(parts.first(), parts.subList(1, parts.size) + value)
        }

        else -> Aspect(id, value)
    }
}

fun String.toAspect(): Aspect {
    return when {
        contains("|") -> {
            val parts = this.split("|")
            Aspect(parts.first(), parts.subList(1, parts.size))
        }

        contains(":") -> {
            val parts = this.split(":")
            Aspect(parts.first(), parts.subList(1, parts.size))
        }

        else -> Aspect(this)
    }
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

fun parseProfile(json: Json): Profile {
    val player = json["playerName"] as String
    println("Parsing $player's legacy")
    val entries = ((json["unlocks"] as Json)["entries"] as Array<Array<Json>>).map { it.last() }

    val unlocks = entries.map {
        val id = (it["aspectId"] as String)
        val name = id.replace("achievementProgress_", "")
            .replace("achievement_", "")
            .replace("legacy_", "")
            .replace("_", " ")
            .replace("|", " ")
            .capitalize()
            .splitByCapitals()
        val count = (it["value"] as Int?) ?: 0
        Unlock(id, name, count)
    }

    return Profile(player, unlocks)
}