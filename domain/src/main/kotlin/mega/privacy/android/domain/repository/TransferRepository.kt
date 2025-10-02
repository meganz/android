package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroup
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferAppData.RecursiveTransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdatePendingTransferRequest
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Transfer repository of Domain Module
 */
interface TransferRepository {

    companion object {
        const val MAX_COMPLETED_TRANSFERS = 100
    }

    /**
     * Timestamp of the last transfer over quota event warned
     */
    var transferOverQuotaTimestamp: AtomicLong

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
     * @return the number of pending Camera Uploads
     */
    suspend fun getNumPendingCameraUploads(): Int

    /**
     * Gets number of pending transfers.
     *
     * @return Number of pending transfers.
     */
    suspend fun getNumPendingTransfers(): Int

    /**
     * Gets the number of pending and paused uploads.
     *
     * @return Number of pending and paused uploads.
     */
    suspend fun getNumPendingPausedUploads(): Int

    /**
     * @return the number of pending and paused Camera Uploads
     */
    suspend fun getNumPendingPausedCameraUploads(): Int

    /**
     * Gets the number of pending, non-background and paused downloads.
     *
     * @return Number of pending, non-background and paused downloads.
     */
    suspend fun getNumPendingNonBackgroundPausedDownloads(): Int

    /**
     * Cancel Transfer by Tag
     * @param transferTag   Tag that identifies the transfer
     */
    suspend fun cancelTransferByTag(transferTag: Int)

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
    suspend fun broadcastStorageOverQuota(isCurrentOverQuota: Boolean)

    /**
     * Cancels all transfers, uploads and downloads.
     */
    suspend fun cancelTransfers()

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
     * Get transfer by id
     *
     * @param id Unique identifier of the transfer
     */
    suspend fun getTransferByUniqueId(id: Long): Transfer?

    /**
     * Monitors paused transfers.
     */
    fun monitorPausedTransfers(): StateFlow<Boolean>

    /**
     * Resume incomplete transfers started while not logged in
     *
     * This method resumes transfers that were cached while using a non-logged-in MegaApi
     * instance
     *
     * This method can be called when the app detects that there is no session to resume.
     * If a valid session exists, the app should proceed with resuming it, and calling
     * this method will have no effect.
     *
     * @note If there are transfers in progress and the app logs in,
     * any incomplete transfers will be aborted immediately.
     *
     * Please avoid calling this method when logged in.
     */
    suspend fun resumeTransfersForNotLoggedInInstance()

    /**
     * Get in progress transfers
     *
     */
    suspend fun getInProgressTransfers(): List<Transfer>

    /**
     * Monitors list of completed transfers
     *
     * @param limit the limit size of the list. If null, the limit does not apply
     */
    fun monitorCompletedTransfersByStateWithLimit(
        limit: Int,
        vararg states: TransferState,
    ): Flow<List<CompletedTransfer>>

    /**
     * Add a list of completed transfer to local storage. Please note that completed transfers are pruned to prevent them from growing without limit.
     *
     * @param finishEvents
     */
    suspend fun addCompletedTransfers(finishEvents: List<TransferEvent.TransferFinishEvent>)

    /**
     * Add failed completed transfers from a failed pending transfer
     *
     * @param pendingTransfer
     * @param sizeInBytes Size of the node related to the pending transfer
     * @param error The error that caused this pending transfer to fail
     */
    suspend fun addCompletedTransferFromFailedPendingTransfer(
        pendingTransfer: PendingTransfer,
        sizeInBytes: Long,
        error: Throwable,
    )

    /**
     * Add failed completed transfers from a failed pending transfer list
     *
     * @param pendingTransfers
     * @param error The error that caused this pending transfer to fail
     */
    suspend fun addCompletedTransferFromFailedPendingTransfers(
        pendingTransfers: List<PendingTransfer>,
        error: Throwable,
    )

