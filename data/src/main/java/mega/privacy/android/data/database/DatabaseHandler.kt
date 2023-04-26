package mega.privacy.android.data.database

import android.database.sqlite.SQLiteDatabase
import mega.privacy.android.data.model.ChatSettings
import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.data.model.chat.NonContactInfo
import mega.privacy.android.data.model.node.OfflineInformation
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.user.UserCredentials

interface DatabaseHandler {

    companion object {
        const val MAX_TRANSFERS = 100
    }

    /**
     * Camera upload backup ID
     *
     * @return [Long].
     */
    val cuBackupID: Long?

    /**
     * Media upload backup id
     *
     *   @return [Long].
     */
    val muBackupID: Long?

    val myEmail: String?

    //get the credential of last login
    val credentials: UserCredentials?

    /**
     * Gets preferences.
     *
     * @return Preferences.
     */
    val preferences: MegaPreferences?

    /**
     * Save chat settings in the current DB.
     *
     * @param chatSettings Chat settings to save.
     */
    var chatSettings: ChatSettings?

    /**
     * Sets the chat video quality value.
     * There are four possible values for this setting: VIDEO_QUALITY_ORIGINAL, VIDEO_QUALITY_HIGH,
     * VIDEO_QUALITY_MEDIUM or VIDEO_QUALITY_LOW.
     *
     * @param chatVideoQuality The new chat video quality.
     */
    var chatVideoQuality: Int

    /**
     * Sets the local path selected from an external SD card as Media Uploads local folder.
     *
     * @param uriMediaExternalSdCard local path
     */
    var uriMediaExternalSdCard: String?

    /**
     * Sets the flag to indicate if the local path selected as Media Uploads local folder belongs to an external SD card.
     *
     * @param mediaFolderExternalSdCard true if the local path selected belongs to an external SD card, false otherwise
     */
    var mediaFolderExternalSdCard: Boolean
    var passcodeLockType: String?
    var isPasscodeLockEnabled: Boolean
    var passcodeLockCode: String

    /**
     * Sets the time required before ask for the passcode.
     *
     * @param requiredTime The time required before ask for the passcode.
     */
    var passcodeRequiredTime: Int

    /**
     * Sets if the fingerprint lock setting is enabled or not.
     *
     * @param enabled True if the fingerprint is enabled, false otherwise.
     */
    var isFingerprintLockEnabled: Boolean

    /**
     * Sets the flag to indicate if should ask the user about set the current path as default download location.
     *
     * @param askSetDownloadLocation true if should ask, false otherwise.
     */
    var askSetDownloadLocation: Boolean
    val useHttpsOnly: String?
    val showCopyright: String?

    /**
     * Set the last public handle type value into the database.
     *
     * @param lastPublicHandleType Last public handle type value.
     */
    var lastPublicHandleType: Int

    /**
     * Set the storage state value into the database.
     *
     * @param storageState Storage state value.
     */
    var storageState: StorageState

    /**
     * Set the handle of "My chat files" folder into the database.
     *
     * @param myChatFilesFolderHandle Handle value.
     */
    var myChatFilesFolderHandle: Long

    /**
     * Set the status of the transfer queue.
     *
     * @param transferQueueStatus True if the queue is paused, false otherwise.
     */
    var transferQueueStatus: Boolean
    val showNotifOff: String?
    val autoPlayEnabled: String?
    var sdCardUri: String?

    /**
     * Saves attributes in DB.
     *
     * @param attr Attributes to save.
     */
    var attributes: MegaAttributes?

    fun saveCredentials(userCredentials: UserCredentials)
    fun saveSyncRecord(record: SyncRecord)
    fun updateVideoState(state: Int)
    fun fileNameExists(name: String?, isSecondary: Boolean, fileType: Int): Boolean
    fun localPathExists(localPath: String?, isSecondary: Boolean, fileType: Int): Boolean
    fun recordExists(
        originalFingerprint: String?,
        isSecondary: Boolean,
        isCopyOnly: Boolean,
    ): SyncRecord?

