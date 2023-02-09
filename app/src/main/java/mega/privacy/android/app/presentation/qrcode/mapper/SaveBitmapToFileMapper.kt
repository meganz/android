package mega.privacy.android.app.presentation.qrcode.mapper

import android.graphics.Bitmap
import java.io.File

/**
 * Save a bitmap into a file in JPEG format
 */
fun interface SaveBitmapToFileMapper {

    /**
     *  Invoke
     *  @param srcBitmap bitmap
     *  @param dstFile file
     *
     */
    suspend operator fun invoke(srcBitmap: Bitmap, dstFile: File)

}