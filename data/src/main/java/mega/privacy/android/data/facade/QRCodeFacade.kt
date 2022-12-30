package mega.privacy.android.data.facade

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.QRCodeGateway
import mega.privacy.android.data.model.QRCodeBitSet
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import timber.log.Timber
import java.util.BitSet
import java.util.EnumMap
import javax.inject.Inject

/**
 * [QRCodeGateway] implementation.
 */
class QRCodeFacade @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : QRCodeGateway {

    override suspend fun createQRCode(text: String, width: Int, height: Int): QRCodeBitSet? =
        withContext(defaultDispatcher) {
            val hints: MutableMap<EncodeHintType, ErrorCorrectionLevel?> =
                EnumMap(EncodeHintType::class.java)
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            val bitMatrix: BitMatrix = try {
                MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints)
            } catch (e: Exception) {
                Timber.e(e)
                return@withContext null
            }

            val w = bitMatrix.width
            val h = bitMatrix.height
            val bits = BitSet(w * h)
            for (y in 0 until h) {
                val offset = y * w
                for (x in 0 until w) {
                    bits[offset + x] = bitMatrix[x, y]
                }
            }

            return@withContext QRCodeBitSet(w, h, bits)
        }
}