    fun findAllPendingSyncRecords(): List<SyncRecord>
    fun findVideoSyncRecordsByState(state: Int): List<SyncRecord>
    fun deleteAllSyncRecords(fileType: Int)
    fun deleteAllSyncRecordsTypeAny()
    fun deleteAllSecondarySyncRecords()
    fun deleteAllPrimarySyncRecords()
    fun deleteVideoRecordsByState(state: Int)
    fun findSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean): SyncRecord?
    fun deleteSyncRecordByPath(path: String?, isSecondary: Boolean)
    fun deleteSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean)
    fun deleteSyncRecordByNewPath(newPath: String?)
    fun deleteSyncRecordByFileName(fileName: String?)
    fun deleteSyncRecordByFingerprint(
        oriFingerprint: String?,
        newFingerprint: String?,
        isSecondary: Boolean,
    )

    fun updateSyncRecordStatusByLocalPath(status: Int, localPath: String?, isSecondary: Boolean)
    fun findSyncRecordByNewPath(newPath: String?): SyncRecord?
    fun shouldClearCamsyncRecords(): Boolean
    fun saveShouldClearCamsyncRecords(should: Boolean)
    fun findMaxTimestamp(isSecondary: Boolean, fileType: Int): Long?
    fun setCameraUploadVideoQuality(quality: Int)
    fun setConversionOnCharging(onCharging: Boolean)
    fun setChargingOnSize(size: Int)
    fun setRemoveGPS(removeGPS: Boolean)
    fun saveMyEmail(email: String?)
    fun saveMyFirstName(firstName: String?)
    fun saveMyLastName(lastName: String?)
    fun clearMegaContacts()
    fun shouldAskForDisplayOver(): Boolean
    fun dontAskForDisplayOver()
    fun setNotificationSoundChat(sound: String?)
    fun setVibrationEnabledChat(enabled: String?)
    fun setWrittenTextItem(handle: String?, text: String?, editedMsgId: String?): Int
    fun areNotificationsEnabled(handle: String?): String?

    /**
     * Add a completed transfer
     *
     * @param transfer the completed transfer to add
     */
    fun addCompletedTransfer(transfer: CompletedTransfer)

    /**
     * Deletes a completed transfer.
     *
     * @param id the identifier of the transfer to delete
     */
    fun deleteTransfer(id: Long)

    fun emptyCompletedTransfers()

    /**
     * Removes the completed transfers which have cancelled or failed as state .
     */
    fun removeFailedOrCancelledTransfers()

    fun isPasscodeLockEnabled(db: SQLiteDatabase): Boolean
    fun setNonContactFirstName(name: String?, handle: String?): Int
    fun setNonContactLastName(lastName: String?, handle: String?): Int
    fun setNonContactEmail(email: String?, handle: String?): Int
    fun setContactNickname(nickname: String?, handle: Long)

    fun exists(handle: Long): Boolean
    fun removeById(id: Int): Int
    fun setFirstTime(firstTime: Boolean)
    fun setCamSyncWifi(wifi: Boolean)
    fun setPreferredViewList(list: Boolean)
    fun setPreferredViewListCamera(list: Boolean)
    fun setPreferredSortCloud(order: String?)
    fun setPreferredSortCameraUpload(order: String?)
    fun setPreferredSortOthers(order: String?)
    fun setLastUploadFolder(folderPath: String)
    fun setLastCloudFolder(folderHandle: String)
    fun setKeepFileNames(charging: Boolean)
    fun setCamSyncEnabled(enabled: Boolean)
    fun setSecondaryUploadEnabled(enabled: Boolean)
    fun setCamSyncHandle(handle: Long)
    fun setSecondaryFolderHandle(handle: Long)
    fun setCamSyncLocalPath(localPath: String)
    fun setUriExternalSDCard(uriExternalSDCard: String?)
    fun setCameraFolderExternalSDCard(cameraFolderExternalSDCard: Boolean)
    fun setSecondaryFolderPath(localPath: String)
    fun setCamSyncFileUpload(fileUpload: Int)
    fun setAccountDetailsTimeStamp()
    fun resetAccountDetailsTimeStamp()
    fun setExtendedAccountDetailsTimestamp()
    fun resetExtendedAccountDetailsTimestamp()
    fun setCamSyncTimeStamp(camSyncTimeStamp: Long)
    fun setCamVideoSyncTimeStamp(camVideoSyncTimeStamp: Long)
    fun setSecSyncTimeStamp(secSyncTimeStamp: Long)
    fun setSecVideoSyncTimeStamp(secVideoSyncTimeStamp: Long)
    fun setStorageAskAlways(storageAskAlways: Boolean)
    fun setStorageDownloadLocation(storageDownloadLocation: String?)
    fun setAttrAskSizeDownload(askSizeDownload: String?)
    fun setAttrAskNoAppDownload(askNoAppDownload: String?)
    fun setAttrAttempts(attempt: Int)
    fun setUseHttpsOnly(useHttpsOnly: Boolean)
    fun setShowCopyright(showCopyright: Boolean)
    fun setShowNotifOff(showNotifOff: Boolean)
    fun setLastPublicHandle(handle: Long)
    fun setLastPublicHandleTimeStamp(lastPublicHandleTimeStamp: Long)
    fun setLastPublicHandleTimeStamp()
    fun setInvalidateSdkCache(invalidateSdkCache: Boolean)
    fun clearCredentials()
    fun clearEphemeral()
    fun clearPreferences()
    fun clearAttributes()
    fun clearContacts()
    fun clearNonContacts()
    fun clearChatItems()
    fun clearChatSettings()
    fun clearOffline(db: SQLiteDatabase)
    fun clearOffline()
    fun clearCompletedTransfers()
    fun clearPendingMessage()


    /**
     * Updates a pending message.
     *
     * @param idMessage   Identifier of the pending message.
     * @param transferTag Identifier of the transfer.
     */
    fun updatePendingMessageOnTransferStart(idMessage: Long, transferTag: Int)

    /**
     * Updates a pending message.
     *
     * @param idMessage  Identifier of the pending message.
     * @param nodeHandle Handle of the node already uploaded.
     * @param state      State of the pending message.
     */
    fun updatePendingMessageOnTransferFinish(idMessage: Long, nodeHandle: String?, state: Int)

    /**
     * Updates a pending message.
     *
     * @param idMessage   Identifier of the pending message.
     * @param transferTag Identifier of the transfer.
     * @param nodeHandle  Handle of the node already uploaded.
     * @param state       State of the pending message.
     */
    fun updatePendingMessage(idMessage: Long, transferTag: Int, nodeHandle: String?, state: Int)
    fun updatePendingMessageOnAttach(idMessage: Long, temporalId: String?, state: Int)
    fun findPendingMessageByIdTempKarere(idTemp: Long): Long
    fun removeSentPendingMessages()
    fun removePendingMessageByChatId(idChat: Long)
    fun removePendingMessageById(idMsg: Long)
    fun setAutoPlayEnabled(enabled: String)
    fun setShowInviteBanner(show: String)
    fun removeSDTransfer(tag: Int)
    fun setBackupAsOutdated(id: Long)
    fun deleteBackupById(id: Long)
    fun clearBackups()

    fun findNonContactByHandle(handle: String?): NonContactInfo?
    fun setContact(contact: Contact)
    fun findContactByHandle(handleParam: Long): Contact?
    fun findContactByEmail(mail: String?): Contact?

    /**
     * Is completed transfers empty
     */
    val isCompletedTransfersEmpty: Boolean

    /**
     * Get offline information
     *
     * @param handle
     * @return Offline information if available
     */
    suspend fun getOfflineInformation(handle: Long): OfflineInformation?

    /**
     * Get list of [OfflineInformation]
     *
     * @param nodePath
     * @param searchQuery
     *
     * @return List of [OfflineInformation]
     */
    suspend fun getOfflineInformationList(
        nodePath: String,
        searchQuery: String?,
    ): List<OfflineInformation>

    /**
     * Get Backup by id
     *
     * @param id
     * @return [Backup]
     */
    fun getBackupById(id: Long): Backup?

    /**
     * update backup
     *
     * @param backup [Backup]
     */
    fun updateBackup(backup: Backup)
}