    /**
     * Delete oldest completed transfers
     */
    suspend fun deleteOldestCompletedTransfers()

    /**
     * Delete completed transfers which path contains the given path
     * @param path to search for
     */
    suspend fun deleteCompletedTransfersByPath(path: String)

    /**
     * Starts the download worker to monitor the download transfers as a foreground service
     */
    suspend fun startDownloadWorker()

    /**
     * Starts the chat uploads worker to monitor the chat uploads transfers as a foreground service
     */
    suspend fun startChatUploadsWorker()

    /**
     * Start downloading a node to desired destination and returns a flow to expose download progress
     *
     * @param node              The node we want to download, it can be a folder
     * @param localPath         Full path to the destination folder of [node]. If this path does not exist it will try to create it.
     * @param appData           Custom app data to save in the MegaTransfer object.
     * @param shouldStartFirst  Puts the transfer on top of the download queue.
     */
    fun startDownload(
        node: TypedNode,
        localPath: String,
        appData: List<TransferAppData>?,
        shouldStartFirst: Boolean,
    ): Flow<TransferEvent>

    /**
     * Upload a file or folder
     *
     * @param localPath The local path of the file or folder
     * @param parentNodeId The parent node id for the file or folder
     * @param fileName The custom file name for the file or folder. Leave the parameter as "null"
     * if there are no changes
     * @param modificationTime The custom modification time for the file or folder, denoted in
     * seconds since the epoch. Null if no custom modification time is needed.
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
        modificationTime: Long?,
        appData: List<TransferAppData>?,
        isSourceTemporary: Boolean,
        shouldStartFirst: Boolean,
    ): Flow<TransferEvent>

    /**
     * Get active transfer by uniqueId
     */
    suspend fun getActiveTransferByUniqueId(uniqueId: Long): ActiveTransfer?

    /**
     * Get active transfer by tag
     *
     * Make you sure you use this only for getting the parent folder Transfer using
     * Transfer.folderTransferTag, otherwise it may lead to unexpected results.
     */
    suspend fun getActiveTransferByTag(tag: Int): ActiveTransfer?

    /**
     * Get active transfers by type
     * @return a flow of all active transfers list
     */
    fun getActiveTransfersByType(transferType: TransferType): Flow<List<ActiveTransfer>>

    /**
     * Get current active transfers by type
     * @return A list of all active transfers of this type
     */
    suspend fun getCurrentActiveTransfersByType(transferType: TransferType): List<ActiveTransfer>

    /**
     * Get current active transfers
     * @return all active transfers list
     */
    suspend fun getCurrentActiveTransfers(): List<ActiveTransfer>

    /**
     * Insert a new active transfer or replace it if there's already an active transfer with the same tag
     */
    suspend fun insertOrUpdateActiveTransfer(activeTransfer: ActiveTransfer)

    /**
     * Insert (or replace  if there's already an active transfer with the same tag) a list of active transfers
     */
    suspend fun insertOrUpdateActiveTransfers(activeTransfers: List<ActiveTransfer>)

    /**
     * Set or update the transferred bytes counter of this transfer
     */
    suspend fun updateTransferredBytes(transfers: List<Transfer>)

    /**
     * Delete all active transfer of this type
     */
    suspend fun deleteAllActiveTransfersByType(transferType: TransferType)

    /**
     * Delete all active transfer
     */
    suspend fun deleteAllActiveTransfers()

    /**
     * Set an active transfer as finished by its uniqueId
     * @param uniqueIds the unique ids of the active transfers to be set as finished
     * @param cancelled whether the transfer was cancelled or not
     */
    suspend fun setActiveTransfersAsFinishedByUniqueId(uniqueIds: List<Long>, cancelled: Boolean)

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
     * Sets the pause transfers queue as false, which should be the default value after login.
     */
    suspend fun resetPauseTransfers()

    /**
     * Delete all completed transfers
     */
    suspend fun deleteAllCompletedTransfers()

