package mega.privacy.android.app.presentation.qrcode.mapper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import java.util.EnumMap
import javax.inject.Inject

/**
 * [QRCodeMapper] implementation.
 */
class DefaultQRCodeMapper @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : QRCodeMapper {
    override suspend fun invoke(
        text: String,
        width: Int,
        height: Int,
        penColor: Int,
        bgColor: Int,
    ): Bitmap = withContext(defaultDispatcher) {

        // use zxing library to generate QR code
        val hints: MutableMap<EncodeHintType, ErrorCorrectionLevel?> =
            EnumMap<EncodeHintType, ErrorCorrectionLevel>(EncodeHintType::class.java).apply {
                set(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H)
            }
        val bitMatrix: BitMatrix =
            MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints)

        val w = bitMatrix.width
        val h = bitMatrix.height
        val pixels = IntArray(w * h)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            color = bgColor
        }

        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            paint
        )
        paint.color = penColor
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (bitMatrix[x, y]) penColor else bgColor
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        bitmap

    }
}