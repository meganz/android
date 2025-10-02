package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.model.VideoRecentlyWatchedItem
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.chat.ChatPendingChanges
import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.pdf.LastPageViewedInPdf
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroup
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdatePendingTransferRequest
import mega.privacy.android.domain.repository.TransferRepository.Companion.MAX_COMPLETED_TRANSFERS

/**
 * Mega local room gateway
 *
 */
interface MegaLocalRoomGateway {
    /**
     * Save contact
     *
     * @param contact
     */
    suspend fun insertContact(contact: Contact)

    /**
     * Set contact name
     *
     * @param firstName
     * @param email
     * @return
     */
    suspend fun updateContactNameByEmail(firstName: String?, email: String?)

    /**
     * Set contact last name
     *
     * @param lastName
     * @param email
     */
    suspend fun updateContactLastNameByEmail(lastName: String?, email: String?)

    /**
     * Set contact mail
     *
     * @param handle
     * @param email
     */
    suspend fun updateContactMailByHandle(handle: Long, email: String?)

    /**
     * Set contact fist name
     *
     * @param handle
     * @param firstName
     */
    suspend fun updateContactFistNameByHandle(handle: Long, firstName: String?)

    /**
     * Set contact last name
     *
     * @param handle
     * @param lastName
     */
    suspend fun updateContactLastNameByHandle(handle: Long, lastName: String?)

    /**
     * Set contact nickname
     *
     * @param handle
     * @param nickname
     */
    suspend fun updateContactNicknameByHandle(handle: Long, nickname: String?)

    /**
     * Find contact by handle
     *
     * @param handle
     * @return
     */
    suspend fun getContactByHandle(handle: Long): Contact?

    /**
     * Monitor contact by handle
     *
     * @param handle
     * @return
     */
    fun monitorContactByHandle(handle: Long): Flow<Contact>

    /**
     * Find contact by email
     *
     * @param email
     */
    suspend fun getContactByEmail(email: String?): Contact?

    /**
     * Monitor contact by email
     *
     * @param email
     */
    fun monitorContactByEmail(email: String): Flow<Contact?>

    /**
     * Clear contacts
     *
     */
    suspend fun deleteAllContacts()

    /**
     * Get contact count
     *
     * @return
     */
    suspend fun getContactCount(): Int

    /**
     * Get all contacts
     *
     */
    suspend fun getAllContacts(): List<Contact>

    /**
     * Get completed transfers
     *
     * @param size the limit size of the list. If null, the limit does not apply and gets all.
     */
    fun getCompletedTransfers(size: Int? = null): Flow<List<CompletedTransfer>>

    /**
     * Get completed transfers by states
     *
     * @param limit the limit size of the list.
     * @param transferStates the transfer states to filter the completed transfers
     */
    fun getCompletedTransfersByStateWithLimit(
        limit: Int = MAX_COMPLETED_TRANSFERS,
        vararg transferStates: TransferState,
    ): Flow<List<CompletedTransfer>>

    /**
     * Add a completed transfer
     *
     * @param transfer the completed transfer to add
     */
    suspend fun addCompletedTransfer(transfer: CompletedTransfer)

    /**
     * Add a list of completed transfer. Please note that completed transfers are pruned to prevent them from growing without limit.
     *
     * @param transfers the completed transfers to add
     */
    suspend fun addCompletedTransfers(transfers: List<CompletedTransfer>)

    /**
     * Get the completed transfers count
     */
    suspend fun getCompletedTransfersCount(): Int

    /**
     * Delete all completed transfers
     */
    suspend fun deleteAllCompletedTransfers()

    /**
     * Get completed transfers by state
     *
     * @param states
     * @return list of transfers match the state
     */
    suspend fun getCompletedTransfersByState(states: List<Int>): List<CompletedTransfer>

    /**
     * Delete completed transfers by state
     *
     * @param states
     * @return deleted completed transfer list
     */
    suspend fun deleteCompletedTransfersByState(states: List<Int>): List<CompletedTransfer>

    /**
     * Delete completed transfers by id
     *
     * @param ids
     */
    suspend fun deleteCompletedTransfersById(ids: List<Int>)

    /**
     * Delete completed transfer
     *
     * @param completedTransfer
     */
    suspend fun deleteCompletedTransfer(completedTransfer: CompletedTransfer)

    /**
     * Delete oldest completed transfers
     */
    suspend fun deleteOldestCompletedTransfers()

    /**
     * Migrate legacy completed transfers
     */
    suspend fun migrateLegacyCompletedTransfers()

