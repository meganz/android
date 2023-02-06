package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import java.io.File

/**
 * Repository to create the QR Code
 */
interface QRCodeRepository {

    /**
     * Get the handle of the QR File
     *
     * @param fileName name of the file
     * @return file handle. null file does not exist because parent folder does not exist.
     * Otherwise a file handle is always returned even if file does not exist.
     */
    suspend fun getQRFile(fileName: String): File?

    /**
     * Query details of the scanned contact
     *
     * @param scannedHandle Base 64 handle of the scanned qr code
     * @return Details of the scanned contact
     */
    suspend fun queryScannedContactLink(scannedHandle: String): ScannedContactLinkResult

    /**
     * Update database fields on successful result from queryScannedContactLink
     *
     * @param nodeHandle Handle of the contact
     */
    suspend fun updateDatabaseOnQueryScannedContactSuccess(nodeHandle: Long)
}