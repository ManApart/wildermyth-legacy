import kotlinx.browser.localStorage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.w3c.dom.get
import org.w3c.dom.set
import org.w3c.files.Blob
import org.w3c.files.FileReader
import kotlin.js.Promise

private val inMemoryStorage = mutableMapOf<String, String>()

fun getCharacterList(): MutableSet<String> {
    return inMemoryStorage["character-list"]?.split(",")?.toMutableSet() ?: mutableSetOf()
}

fun saveCharacterList(list: Set<String>) {
    inMemoryStorage["character-list"] = list.joinToString(",")
}

fun getCharacter(uuid: String): LegacyCharacter? {
    return inMemoryStorage[uuid]?.let { jsonMapper.decodeFromString(it) }
}

fun saveCharacter(character: LegacyCharacter) {
    inMemoryStorage[character.uuid] = jsonMapper.encodeToString(character)
}

fun getPicture(path: String): String? {
    return inMemoryStorage[path]
}

fun savePicture(path: String, blob: Blob): Promise<Unit> {
    return Promise { resolve, reject ->
        val fr = FileReader()
        fr.onload = { _ ->
            inMemoryStorage[path] = fr.result as String
            resolve(Unit)
        }
        fr.readAsDataURL(blob)
    }
}

fun getAdditionalInfo(): MutableMap<String, AdditionalInfo> {
    return localStorage["additional-info"]?.let { jsonMapper.decodeFromString(it) } ?: mutableMapOf()
}

fun saveAdditionalInfo(info: MutableMap<String, AdditionalInfo>) {
    localStorage["additional-info"] = jsonMapper.encodeToString(info)
}

fun getAdditionalInfo(uuid: String): AdditionalInfo {
    return localStorage["additional-info"]?.let { jsonMapper.decodeFromString<Map<String, AdditionalInfo>>(it)[uuid] } ?: AdditionalInfo(uuid)
}

fun saveAdditionalInfo(info: AdditionalInfo) {
    val allInfo = getAdditionalInfo()
    allInfo[info.uuid] = info
    saveAdditionalInfo(allInfo)
}

fun getCompany(uuid: String): Company {
    return localStorage["companies"]?.let { jsonMapper.decodeFromString<Map<String, Company>>(it)[uuid] } ?: Company(uuid, 0, "Unknown")
}

fun saveCompanies(companies: Map<String, Company>) {
    inMemoryStorage["companies"] = jsonMapper.encodeToString(companies)
}