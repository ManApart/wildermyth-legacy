import org.khronos.webgl.ArrayBuffer
import kotlin.js.Json
import kotlin.js.Promise

@JsModule("jszip")
@JsNonModule
external class JSZip {
    fun loadAsync(content: ArrayBuffer): Promise<ZipObject>
    class ZipObject {
        val files: Json
        fun file(name: String): ZipObject?
        fun <T> async(kind: String): Promise<T>
    }
}

object JsonObject {
    fun keys(obj: Any): List<String> {
        val raw = js("Object.keys(obj)") as Array<*>
        return raw.map { it as String }
    }

    fun entries(obj: Any): List<Pair<Any, Any>> {
        val raw = js("Object.entries(obj)") as Array<Array<Any>>
        return raw.map { it[0] to it[1] }
    }
}

@JsModule("idb")
@JsNonModule
external object IDB {
    fun openDB(name: String, version: Int, dbUpgrade: DBUpgrade): Promise<IDBDatabase>
    class DBUpgrade{
        val upgrade: (db: Any, oldVersion: Int, newVersion: Int, transaction: Any) -> Unit
    }
    class IDBDatabase() {
        fun transaction(storeName: String, mode: String): Transaction
    }
    class Transaction {
        fun objectStore(name: String): ObjectStore
    }
    class ObjectStore {
        fun get(key: String): dynamic
        fun put(key: String, value: Any)
    }
}

//object IndexedDBHelper {
//    fun open(name: String, version: Int): IndexDBRequest {
//        return js("indexedDB.open(name, version)") as IndexDBRequest
//    }
//}
//
//class IndexDBRequest {
//    val result: IndexedDB
//    var onSuccess: () -> Unit
//}
//
//class IndexedDB {
//
//}