    /**
     * Delete completed transfers with MegaTransfer.STATE_FAILED or MegaTransfer.STATE_CANCELLED state.
     *
     * @return the failed or cancelled transfer list that was deleted
     */
    suspend fun deleteFailedOrCancelledTransfers(): List<CompletedTransfer>

    /**
     * Delete completed transfers with MegaTransfer.STATE_COMPLETED state.
     */
    suspend fun deleteCompletedTransfers()

    /**
     * Delete completed transfers by id.
     *
     * @param ids
     */
    suspend fun deleteCompletedTransfersById(ids: List<Int>)

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
     * Get current download speed.
     *
     * @return Current download speed.
     */
    suspend fun getCurrentDownloadSpeed(): Int

    /**
     * @return a flow that emits true if DownloadsWorker is enqueued. false otherwise
     */
    fun monitorIsDownloadsWorkerEnqueued(): Flow<Boolean>

    /**
     * @return a flow that emits true if DownloadsWorker is finished (not running or enqueued). false otherwise
     */
    fun monitorIsDownloadsWorkerFinished(): Flow<Boolean>

    /**
     * @return a flow that emits true if ChatUploadsWorker is enqueued. false otherwise
     */
    fun monitorIsChatUploadsWorkerEnqueued(): Flow<Boolean>

    /**
     * @return a flow that emits true if ChatUploadsWorker is finished (not running or enqueued). false otherwise
     */
    fun monitorIsChatUploadsWorkerFinished(): Flow<Boolean>

    /**
     * Monitors ask resume transfers.
     */
    fun monitorAskedResumeTransfers(): StateFlow<Boolean>

    /**
     * Set ask resume transfers.
     */
    suspend fun setAskedResumeTransfers()

    /**
     * Starts the uploads worker to monitor the uploads transfers as a foreground service
     */
    suspend fun startUploadsWorker()

    /**
     * @return a flow that emits true if UploadsWorker is enqueued. false otherwise
     */
    fun monitorIsUploadsWorkerEnqueued(): Flow<Boolean>

    /**
     * @return a flow that emits true if UploadsWorker is finished (not running or enqueued). false otherwise
     */
    fun monitorIsUploadsWorkerFinished(): Flow<Boolean>

    /**
     * Updates or adds a list of transfers to the in progress transfers list.
     */
    suspend fun updateInProgressTransfers(transfers: List<Transfer>)

    /**
     * Monitor in progress transfers flow.
     */
    fun monitorInProgressTransfers(): Flow<Map<Long, InProgressTransfer>>

    /**
     * Remove in progress transfer by uniqueId.
     */
    suspend fun removeInProgressTransfer(uniqueId: Long)

    /**
     * Remove a list of in progress transfers by uniqueId.
     */
    suspend fun removeInProgressTransfers(uniqueIds: Set<Long>)

    /**
     * Gets a flow for pending transfers by type.
     */
    fun monitorPendingTransfersByType(transferType: TransferType): Flow<List<PendingTransfer>>

    /**
     * Gets pending transfers by type.
     */
    suspend fun getPendingTransfersByType(transferType: TransferType): List<PendingTransfer>

    /**
     * Gets pending transfers by state.
     */
    suspend fun getPendingTransfersByState(pendingTransferState: PendingTransferState): List<PendingTransfer>

    /**
     * Gets a flow for pending transfers by type and state.
     */
    fun monitorPendingTransfersByTypeAndState(
        transferType: TransferType,
        pendingTransferState: PendingTransferState,
    ): Flow<List<PendingTransfer>>

    /**
     * Gets pending transfers by type and state.
     */
    suspend fun getPendingTransfersByTypeAndState(
        transferType: TransferType,
        pendingTransferState: PendingTransferState,
    ): List<PendingTransfer>