    /**
     * Get active transfer by uniqueId
     */
    suspend fun getActiveTransferByUniqueId(uniqueId: Long): ActiveTransfer?

    /**
     * Get active transfer by tag.
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
     * Get active transfers by type
     * @return A list of all active transfers of this type
     */
    suspend fun getCurrentActiveTransfersByType(transferType: TransferType): List<ActiveTransfer>

    /**
     * Get all active transfers
     * @return A list of all active transfers
     */
    suspend fun getCurrentActiveTransfers(): List<ActiveTransfer>

    /**
     * Insert a new active transfer or update it if there's already an active transfer with the same tag but it's not yet finished
     */
    suspend fun insertOrUpdateActiveTransfer(activeTransfer: ActiveTransfer)

    /**
     * Insert (or update if there's already an active transfer with the same tag) a list of active transfers
     */
    suspend fun insertOrUpdateActiveTransfers(activeTransfers: List<ActiveTransfer>)

    /**
     * Delete all active transfer by type
     */
    suspend fun deleteAllActiveTransfersByType(transferType: TransferType)

    /**
     * Delete all active transfer by type
     */
    suspend fun deleteAllActiveTransfers()

    /**
     * Set an active transfer as finished by its uniqueId
     * @param uniqueIds the unique ids of the active transfers to be set as finished
     * @param cancelled whether the transfer was cancelled or not
     */
    suspend fun setActiveTransfersAsFinishedByUniqueId(uniqueIds: List<Long>, cancelled: Boolean)

    /**
     * Insert a new active transfer group and returns it's id
     */
    suspend fun insertActiveTransferGroup(activeTransferActionGroup: ActiveTransferActionGroup): Long

    /**
     * Get the [ActiveTransferActionGroup] by [groupId]
     */
    suspend fun getActiveTransferGroup(groupId: Int): ActiveTransferActionGroup?

    /**
     * Delete the [ActiveTransferActionGroup] by [groupId]
     */
    suspend fun deleteActiveTransferGroup(groupId: Int)

    /**
     * Insert a list of [CameraUploadsRecord] or replace the record if already exists
     *
     * @param records the list to save in the database
     */
    suspend fun insertOrUpdateCameraUploadsRecords(records: List<CameraUploadsRecord>)

    /**
     * Get all camera uploads records
     *
     * @return the list of [CameraUploadsRecord]
     */
    suspend fun getAllCameraUploadsRecords(): List<CameraUploadsRecord>

    /**
     * Get the records from the database
     *
     * @param uploadStatus a list of upload status to filter the result
     * @param types a list of types (Photos, Videos or both) to filter the result
     * @param folderTypes a list of folder types (Primary, Secondary or both) to filter the list with
     * @return the list of CameraUploadsRecord with matching status
     */
    suspend fun getCameraUploadsRecordsBy(
        uploadStatus: List<CameraUploadsRecordUploadStatus>,
        types: List<CameraUploadsRecordType>,
        folderTypes: List<CameraUploadFolderType>,
    ): List<CameraUploadsRecord>

    /**
     * Set the upload status for the camera uploads record
     *
     * @param mediaId the id of the record
     * @param timestamp the timestamp of the record
     * @param folderType the folder type of the record
     * @param uploadStatus the upload status to set
     */
    suspend fun updateCameraUploadsRecordUploadStatus(
        mediaId: Long,
        timestamp: Long,
        folderType: CameraUploadFolderType,
        uploadStatus: CameraUploadsRecordUploadStatus,
    )

    /**
     * Set the generated fingerprint for the camera uploads record
     *
     * @param mediaId the id of the record
     * @param timestamp the timestamp of the record
     * @param folderType the folder type of the record
     * @param generatedFingerprint the fingerprint computed from the generated file
     */
    suspend fun setCameraUploadsRecordGeneratedFingerprint(
        mediaId: Long,
        timestamp: Long,
        folderType: CameraUploadFolderType,
        generatedFingerprint: String,
    )

    /**
     * Delete the camera uploads record given the folder types
     *
     * @param folderTypes a list of folder type (Primary, Secondary, or both)
     */
    suspend fun deleteCameraUploadsRecords(folderTypes: List<CameraUploadFolderType>)

    /**
     * Remove back up folder
     *
     * @param backupId back up id to be removed
     */
    suspend fun deleteBackupById(backupId: Long)

