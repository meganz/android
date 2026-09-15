package mega.privacy.android.core.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.EnumMap

/**
 * Encodes [text] into a QR code bitmap on the calling thread. Pure CPU work with no dependencies
 * of its own, so callers — composables, view models, use cases — can call it directly rather than
 * injecting it.
 *
 * Prefer [encodeQrCode], which moves the work off the caller's thread. Reach for this overload only
 * where the result is needed synchronously, such as rendering under Compose inspection.
 *
 * The result is opaque black on a transparent background so that it can be recoloured at draw
 * time, which keeps the bitmap independent of the theme.
 *
 * No quiet zone is encoded into the bitmap: the code fills the requested size, and callers are
 * expected to supply the surrounding margin from their own layout. That keeps the rendered code
 * the size the caller asked for, rather than ZXing's default four-module inset.
 *
 * @param text the value to encode.
 * @param width width of the target bitmap, in pixels.
 * @param height height of the target bitmap, in pixels.
 * @return the encoded bitmap, or null if [text] is blank, the size is not positive, or the text
 * cannot be encoded.
 */
fun encodeQrCodeBitmap(
    text: String,
    width: Int,
    height: Int,
): Bitmap? {
    if (text.isBlank() || width <= 0 || height <= 0) return null

    val hints: MutableMap<EncodeHintType, Any> =
        EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            set(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            set(EncodeHintType.MARGIN, QUIET_ZONE_MODULES)
        }
    val bitMatrix: BitMatrix = runCatching {
        MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints)
    }.getOrNull() ?: return null

    val matrixWidth = bitMatrix.width
    val matrixHeight = bitMatrix.height
    val pixels = IntArray(matrixWidth * matrixHeight)
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        isAntiAlias = true
        color = Color.TRANSPARENT
    }

    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    for (y in 0 until matrixHeight) {
        val offset = y * matrixWidth
        for (x in 0 until matrixWidth) {
            pixels[offset + x] = if (bitMatrix[x, y]) Color.BLACK else Color.TRANSPARENT
        }
    }
    bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
    return bitmap
}

/**
 * Encodes [text] into a QR code bitmap off the caller's thread. See [encodeQrCodeBitmap] for the
 * encoding itself.
 *
 * @param text the value to encode.
 * @param width width of the target bitmap, in pixels.
 * @param height height of the target bitmap, in pixels.
 * @param dispatcher the dispatcher the encoding runs on.
 * @return the encoded bitmap, or null if [text] is blank, the size is not positive, or the text
 * cannot be encoded.
 */
suspend fun encodeQrCode(
    text: String,
    width: Int,
    height: Int,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): Bitmap? = withContext(dispatcher) { encodeQrCodeBitmap(text, width, height) }

/**
 * Quiet zone encoded into the bitmap, in modules. Zero, because callers lay the code out with
 * their own surrounding margin — see [encodeQrCodeBitmap].
 */
private const val QUIET_ZONE_MODULES = 0
