package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransfersFinishedState

/**
 * Transfer repository of Domain Module
 */
interface TransferRepository {
    /**
     * Monitor transfer events
     *
     * @return flow of transfer event
     */
    fun monitorTransferEvents(): Flow<TransferEvent>

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

    /**
     * Are transfers paused (downloads and uploads)
     *
     * @return true if downloads and uploads are paused
     */
    suspend fun areTransfersPaused(): Boolean

    /**
     * Gets the number of pending and paused uploads.
     *
     * @return Number of pending and paused uploads.
     */
    suspend fun getNumPendingPausedUploads(): Int

    /**
     * Gets the number of pending, non-background and paused downloads.
     *
     * @return Number of pending, non-background and paused downloads.
     */
    suspend fun getNumPendingNonBackgroundPausedDownloads(): Int

    /**
     * Checks if the queue of transfers is paused or if all in progress transfers are individually paused.
     *
     * @return True if the queue of transfers is paused or if all in progress transfers are
     * individually paused, false otherwise.
     */
    suspend fun areAllTransfersPaused(): Boolean

    /**
     * Cancels all upload transfers
     */
    suspend fun cancelAllUploadTransfers()

    /**
     * Cancel Transfer by Tag
     * @param transferTag   Tag that identifies the transfer
     */
    suspend fun cancelTransferByTag(transferTag: Int)

    /**
     * Reset Total Downloads
     */
    suspend fun resetTotalDownloads()


    /**
     * Monitor the offline availability of the file
     */
    fun monitorOfflineFileAvailability(): Flow<Long>

    /**
     * Broadcast the offline availability of the file
     * @param nodeHandle the node handle
     */
    suspend fun broadcastOfflineFileAvailability(nodeHandle: Long)

    /**
     * Monitor transfer over quota
     */
    fun monitorTransferOverQuota(): Flow<Boolean>

    /**
     * Broadcast transfer over quota
     *
     */
    suspend fun broadcastTransferOverQuota()

    /**
     * Cancels all transfers, uploads and downloads.
     */
    suspend fun cancelTransfers()

    /**
     * Monitor transfer failed
     *
     */
    fun monitorFailedTransfer(): Flow<Boolean>

    /**
     * Broadcast transfer failed
     *
     */
    suspend fun broadcastFailedTransfer(isFailed: Boolean)

    /**
     * Checks if exist ongoing transfers.
     */
    suspend fun ongoingTransfersExist(): Boolean

    /**
     * Move transfer to first by tag
     *
     * @param transferTag
     */
    suspend fun moveTransferToFirstByTag(transferTag: Int)

    /**
     * Move transfer to last by tag
     *
     * @param transferTag
     */
    suspend fun moveTransferToLastByTag(transferTag: Int)

    /**
     * Move transfer before by tag
     *
     * @param transferTag
     * @param prevTransferTag
     */
    suspend fun moveTransferBeforeByTag(transferTag: Int, prevTransferTag: Int)

    /**
     * Get transfer by tag
     *
     * @param transferTag
     */
    suspend fun getTransferByTag(transferTag: Int): Transfer?

    /**
     * Monitors paused transfers.
     */
    fun monitorPausedTransfers(): Flow<Boolean>

    /**
     * Broadcasts paused transfers.
     */
    suspend fun broadcastPausedTransfers()

    /**
     * Get in progress transfers
     *
     */
    suspend fun getInProgressTransfers(): List<Transfer>

    /**
     * Monitor completed transfers
     *
     * @return a flow of completed transfer
     */
    fun monitorCompletedTransfer(): Flow<CompletedTransfer>

    /**
     * Add a completed transfer to local storage
     *
     * @param transfer
     */
    suspend fun addCompletedTransfer(transfer: CompletedTransfer)

    /**
     * Delete oldest completed transfers
     */
    suspend fun deleteOldestCompletedTransfers()

    /**
     * Monitors transfers finished.
     */
    fun monitorTransfersFinished(): Flow<TransfersFinishedState>

    /**
     * Broadcasts transfers finished.
     */
    suspend fun broadcastTransfersFinished(transfersFinishedState: TransfersFinishedState)
}