    /**
     * Set up back up as outdated
     * @param backupId back up id to be removed
     */
    suspend fun setBackupAsOutdated(backupId: Long)

    /**
     * Save a backup to Database
     *
     * @param backup [Backup]
     */
    suspend fun saveBackup(backup: Backup)

    /**
     * Get Camera upload Backup
     * @return [Backup]
     */
    suspend fun getCuBackUp(): Backup?

    /**
     * Get Media upload Backup
     * @return [Backup]
     */
    suspend fun getMuBackUp(): Backup?

    /**
     * Get Camera upload Backup ID
     * @return [Long]
     */
    suspend fun getCuBackUpId(): Long?


    /**
     * Get Media upload Backup ID
     * @return [Long]
     */
    suspend fun getMuBackUpId(): Long?

    /**
     * Get upload backup by Id
     * @return [Backup]
     */
    suspend fun getBackupById(id: Long): Backup?

    /**
     * Given an updated [Backup] object, this updates a specific [Backup] in the Database
     *
     * @param backup the updated [Backup] object
     */
    suspend fun updateBackup(backup: Backup)

    /**
     * Delete All backups from the backups table
     */
    suspend fun deleteAllBackups()

    /**
     * Is offline information available
     *
     * @param nodeHandle
     * @return true if available, else false
     */
    suspend fun isOfflineInformationAvailable(nodeHandle: Long): Boolean

    /**
     * Get offline information
     *
     * @param nodeHandle
     * @return OfflineInformation if available, else null
     */
    suspend fun getOfflineInformation(nodeHandle: Long): Offline?

    /**
     * Save offline information
     *
     * @param offline [Offline]
     */
    suspend fun saveOfflineInformation(offline: Offline): Long

    /**
     * Clears offline files.
     */
    suspend fun clearOffline()

    /**
     * Observer for Offline update
     */
    fun monitorOfflineUpdates(): Flow<List<Offline>>

    /**
     * Get all offline files
     */
    suspend fun getAllOfflineInfo(): List<Offline>

    /**
     * Remove offline node
     */
    suspend fun removeOfflineInformation(nodeId: String)

    /**
     * Get offline node by parent id
     */
    suspend fun getOfflineInfoByParentId(parentId: Int): List<Offline>

    /**
     * Get offline node by ID
     */
    suspend fun getOfflineLineById(id: Int): Offline?

    /**
     * Remove offline info by ID
     */
    suspend fun removeOfflineInformationById(id: Int)

    /**
     * Remove offline info by IDs
     */
    suspend fun removeOfflineInformationByIds(ids: List<Int>)

    /**
     * monitor chat pending changes
     *
     * @param chatId
     * @return
     */
    fun monitorChatPendingChanges(chatId: Long): Flow<ChatPendingChanges?>

    /**
     * Set chat pending changes
     *
     * @param chatPendingChanges [ChatPendingChanges]
     */
    suspend fun setChatPendingChanges(chatPendingChanges: ChatPendingChanges)

    /**
     * Get all recently watched videos
     *
     * @return flow of [VideoRecentlyWatchedItem] list
     */
    suspend fun getAllRecentlyWatchedVideos(): Flow<List<VideoRecentlyWatchedItem>>

    /**
     * Remove recently watched video
     *
     * @param handle removed video handle
     */
    suspend fun removeRecentlyWatchedVideo(handle: Long)

    /**
     * Clear recently watched videos
     */
    suspend fun clearRecentlyWatchedVideos()

    /**
     * Save recently watched video
     *
     * @param item saved [VideoRecentlyWatchedItem]
     */
    suspend fun saveRecentlyWatchedVideo(item: VideoRecentlyWatchedItem)

    /**
     * Save recently watched videos
     *
     * @param items [VideoRecentlyWatchedItem] list
     */
    suspend fun saveRecentlyWatchedVideos(items: List<VideoRecentlyWatchedItem>)

    /**
     * Insert pending transfers
     * @param pendingTransfer
     */
    suspend fun insertPendingTransfers(pendingTransfers: List<InsertPendingTransferRequest>)

    /**
     * Get a flow for pending transfers by type
     * @return A flow with a list of all pending transfers of this type
     */
    fun monitorPendingTransfersByType(transferType: TransferType): Flow<List<PendingTransfer>>

    /**
     * Get pending transfers by type
     * @return A list of all pending transfers of this type
     */
    suspend fun getPendingTransfersByType(transferType: TransferType): List<PendingTransfer>

