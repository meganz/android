package mega.privacy.android.domain.repository

import java.io.File

/**
 * Repository to create the QR Code
 */
interface QRCodeRepository {

    /**
     * Get the handle of the QR File, if it exists.
     *
     * @param fileName name of the file
     * @return file handle. null if expected file does not exist.
     */
    suspend fun getQRFile(fileName: String): File?
}