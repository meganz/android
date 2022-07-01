package mega.privacy.android.app.domain.repository

import nz.mega.sdk.MegaTransfer

/**
 * Transfers repository.
 */
interface TransfersRepository {

    /**
     * Gets all transfers of a MegaTransfer::TYPE_UPLOAD type.
     *
     * @return List with upload transfers.
     */
    suspend fun getUploadTransfers(): List<MegaTransfer>

    /**
     * Gets all transfers of a MegaTransfer::TYPE_DOWNLOAD type.
     *
     * @return List with download transfers.
     */
    suspend fun getDownloadTransfers(): List<MegaTransfer>

    /**
     * Gets the number of pending download transfers that are not background transfers.
     *
     * @return Number of pending downloads.
     */
    suspend fun getNumPendingDownloadsNonBackground(): Int

    /**
     * Gets the number of pending upload transfers.
     *
     * @return Number of pending uploads.
     */
    suspend fun getNumPendingUploads(): Int

    /**
     * Gets number of pending transfers.
     *
     * @return Number of pending transfers.
     */
    suspend fun getNumPendingTransfers(): Int

    /**
     * Checks if the completed transfers list is empty.
     *
     * @return True if the completed transfers is empty, false otherwise.
     */
    suspend fun isCompletedTransfersEmpty(): Boolean
}