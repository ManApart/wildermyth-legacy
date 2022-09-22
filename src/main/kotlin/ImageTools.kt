import kotlinx.browser.document
import kotlinx.dom.clear
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image

val canvas = (document.createElement("canvas") as HTMLCanvasElement)
val image = Image()
fun getCroppedHead(
    character: LegacyCharacter,
    sx: Double = 45.0,
    sy: Double = 55.0,
    width: Double = 100.0,
    height: Double = 115.0,
): String? {
    return getPicture("${character.uuid}/head")?.let { picture ->

        image.apply { src = picture }
        canvas.clear()
        canvas.width = width.toInt()
        canvas.height = height.toInt()
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        ctx.drawImage(image, sx, sy, width, height, 0.0, 0.0, width, height)
        canvas.toDataURL("image/png", 0.9)
    }
}