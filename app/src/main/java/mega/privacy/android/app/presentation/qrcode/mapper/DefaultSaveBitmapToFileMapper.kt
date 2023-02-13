package mega.privacy.android.app.presentation.qrcode.mapper

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Implementation of [SaveBitmapToFileMapper]
 */
class DefaultSaveBitmapToFileMapper @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SaveBitmapToFileMapper {

    override suspend fun invoke(srcBitmap: Bitmap, dstFile: File) {
        withContext(ioDispatcher) {
            FileOutputStream(dstFile, false).use {
                srcBitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
        }
    }
}