package mega.privacy.android.app.presentation.qrcode.mapper

import android.graphics.Bitmap
import androidx.annotation.ColorInt

/**
 * Generate the bitmap with both QR code and avatar
 */
fun interface CombineQRCodeAndAvatarMapper {

    /**
     * This method assumes [qrCodeBitmap] and [avatarBitmap] will no longer be used elsewhere,
     * so these bitmap instances will be recycled in this mapper.
     *
     * @param qrCodeBitmap bitmap of the QR code
     * @param qrCodeWidth expected width of the QR code output. We assume the QR code is a square.
     * @param qrCodeBgColor background color of QR code
     * @param qrCodeBgColor background color of QR code
     * @param avatarBitmap  bitmap of avatar
     * @param avatarWidth expected width of the avatar.
     * @param avatarBorderWidth border width of the circle around the avatar
     */
    suspend operator fun invoke(
        qrCodeBitmap: Bitmap,
        qrCodeWidth: Int,
        @ColorInt qrCodeBgColor: Int,
        avatarBitmap: Bitmap,
        avatarWidth: Int,
        avatarBorderWidth: Int,
    ): Bitmap
}