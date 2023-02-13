package mega.privacy.android.app.presentation.qrcode.mapper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import java.io.File
import javax.inject.Inject

/**
 * Implementation of [LoadBitmapFromFileMapper]
 */
class DefaultLoadBitmapFromFileMapper @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LoadBitmapFromFileMapper {

    override suspend fun invoke(file: File): Bitmap? = withContext(ioDispatcher) {
        @Suppress("DEPRECATION")
        val bOpts = BitmapFactory.Options().apply {
            inPurgeable = true
            inInputShareable = true
        }

        BitmapFactory.decodeFile(file.absolutePath, bOpts)
    }


}