    /**
     * Inserts a list of pending transfers.
     */
    suspend fun insertPendingTransfers(pendingTransfer: List<InsertPendingTransferRequest>)

    /**
     * Updates a list of pending transfers.
     */
    suspend fun updatePendingTransfers(
        updatePendingTransferRequests: List<UpdatePendingTransferRequest>,
    )

    /**
     * Updates a pending transfer.
     */
    suspend fun updatePendingTransfer(
        updatePendingTransferRequest: UpdatePendingTransferRequest,
    )

    /**
     * Gets a pending transfer by uniqueId.
     */
    suspend fun getPendingTransfersByUniqueId(uniqueId: Long): PendingTransfer?

    /**
     * Deletes a pending transfer by uniqueId.
     */
    suspend fun deletePendingTransferByUniqueId(uniqueId: Long)

    /**
     * Deletes all pending transfers.
     */
    suspend fun deleteAllPendingTransfers()

    /**
     * Sets whether the user has denied the file access permission request
     */
    suspend fun setRequestFilesPermissionDenied()

    /**
     * Monitors whether the user has denied the file access permission request
     */
    fun monitorRequestFilesPermissionDenied(): Flow<Boolean>

    /**
     * Clear all preferences
     */
    suspend fun clearPreferences()

    /**
     * Get the time during which transfers will be stopped due to a bandwidth over quota
     *
     * @return Time during which transfers will be stopped, otherwise 0
     */
    suspend fun getBandwidthOverQuotaDelay(): Duration

    /**
     * Insert a new [ActiveTransferActionGroup].
     * If there's an existing [ActiveTransferActionGroup] with the same id, it will be ignored
     */
    suspend fun insertActiveTransferGroup(activeTransferActionGroup: ActiveTransferActionGroup): Long

    /**
     * Get the [ActiveTransferActionGroup] by id
     *
     * @return [ActiveTransferActionGroup] with this [id] or null if it's not found
     */
    suspend fun getActiveTransferGroupById(id: Int): ActiveTransferActionGroup?

    /**
     * Broadcast transfer tag to cancel. Null it no transfer to cancel.
     */
    suspend fun broadcastTransferTagToCancel(transferTag: Int?)

    /**
     * Monitor transfer tag to cancel
     *
     * @return Flow of Int. Null it no transfer to cancel.
     */
    fun monitorTransferTagToCancel(): Flow<Int?>

    /**
     * Get transfer app data from a folder transfer that needs to be used in children, this data is cached to be used in other calls
     * @param parentTransferTag the tag of the folder transfer which RecursiveTransferAppData app data should be retrieved
     * @param fetchInMemoryParent lambda to get the parent from memory if not already cached. For performance and race condition reasons. If null it will be get from database if needed.
     */
    suspend fun getRecursiveTransferAppDataFromParent(
        parentTransferTag: Int,
        fetchInMemoryParent: () -> Transfer?,
    ): List<RecursiveTransferAppData>

    /**
     * Clear cached transfer app data related to this parent transfer tag
     * @param parentTransferTag the tag of the folder transfer that should be removed from cache because it's not needed anymore
     */
    suspend fun clearRecursiveTransferAppDataFromCache(parentTransferTag: Int)

    /**
     * Monitor transfer over quota error timestamp.
     */
    @OptIn(ExperimentalTime::class)
    fun monitorTransferOverQuotaErrorTimestamp(): Flow<Instant?>

    /**
     * Set transfer over quota error timestamp as current time.
     */
    suspend fun setTransferOverQuotaErrorTimestamp()


    /**
     * Monitors the transfer in error status.
     * The error status is initiated when any transfer completes with an error.
     * And it finishes when the user sees the error (transfers section is opened)
     */
    fun monitorTransferInErrorStatus(): StateFlow<Boolean>

    /**
     * Sets the transfer in error status to false.
     * It should be set when the user sees the error (transfers section is opened)
     */
    fun clearTransferErrorStatus()
}
