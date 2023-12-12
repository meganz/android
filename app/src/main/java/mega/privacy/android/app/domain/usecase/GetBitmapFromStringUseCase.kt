package mega.privacy.android.app.domain.usecase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject

/**
 * Get bitmap from string use case
 *
 */
class GetBitmapFromStringUseCase @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Invoke
     *
     * @param value
     * @return
     */
    suspend operator fun invoke(value: String): Bitmap? = withContext(ioDispatcher) {
        val decodedBytes: ByteArray = Base64.decode(value, 0)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }
}