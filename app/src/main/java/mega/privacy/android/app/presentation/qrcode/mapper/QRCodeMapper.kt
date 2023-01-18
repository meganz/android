package mega.privacy.android.app.presentation.qrcode.mapper

import android.graphics.Bitmap

/**
 * Mapper that maps a text to QR code bitmap
 */
fun interface QRCodeMapper {

    /**
     * create QR code bitmap
     *
     * @param text the text value of QR code.
     * @param width width of the target bitmap.
     * @param height height of the target bitmap.
     * @param penColor pen color of the QR code. Color format is ARGB.
     * @param bgColor background color of the QR code. Color format is ARGB.
     * @return generated QR code bitmap.  null if anything wrong.
     */
    suspend operator fun invoke(
        text: String,
        width: Int,
        height: Int,
        penColor: Int,
        bgColor: Int,
    ): Bitmap?
}