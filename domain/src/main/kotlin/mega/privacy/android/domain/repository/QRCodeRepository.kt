package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.account.MegaBitmap
import java.io.File

/**
 * Repository to create the QR Code
 */
interface QRCodeRepository {

    /**
     * Generate the bitmap of QR Code <p/>
     *
     * @param text value of the QrCode
     * @param width width of target bitmap
     * @param height height of target bitmap
     * @param color color of the QR code. This is a RGB value
     * @param backgroundColor background color of generated QR code. This is a ARGB value.
     * @return bitmap entity of QrCode bitmap
     *
     */
    suspend fun createQRCode(
        text: String,
        width: Int,
        height: Int,
        color: Int,
        backgroundColor: Int,
    ): MegaBitmap?

    /**
     * Get the handle of the QR File, if it exists.
     *
     * @param fileName name of the file
     * @return file handle. null if expected file does not exist.
     */
    suspend fun getQRFile(fileName: String): File?
}