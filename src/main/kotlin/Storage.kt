import LocalForage.config
import kotlinx.browser.localStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import org.w3c.dom.set
import org.w3c.files.Blob
import org.w3c.files.FileReader
import pages.loadExample
import kotlin.js.Promise

@Serializable
data class InMemoryStorage(
    var profile: Profile = Profile("Unknown"),
    val characters: MutableMap<String, LegacyCharacter> = mutableMapOf(),
    val pictures: MutableMap<String, String> = mutableMapOf(),
    var additionalInfo: MutableMap<String, AdditionalInfo> = mutableMapOf(),
    var companies: Map<String, Company> = mapOf(),
    var storyProps: Map<String, String> = mapOf(),
    var dynamicProps: Map<String, String> = mapOf(),
    var aspectProps: Map<String, String> = mapOf(),
) {
    @Transient
    var companyByGameId = mapOf<String, Company>()

}

private var inMemoryStorage = InMemoryStorage()
var characterCards: Map<String, HTMLElement> = mapOf()

fun clearStorage(){
    inMemoryStorage = InMemoryStorage()
    characterCards = mapOf()
}

fun resetStorage() {
    inMemoryStorage = InMemoryStorage()
    characterCards = mapOf()
    loadExample(false).then {
        persistMemory()
        doRouting()
    }
}

fun getProfile(): Profile {
    return inMemoryStorage.profile
}

fun saveProfile(profile: Profile) {
    inMemoryStorage.profile = profile
}

fun getCharacters(): List<LegacyCharacter> {
    return inMemoryStorage.characters.values.toList()
}

fun getCharacter(uuid: String): LegacyCharacter? {
    return inMemoryStorage.characters[uuid]
}

fun saveCharacter(character: LegacyCharacter) {
    inMemoryStorage.characters[character.uuid] = character
}

fun getSnapshot(uuid: String): Character? {
    return inMemoryStorage.characters.values.firstNotNullOfOrNull { it.snapshots.firstOrNull { snapshot -> snapshot.uuid == uuid } }
}

fun getPicture(path: String): String? {
    return inMemoryStorage.pictures[path]
}

fun savePicture(path: String, blob: Blob): Promise<Unit> {
    return Promise { resolve, _ ->
        val fr = FileReader()
        fr.onload = { _ ->
            inMemoryStorage.pictures[path] = fr.result as String
            resolve(Unit)
        }
        fr.readAsDataURL(blob)
    }
}

fun getAdditionalInfo(): MutableMap<String, AdditionalInfo> {
    return inMemoryStorage.additionalInfo
}

fun saveAdditionalInfo(info: MutableMap<String, AdditionalInfo>) {
    inMemoryStorage.additionalInfo = info
    persistMemory()
}

fun saveAdditionalInfo(info: AdditionalInfo) {
    val allInfo = getAdditionalInfo()
    allInfo[info.uuid] = info
    saveAdditionalInfo(allInfo)
}

fun getCompany(uuid: String): Company {
    return inMemoryStorage.companies[uuid] ?: Company(uuid, "Unknown", 0.0, "Unknown")
}

fun getCompanies(): List<Company> {
    return inMemoryStorage.companies.values.toList()
}

fun getCompanyForGameId(uuid: String): Company {
    return inMemoryStorage.companyByGameId[uuid] ?: Company(uuid, "Unknown", 0.0, "Unknown")
}

fun saveCompanies(companies: Map<String, Company>) {
    inMemoryStorage.companies = companies.entries.sortedBy { it.value.date }.associate { it.key to it.value }
    inMemoryStorage.companyByGameId = companies.values.associateBy { it.gameId }
}

fun getStoryProp(id: String): String? {
    return inMemoryStorage.storyProps[id]
}

fun saveStoryProps(props: Map<String, String>) {
    inMemoryStorage.storyProps = props
}

fun getDynamicProp(id: String): String? {
    return inMemoryStorage.dynamicProps[id]
}

fun saveDynamicProps(props: Map<String, String>) {
    inMemoryStorage.dynamicProps = props
}

fun getAspectProp(id: String): String? {
    return inMemoryStorage.aspectProps[id]
}

fun saveAspectProps(props: Map<String, String>) {
    inMemoryStorage.aspectProps = props
}

fun createDB() {
    config(LocalForageConfig("wildermyth-legacy"))
}

fun persistMemory() {
    LocalForage.setItem("memory", jsonMapper.encodeToString(inMemoryStorage))
}


fun loadMemory(): Promise<*> {
    return LocalForage.getItem("memory").then { persisted ->
        if (persisted != null && persisted != undefined) {
            inMemoryStorage = jsonMapper.decodeFromString(persisted as String)
            inMemoryStorage.characters.values.forEach { legacyCharacter -> legacyCharacter.snapshots.forEach { it.reload() } }
            inMemoryStorage.companyByGameId = inMemoryStorage.companies.values.associateBy { it.gameId }
        }
    }
}

fun getDepth(): Int {
    return localStorage["relationship-depth"]?.toIntOrNull() ?: 2
}

fun saveDepth(depth: Int) {
    localStorage["relationship-depth"] = depth.toString()
}