package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.model.VideoRecentlyWatchedItem
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.chat.ChatPendingChanges
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdatePendingTransferRequest

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
     * Find contact by email
     *
     * @param email
     * @return
     */
    suspend fun getContactByEmail(email: String?): Contact?

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
     * Add a completed transfer
     *
     * @param transfer the completed transfer to add
     */
    suspend fun addCompletedTransfer(transfer: CompletedTransfer)

    /**
     * Add a list of completed transfer
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
     * Get active transfer by tag
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
     * Insert a new active transfer or replace it if there's already an active transfer with the same tag
     */
    suspend fun insertOrUpdateActiveTransfer(activeTransfer: ActiveTransfer)

    /**
     * Insert (or replace  if there's already an active transfer with the same tag) a list of active transfers
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
     * Delete an active transfer by its tag
     */
    suspend fun setActiveTransferAsFinishedByTag(tags: List<Int>)

    /**
     * Get all sd transfers
     *
     * @return the list of sd transfers
     */
    suspend fun getAllSdTransfers(): List<SdTransfer>

    /**
     * Get sd transfers by tag
     *
     * @return the sd transfer with this tag or null if not found
     */
    suspend fun getSdTransferByTag(tag: Int): SdTransfer?

    /**
     * Insert sd transfer
     *
     */
    suspend fun insertSdTransfer(transfer: SdTransfer)

    /**
     * Delete sd transfer by tag
     *
     */
    suspend fun deleteSdTransferByTag(tag: Int)

    /**
     * Get completed transfer by id
     *
     * @param id the id of the completed transfer
     */
    suspend fun getCompletedTransferById(id: Int): CompletedTransfer?

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
     * Get pending transfers by type
     * @return A list of all pending transfers of this type
     */
    fun getPendingTransfersByType(transferType: TransferType): Flow<List<PendingTransfer>>

    /**
     * Get pending transfers by type and state
     * @return A list of all pending transfers of this type and state
     */
    fun getPendingTransfersByTypeAndState(
        transferType: TransferType,
        pendingTransferState: PendingTransferState,
    ): Flow<List<PendingTransfer>>

    /**
     * Get pending transfers by tag
     * @return The pending transfer with this tag or null if not found
     */
    suspend fun getPendingTransfersByTag(tag: Int): PendingTransfer?

    /**
     * Update pending transfers
     */
    suspend fun updatePendingTransfers(vararg updatePendingTransferRequests: UpdatePendingTransferRequest)

    /**
     * Delete pending transfer by tag
     */
    suspend fun deletePendingTransferByTag(tag: Int)

    /**
     * Delete all pending transfers
     */
    suspend fun deleteAllPendingTransfers()
}
