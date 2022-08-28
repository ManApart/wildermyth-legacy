import kotlin.test.Test
import kotlin.test.assertEquals

/*
TODO - get this working.
If I use node, it fails to find source files
If I use browser, it fails to find the browser
Running tests in another project for now...
 */
class TestClient {
    @Test
    fun testGreet() {
        assertEquals("world", "world")
    }
} 