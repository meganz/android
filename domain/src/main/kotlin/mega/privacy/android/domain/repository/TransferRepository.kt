package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.TransfersFinishedState
import mega.privacy.android.domain.exception.MegaException

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
     * @return the number of pending General Uploads
     */
    suspend fun getNumPendingGeneralUploads(): Int

    /**
     * @return the number of pending Camera Uploads
     */
    suspend fun getNumPendingCameraUploads(): Int

    /**
     * @return the number of pending Chat Uploads
     */
    suspend fun getNumPendingChatUploads(): Int

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
     * Gets the number of pending and paused uploads.
     *
     * @return Number of pending and paused uploads.
     */
    suspend fun getNumPendingPausedUploads(): Int

    /**
     * @return the number of pending and paused General Uploads
     */
    suspend fun getNumPendingPausedGeneralUploads(): Int

    /**
     * @return the number of pending and paused Camera Uploads
     */
    suspend fun getNumPendingPausedCameraUploads(): Int

    /**
     * @return the number of pending and paused Chat Uploads
     */
    suspend fun getNumPendingPausedChatUploads(): Int

    /**
     * Gets the number of pending, non-background and paused downloads.
     *
     * @return Number of pending, non-background and paused downloads.
     */
    suspend fun getNumPendingNonBackgroundPausedDownloads(): Int

    /**
     * Cancels all upload transfers
     */
    suspend fun cancelAllUploadTransfers()

    /**
     * Cancels all download transfers
     */
    suspend fun cancelAllDownloadTransfers()

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
     * @param isCurrentOverQuota true if the overquota is currently received, false otherwise
     *
     */
    suspend fun broadcastTransferOverQuota(isCurrentOverQuota: Boolean)

    /**
     * Monitor storage over quota
     */
    fun monitorStorageOverQuota(): Flow<Boolean>

    /**
     * Broadcast storage over quota
     *
     */
    suspend fun broadcastStorageOverQuota()

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
    fun monitorPausedTransfers(): StateFlow<Boolean>

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
     * Get the list of completed transfers
     *
     * @param size the limit size of the list. If null, the limit does not apply
     */
    fun getAllCompletedTransfers(size: Int? = null): Flow<List<CompletedTransfer>>

    /**
     * Add a completed transfer to local storage
     *
     * @param transfer
     */
    suspend fun addCompletedTransfer(transfer: Transfer, megaException: MegaException?)

    /**
     * Add completed transfers if not exist
     *
     * @param transfers
     */
    suspend fun addCompletedTransfersIfNotExist(transfers: List<CompletedTransfer>)

    /**
     * Delete oldest completed transfers
     */
    suspend fun deleteOldestCompletedTransfers()

    /**
     * Starts the download worker to monitor the download transfers as a foreground service
     */
    fun startDownloadWorker()

    /**
     * Monitors transfers finished.
     */
    fun monitorTransfersFinished(): Flow<TransfersFinishedState>

    /**
     * Broadcasts transfers finished.
     */
    suspend fun broadcastTransfersFinished(transfersFinishedState: TransfersFinishedState)

    /**
     * Monitors when transfers management have to stop.
     */
    fun monitorStopTransfersWork(): Flow<Boolean>

    /**
     * Broadcasts when transfers management have to stop.
     */
    suspend fun broadcastStopTransfersWork()

    /**
     * Reset total uploads
     */
    suspend fun resetTotalUploads()

    /**
     * Start downloading a node to desired destination and returns a flow to expose download progress
     *
     * @param node              The node we want to download, it can be a folder
     * @param localPath         Full destination path of the node, including file name if it's a file node. All nested folders must exist.
     * @param appData           Custom app data to save in the MegaTransfer object.
     * @param shouldStartFirst  Puts the transfer on top of the download queue.
     */
    fun startDownload(
        node: TypedNode,
        localPath: String,
        appData: TransferAppData?,
        shouldStartFirst: Boolean,
    ): Flow<TransferEvent>

    /**
     * Gets information about transfer queues.
     *
     * @return [TransferData]
     */
    suspend fun getTransferData(): TransferData?

    /**
     * Upload a file or folder
     *
     * @param localPath The local path of the file or folder
     * @param parentNodeId The parent node id for the file or folder
     * @param fileName The custom file name for the file or folder. Leave the parameter as "null"
     * if there are no changes
     * @param modificationTime The custom modification time for the file or folder, denoted in
     * seconds since the epoch
     * @param appData The custom app data to save, which can be nullable
     * @param isSourceTemporary Whether the temporary file or folder that is created for upload
     * should be deleted or not
     * @param shouldStartFirst Whether the file or folder should be placed on top of the upload
     * queue or not
     *
     * @return a Flow of [TransferEvent]
     */
    fun startUpload(
        localPath: String,
        parentNodeId: NodeId,
        fileName: String?,
        modificationTime: Long,
        appData: String?,
        isSourceTemporary: Boolean,
        shouldStartFirst: Boolean,
    ): Flow<TransferEvent>

    /**
     * Get active transfer by tag
     */
    suspend fun getActiveTransferByTag(tag: Int): ActiveTransfer?

    /**
     * Get active transfers by type
     * @return a flow of all active transfers list
     */
    fun getActiveTransfersByType(transferType: TransferType): Flow<List<ActiveTransfer>>

    /**
     * Get current active transfers by type
     * @return all active transfers list
     */
    suspend fun getCurrentActiveTransfersByType(transferType: TransferType): List<ActiveTransfer>

    /**
     * Insert a new active transfer or replace it if there's already an active transfer with the same tag
     */
    suspend fun insertOrUpdateActiveTransfer(activeTransfer: ActiveTransfer)

    /**
     * Set or update the transferred bytes counter of this transfer
     */
    suspend fun updateTransferredBytes(transfer: Transfer)

    /**
     * Delete all active transfer
     */
    suspend fun deleteAllActiveTransfersByType(transferType: TransferType)

    /**
     * Set an active transfer as finished by its tag
     */
    suspend fun setActiveTransferAsFinishedByTag(tags: List<Int>)

    /**
     * Get active transfer totals by type
     * @return a flow of active transfer totals
     */
    fun getActiveTransferTotalsByType(transferType: TransferType): Flow<ActiveTransferTotals>

    /**
     * Get the current active transfer totals by type
     * @return the current active transfer totals
     */
    suspend fun getCurrentActiveTransferTotalsByType(transferType: TransferType): ActiveTransferTotals

    /**
     * Get current upload speed.
     *
     * @return Current upload speed.
     */
    suspend fun getCurrentUploadSpeed(): Int

    /**
     * Pause transfers
     *
     * @param isPause
     * @return boolean is pause or resume
     */
    suspend fun pauseTransfers(isPause: Boolean): Boolean

    /**
     * Delete all completed transfers
     */
    suspend fun deleteAllCompletedTransfers()

    /**
     * Get failed or cancel transfers
     *
     * @return the failed or cancelled transfer list
     */
    suspend fun getFailedOrCanceledTransfers(): List<CompletedTransfer>

    /**
     * Delete failed or canceled transfers
     *
     * @return the failed or cancelled transfer list was deleted
     */
    suspend fun deleteFailedOrCanceledTransfers(): List<CompletedTransfer>

    /**
     * Delete completed transfer
     *
     * @param transfer
     */
    suspend fun deleteCompletedTransfer(transfer: CompletedTransfer, isRemoveCache: Boolean)

    /**
     * Pause transfer by tag
     *
     * @param transferTag
     * @param isPause
     */
    suspend fun pauseTransferByTag(transferTag: Int, isPause: Boolean): Boolean

    /**
     * Get all sd transfers
     *
     * @return the list of sd transfers
     */
    suspend fun getAllSdTransfers(): List<SdTransfer>

    /**
     * Insert sd transfer
     *
     * @param transfer sd Transfer
     */
    suspend fun insertSdTransfer(transfer: SdTransfer)

    /**
     * Delete sd transfer by tag
     *
     * @param tag tag of transfer
     */
    suspend fun deleteSdTransferByTag(tag: Int)

    /**
     * Get completed transfer by id
     *
     * @param id id of completed transfer
     */
    suspend fun getCompletedTransferById(id: Int): CompletedTransfer?

    /**
     * Get current download speed.
     *
     * @return Current download speed.
     */
    suspend fun getCurrentDownloadSpeed(): Int

    /**
     * Get current downloaded bytes.
     *
     * @return Current downloaded bytes.
     */
    @Deprecated(
        "This value is deprecated in SDK. " +
                "Replace with the corresponding value get from ActiveTransfers when ready"
    )
    suspend fun getTotalDownloadedBytes(): Long

    /**
     * Get current download bytes.
     *
     * @return Current download bytes.
     */
    @Deprecated(
        "This value is deprecated in SDK. " +
                "Replace with the corresponding value get from ActiveTransfers when ready"
    )
    suspend fun getTotalDownloadBytes(): Long

    /**
     * Get total downloads
     *
     * @return Total downloads.
     */
    @Deprecated(
        "This value is deprecated in SDK. " +
                "Replace with the corresponding value get from ActiveTransfers when ready"
    )
    suspend fun getTotalDownloads(): Int

    /**
     * @return a flow that emits true if DownloadsWorker is enqueued. false otherwise
     */
    fun isDownloadsWorkerEnqueuedFlow(): Flow<Boolean>
}