    /**
     * Get pending transfers by state
     * @return A list of all pending transfers in this state
     */
    suspend fun getPendingTransfersByState(pendingTransferState: PendingTransferState): List<PendingTransfer>

    /**
     * Get a flow for pending transfers by type and state
     * @return A flow with a list of all pending transfers of this type and state
     */
    fun monitorPendingTransfersByTypeAndState(
        transferType: TransferType,
        pendingTransferState: PendingTransferState,
    ): Flow<List<PendingTransfer>>

    /**
     * Get pending transfers by type and state
     * @return A list of all pending transfers of this type and state
     */
    suspend fun getPendingTransfersByTypeAndState(
        transferType: TransferType,
        pendingTransferState: PendingTransferState,
    ): List<PendingTransfer>

    /**
     * Get pending transfers by uniqueId
     * @return The pending transfer with this uniqueId or null if not found
     */
    suspend fun getPendingTransfersByUniqueId(uniqueId: Long): PendingTransfer?

    /**
     * Update pending transfers
     */
    suspend fun updatePendingTransfers(vararg updatePendingTransferRequests: UpdatePendingTransferRequest)

    /**
     * Delete pending transfer by uniqueId
     */
    suspend fun deletePendingTransferByUniqueId(uniqueId: Long)

    /**
     * Delete all pending transfers
     */
    suspend fun deleteAllPendingTransfers()

    /**
     * Delete completed transfers which path contains the given path
     * @param path to search for
     */
    suspend fun deleteCompletedTransfersByPath(path: String)

    /**
     * Insert or update the last page viewed in a PDF document.
     */
    suspend fun insertOrUpdateLastPageViewedInPdf(lastPageViewedInPdf: LastPageViewedInPdf)

    /**
     * Get the last page viewed in a PDF document by its handle.
     */
    suspend fun getLastPageViewedInPdfByHandle(handle: Long): LastPageViewedInPdf?

    /**
     * Delete the last page viewed in a PDF document by its handle.
     */
    suspend fun deleteLastPageViewedInPdfByHandle(handle: Long)

    /**
     * Delete all last page viewed in PDF records.
     */
    suspend fun deleteAllLastPageViewedInPdf()

    /**
     * Delete playback info by handle
     *
     * @param handle the handle of the playback info to delete
     */
    suspend fun deletePlaybackInfo(handle: Long)

    /**
     * Clear all playback infos
     */
    suspend fun clearAllPlaybackInfos()

    /**
     * Clear all audio playback infos
     */
    suspend fun clearAudioPlaybackInfos()

    /**
     * Insert or update playback info
     *
     * @param info the [MediaPlaybackInfo] to insert or update
     */
    suspend fun insertOrUpdatePlaybackInfo(info: MediaPlaybackInfo)

    /**
     * Insert or update a list of playback infos
     *
     * @param infos the list of [MediaPlaybackInfo] to insert or update
     */
    suspend fun insertOrUpdatePlaybackInfos(infos: List<MediaPlaybackInfo>)

    /**
     * Get media playback info by handle
     *
     * @param handle the handle of the media playback info
     * @return the [MediaPlaybackInfo] if found, null otherwise
     */
    suspend fun getMediaPlaybackInfo(handle: Long): MediaPlaybackInfo?

    /**
     * Get all playback infos
     *
     * @return a flow of list of [MediaPlaybackInfo]
     */
    suspend fun monitorAllPlaybackInfos(): Flow<List<MediaPlaybackInfo>>

    /**
     * Get all audio playback infos
     *
     * @return a flow of list of [MediaPlaybackInfo] filtered by audio media type
     */
    suspend fun monitorAudioPlaybackInfos(): Flow<List<MediaPlaybackInfo>>

    /**
     * Insert or update home screen widget configuration
     *
     * @param entity
     */
    suspend fun insertOrUpdateHomeScreenWidgetConfiguration(entity: HomeWidgetConfiguration)

    /**
     * Insert or update home screen widget configurations
     *
     * @param entities
     */
    suspend fun insertOrUpdateHomeScreenWidgetConfigurations(entities: List<HomeWidgetConfiguration>)

    /**
     * Monitor home screen widget configurations
     *
     * @return a flow of list of [HomeWidgetConfiguration]
     */
    fun monitorHomeScreenWidgetConfigurations(): Flow<List<HomeWidgetConfiguration>>

    /**
     * Delete home screen widget configuration
     *
     * @param widgetIdentifier
     */
    suspend fun deleteHomeScreenWidgetConfiguration(widgetIdentifier: String)
}
