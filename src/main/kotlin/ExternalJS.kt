import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLElement
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

@JsModule("localforage")
@JsNonModule
external object LocalForage {
    fun setItem(key: String, value: Any): Promise<*>
    fun getItem(key: String): Promise<Any?>
}


@JsModule("vis-network")
@JsNonModule
@JsName("vis")
external object Vis {
//    class DataSet(data: Array<dynamic>?, options: dynamic?)
//    class DataSet(data: Array<Item>?, options: Options?)
    class Network(container: HTMLElement, data: Data?, options: Options?)
}

interface Item
class Node(id: Int, label: String) : Item
//class DataSet(data: List<Any>?, options: Options? = null)
class Edge(from: Int, to: Int) : Item
class Data(nodes: VisData.DataSet, edges: VisData.DataSet)
class Options

@JsModule("vis-data")
@JsNonModule
@JsName("vis")
external object VisData {
    class DataSet(data: Array<dynamic>, options: Any?)
}