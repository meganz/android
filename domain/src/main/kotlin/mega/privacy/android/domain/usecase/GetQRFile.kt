package mega.privacy.android.domain.usecase

import java.io.File

/**
 * Use case to get a QR file
 */
fun interface GetQRFile {
    /**
     * Invoke method
     *
     * @param fileName file name of the QR code file
     * @return file handle of the QR file.
     */
    suspend operator fun invoke(fileName: String): File?
}