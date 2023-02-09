package mega.privacy.android.app.presentation.qrcode.mapper

import android.graphics.Bitmap

/**
 *  Create a circle bitmap of an existing bitmap
 */
fun interface GetCircleBitmapMapper {

    /**
     * invoke
     * @param bitmap  source bitmap
     * @return generated circle shaped bitmap
     */
    suspend operator fun invoke(bitmap: Bitmap): Bitmap
}