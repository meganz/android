package mega.privacy.android.app.presentation.qrcode.mapper

import android.graphics.Bitmap
import java.io.File

/**
 * Load a bitmap from a file
 */
fun interface LoadBitmapFromFileMapper {
    /**
     *  invoke
     *  @param file handle to the file
     *  @return bitmap in the file. null if the file cannot be decoded.
     */
    suspend operator fun invoke(file: File): Bitmap?
}