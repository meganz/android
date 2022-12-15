package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.QRCodeGateway
import mega.privacy.android.domain.entity.account.MegaBitmap
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.QRCodeRepository
import javax.inject.Inject

/**
 * Implementation of [QRCodeRepository]
 */
class DefaultQRCodeRepository @Inject constructor(
    private val qrCodeGateway: QRCodeGateway,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : QRCodeRepository {

    override suspend fun createQRCode(
        text: String,
        width: Int,
        height: Int,
        color: Int,
        backgroundColor: Int,
    ): MegaBitmap? = withContext(defaultDispatcher) {
        qrCodeGateway.createQRCode(text, width, height)
            ?.takeIf { it.bits != null }
            ?.let { bitsetModel ->
                val w = bitsetModel.width
                val h = bitsetModel.height
                val pixels = IntArray(w * h)
                bitsetModel.bits?.let { bits ->
                    for (index in 0 until w * h) {
                        pixels[index] = if (bits[index]) color else backgroundColor
                    }
                }

                return@withContext MegaBitmap(w, h, pixels)
            }

        return@withContext null
    }
}