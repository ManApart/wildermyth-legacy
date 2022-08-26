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

fun getCharacter(uuid: String): Character? {
    return inMemoryStorage[uuid]?.let { jsonMapper.decodeFromString(it) }
}

fun saveCharacter(character: Character){
    inMemoryStorage[character.uuid] = jsonMapper.encodeToString(character)
}

fun getPicture(path: String): String {
    return inMemoryStorage[path] ?: ""
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