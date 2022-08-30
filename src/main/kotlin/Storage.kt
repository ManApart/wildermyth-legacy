import kotlinx.browser.localStorage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.w3c.dom.get
import org.w3c.dom.set
import org.w3c.files.Blob
import org.w3c.files.FileReader
import kotlin.js.Promise

data class InMemoryStorage(
    var characterList: Set<String> = setOf(),
    val characters: MutableMap<String, LegacyCharacter> = mutableMapOf(),
    val pictures: MutableMap<String, String> = mutableMapOf(),
    val additionalInfo: MutableMap<String, AdditionalInfo> = mutableMapOf(),
    var companies: Map<String, Company> = mapOf(),
    var storyProps: Map<String, String> = mapOf(),
)

private val inMemoryStorage = InMemoryStorage()


fun getCharacterList(): MutableSet<String> {
    return inMemoryStorage.characterList.toMutableSet()
}

fun saveCharacterList(characters: Set<String>) {
    inMemoryStorage.characterList = characters.toSet()
}

fun getCharacter(uuid: String): LegacyCharacter? {
    return inMemoryStorage.characters[uuid]
}

fun saveCharacter(character: LegacyCharacter) {
    inMemoryStorage.characters[character.uuid] = character
}

fun getPicture(path: String): String? {
    return inMemoryStorage.pictures[path]
}

fun savePicture(path: String, blob: Blob): Promise<Unit> {
    return Promise { resolve, reject ->
        val fr = FileReader()
        fr.onload = { _ ->
            inMemoryStorage.pictures[path] = fr.result as String
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
    return inMemoryStorage.companies[uuid] ?: Company(uuid, 0.0, "Unknown")
}

fun saveCompanies(companies: Map<String, Company>) {
    inMemoryStorage.companies = companies.toMap()
}

fun getStoryProp(id: String): String? {
    return inMemoryStorage.storyProps[id]
}

fun saveStoryProps(props: Map<String, String>) {
    inMemoryStorage.storyProps = props
}