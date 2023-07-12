package mega.privacy.android.data.gateway

import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.data.model.MegaAttributes
import mega.privacy.android.data.model.chat.NonContactInfo
import mega.privacy.android.data.model.node.OfflineInformation
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.user.UserCredentials

/**
 * Mega local storage gateway
 *
 * @constructor Create empty Mega local storage gateway
 */
interface MegaLocalStorageGateway {

    /**
     * Get Camera Uploads Primary handle
     */
    suspend fun getCamSyncHandle(): Long?

    /**
     * Get Camera Uploads Secondary handle
     */
    suspend fun getMegaHandleSecondaryFolder(): Long?

    /**
     * Set Camera Uploads Primary handle
     */
    suspend fun setPrimarySyncHandle(primaryHandle: Long)

    /**
     * Set Camera Uploads Secondary handle
     */
    suspend fun setSecondarySyncHandle(secondaryHandle: Long)

    /**
     * Get cloud sort order
     * @return cloud sort order
     */
    suspend fun getCloudSortOrder(): Int

    /**
     * Get camera sort order
     * @return camera sort order
     */
    suspend fun getCameraSortOrder(): Int

    /**
     * Get others sort order
     * @return others sort order
     */
    suspend fun getOthersSortOrder(): Int

    /**
     * Get links sort order
     * @return links sort order
     */
    suspend fun getLinksSortOrder(): Int

    /**
     * Get offline sort order
     * @return offline sort order
     */
    suspend fun getOfflineSortOrder(): Int

    /**
     * Set offline sort order
     * @param order
     */
    suspend fun setOfflineSortOrder(order: Int)

    /**
     * Set cloud sort order
     * @param order
     */
    suspend fun setCloudSortOrder(order: Int)

    /**
     * Set camera sort order
     * @param order
     */
    suspend fun setCameraSortOrder(order: Int)

    /**
     * Set others sort order
     * @param order
     */
    suspend fun setOthersSortOrder(order: Int)

    /**
     * Get user credentials
     *
     * @return user credentials or null
     */
    suspend fun getUserCredentials(): UserCredentials?

    /**
     * Checks if content in Camera Uploads should be uploaded through Wi-Fi only,
     * or through Wi-Fi or Mobile Data
     *
     * @return If true, will only upload on Wi-Fi. It will also return true if the option has not
     * been set (null)
     * Otherwise, will upload through Wi-Fi or Mobile Data
     */
    suspend fun isCameraUploadsByWifi(): Boolean

    /**
     * Get all pending sync records
     * @return pending sync records
     */
    suspend fun getPendingSyncRecords(): List<SyncRecord>

    /**
     * Get sync file upload
     */
    suspend fun getCameraSyncFileUpload(): String?

    /**
     * Sets the upload option of Camera Uploads
     *
     * @param uploadOption A specific [Int] from MegaPreferences
     */
    suspend fun setCameraSyncFileUpload(uploadOption: Int)

    /**
     * Delete sync records by type
     */
    suspend fun deleteAllSyncRecords(syncRecordType: Int)

    /**
     * Delete sync records by path
     */
    suspend fun deleteSyncRecordByPath(path: String?, isSecondary: Boolean)

    /**
     * Delete sync records by local path
     */
    suspend fun deleteSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean)

    /**
     * Delete sync records by fingerprint
     */
    suspend fun deleteSyncRecordByFingerPrint(
        originalPrint: String,
        newPrint: String,
        isSecondary: Boolean,
    )

    /**
     * Get sync record by fingerprint
     * @return sync record
     */
    suspend fun getSyncRecordByFingerprint(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopy: Boolean,
    ): SyncRecord?

    /**
     * Get sync record by new path
     * @return sync record
     */
    suspend fun getSyncRecordByNewPath(path: String): SyncRecord?

    /**
     * Get sync record by local path
     * @return sync record
     */
    suspend fun getSyncRecordByLocalPath(path: String, isSecondary: Boolean): SyncRecord?

    /**
     * Does file name exist
     */
    suspend fun doesFileNameExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean

    /**
     * Does local path exist
     */
    suspend fun doesLocalPathExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean

    /**
     * Save sync record
     */
    suspend fun saveSyncRecord(record: SyncRecord)

    /**
     * Get photo time stamp
     */
    suspend fun getPhotoTimeStamp(): String?

    /**
     * Get video time stamp
     */
    suspend fun getVideoTimeStamp(): String?

    /**
     * Get secondary photo time stamp
     */
    suspend fun getSecondaryPhotoTimeStamp(): String?

    /**
     * Get secondary video time stamp
     */
    suspend fun getSecondaryVideoTimeStamp(): String?

    /**
     * Set photo time stamp
     */
    suspend fun setPhotoTimeStamp(timeStamp: Long)

    /**
     * Set video time stamp
     */
    suspend fun setVideoTimeStamp(timeStamp: Long)

    /**
     * Set secondary photo time stamp
     */
    suspend fun setSecondaryPhotoTimeStamp(timeStamp: Long)

    /**
     * Set secondary video time stamp
     */
    suspend fun setSecondaryVideoTimeStamp(timeStamp: Long)

    /**
     * Do user credentials exist
     */
    suspend fun doCredentialsExist(): Boolean

    /**
     * Do user preferences exist
     */
    suspend fun doPreferencesExist(): Boolean

    /**
     * Do camera upload sync preference exist
     */
    suspend fun doesSyncEnabledExist(): Boolean

    /**
     * Is camera uploads enabled
     */
    suspend fun isCameraUploadsEnabled(): Boolean

    /**
     * Retrieves the Primary Folder local path
     *
     * @return A [String] that contains the Primary Folder local path, or an empty [String]
     * if it does not exist
     */
    suspend fun getPrimaryFolderLocalPath(): String

    /**
     * Sets the new Primary Folder local path
     *
     * @param localPath The new Primary Folder local path
     */
    suspend fun setPrimaryFolderLocalPath(localPath: String)

    /**
     * Sets the new Secondary Folder local path
     *
     * @param localPath The new Secondary Folder local path
     */
    suspend fun setSecondaryFolderLocalPath(localPath: String)

    /**
     * Set secondary upload enabled
     */
    suspend fun setSecondaryEnabled(secondaryCameraUpload: Boolean)

    /**
     * Retrieves the Secondary Folder local path
     *
     * @return A [String] that contains the Primary Folder local path, or an empty [String] if it does not exist
     */
    suspend fun getSecondaryFolderLocalPath(): String

    /**
     * Checks the value in the Database, as to whether Location Tags should be added or not
     * when uploading Photos
     *
     * @return true if Location Tags should be added when uploading Photos, and false if otherwise
     * It will default to false when the value does not exist in the Database yet
     */
    suspend fun areLocationTagsEnabled(): Boolean

    /**
     * Sets the new value in the Database, as to whether Location Tags should be added or not
     * when uploading Photos
     *
     * @param enable true if Location Tags should be added when uploading Photos, and false if otherwise
     */
    suspend fun setLocationTagsEnabled(enable: Boolean)

    /**
     * Get video quality for camera upload
     */
    suspend fun getUploadVideoQuality(): String

    /**
     * Checks whether the File Names are kept or not when uploading content
     *
     * @return true if the File Names should be left as is, and false if otherwise. It will also
     * return false when the value does not exist in the Database
     */
    suspend fun areUploadFileNamesKept(): Boolean

    /**
     * Sets whether the File Names of files to be uploaded will be kept or not
     *
     * @param keepFileNames true if the File Names should now be left as is, and false if otherwise
     */
    suspend fun setUploadFileNamesKept(keepFileNames: Boolean)

    /**
     * Checks whether the Primary Folder is located in an external SD Card or not
     *
     * @return true if the Primary Folder is local in an external SD Card, and false if otherwise
     */
    suspend fun isPrimaryFolderInSDCard(): Boolean

    /**
     * Retrieves the Primary Folder SD Card URI path
     *
     * @return A [String] that contains the Primary Folder SD Card URI path, or an empty [String]
     * if it does not exist
     */
    suspend fun getPrimaryFolderSDCardUriPath(): String

    /**
     * Sets the new Primary Folder SD Card URI Path
     *
     * @param path the new Primary Folder SD Card URI path
     */
    suspend fun setPrimaryFolderSDCardUriPath(path: String)

    /**
     * Is secondary media folder enabled
     */
    suspend fun isSecondaryMediaFolderEnabled(): Boolean

    /**
     * Checks whether the Secondary Folder is located in an external SD Card or not
     *
     * @return true if the Secondary Folder is local in an external SD Card, and false if otherwise
     */
    suspend fun isSecondaryFolderInSDCard(): Boolean

    /**
     * Retrieves the Secondary Folder SD Card URI path
     *
     * @return A [String] that contains the Secondary Folder SD Card URI path, or an empty [String]
     * if it does not exist
     */
    suspend fun getSecondaryFolderSDCardUriPath(): String

    /**
     * Sets the new Secondary Folder SD Card URI Path
     *
     * @param path the new Secondary Folder SD Card URI path
     */
    suspend fun setSecondaryFolderSDCardUriPath(path: String)

    /**
     * Should clear sync records
     */
    suspend fun shouldClearSyncRecords(): Boolean

    /**
     * Get maximum time stamp by upload sync record type
     */
    suspend fun getMaxTimestamp(isSecondary: Boolean, syncRecordType: Int): Long

    /**
     * Get video sync record with status type
     */
    suspend fun getVideoSyncRecordsByStatus(syncStatusType: Int): List<SyncRecord>

    /**
     * Checks whether compressing videos require the device to be charged or not
     *
     * @return true if the device needs to be charged to compress videos, and false if otherwise
     * It will return true by default, if the value does not exist in the Database
     */
    suspend fun isChargingRequiredForVideoCompression(): Boolean

    /**
     * Sets whether compressing videos require the device to be charged or not
     *
     * @param chargingRequired Whether the device needs to be charged or not
     */
    suspend fun setChargingRequiredForVideoCompression(chargingRequired: Boolean)

    /**
     * Retrieves the maximum video file size that can be compressed
     *
     * @return An [Int] that represents the maximum video file size that can be compressed
     */
    suspend fun getVideoCompressionSizeLimit(): Int

    /**
     * Sets the maximum video file size that can be compressed
     *
     * @param size The maximum video file size that can be compressed
     */
    suspend fun setVideoCompressionSizeLimit(size: Int)

    /**
     * Update sync record status by local path
     */
    suspend fun updateSyncRecordStatusByLocalPath(
        syncStatusType: Int,
        localPath: String?,
        isSecondary: Boolean,
    )

    /**
     * Get non contact by handle
     *
     * @param userHandle
     */
    suspend fun getNonContactByHandle(userHandle: Long): NonContactInfo?

    /**
     * Set non contact email
     *
     * @param userHandle
     * @param email
     */
    suspend fun setNonContactEmail(userHandle: Long, email: String)


    /**
     * Get contact by email
     *
     * @param email
     * @return local contact details if found
     */
    suspend fun getContactByEmail(email: String?): Contact?

    /**
     * Set first time
     *
     */
    suspend fun setUserHasLoggedIn()

    /**
     * Get always ask for storage state
     *
     * @return isStorageAskAlways as [Boolean]
     */
    suspend fun getStorageAskAlways(): Boolean

    /**
     * Set to always ask for storage
     *
     * @param isStorageAskAlways
     */
    suspend fun setStorageAskAlways(isStorageAskAlways: Boolean)

    /**
     * Get storage download location
     *
     * @return storageDownloadLocation path as [String]
     */
    suspend fun getStorageDownloadLocation(): String?

    /**
     * Set storage download location
     *
     * @param storageDownloadLocation
     */
    suspend fun setStorageDownloadLocation(storageDownloadLocation: String)

    /**
     * Set passcode l ock enabled
     *
     * @param isPasscodeLockEnabled
     */
    fun setPasscodeLockEnabled(isPasscodeLockEnabled: Boolean)

    /**
     * Set the passcode lock code
     *
     * @param passcodeLockCode
     */
    suspend fun setPasscodeLockCode(passcodeLockCode: String)

    /**
     * Set show copyright
     *
     * @param showCopyrights
     */
    suspend fun setShowCopyright(showCopyrights: Boolean)

    /**
     * Set Camera Upload local path
     *
     * @param path
     */
    suspend fun setCamSyncLocalPath(path: String?)

    /**
     * Sets whether the Primary Folder is located in an external SD Card or not
     *
     * @param isInSDCard Whether the Primary Folder is located in an external SD Card or not
     */
    suspend fun setPrimaryFolderInSDCard(isInSDCard: Boolean)

    /**
     * Sets whether Camera Uploads should only run through Wi-Fi / Wi-Fi or Mobile Data
     *
     * @param wifiOnly If true, Camera Uploads will only run through Wi-Fi
     * If false, Camera Uploads can run through either Wi-Fi or Mobile Data
     */
    suspend fun setCameraUploadsByWifi(wifiOnly: Boolean)

    /**
     * Set if camera upload only uploads images or images and videos
     *
     * @param fileUpload
     */
    suspend fun setCamSyncFileUpload(fileUpload: Int)

    /**
     * Sets the new Video Quality when uploading Videos through Camera Uploads
     *
     * @param quality The Video Quality, represented as an [Int]
     */
    suspend fun setUploadVideoQuality(quality: Int)

    /**
     * Sets the new Video Sync Status for Camera Uploads
     *
     * @param syncStatus The new Video Sync Status, represented as an [Int]
     */
    suspend fun setUploadVideoSyncStatus(syncStatus: Int)

    /**
     * Set camera upload on/off
     *
     * @param enable
     */
    suspend fun setCameraUploadsEnabled(enable: Boolean)

    /**
     * Gets attributes from DB
     */
    suspend fun getAttributes(): MegaAttributes?

    /**
     * This method is to clear Camera Sync Records from the Database
     *
     * @param clearCamSyncRecords the boolean setting whether to clean the cam record
     */
    suspend fun saveShouldClearCamSyncRecords(clearCamSyncRecords: Boolean)

    /**
     * Delete all Primary Sync Records
     */
    suspend fun deleteAllPrimarySyncRecords()

    /**
     * Delete all Secondary Sync Records
     */
    suspend fun deleteAllSecondarySyncRecords()


    /**
     * Get chat files folder handle
     */
    suspend fun getChatFilesFolderHandle(): Long?

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
    suspend fun getOfflineInformation(nodeHandle: Long): OfflineInformation?

    /**
     * Save offline information
     *
     * @param offlineInformation [OfflineInformation]
     */
    suspend fun saveOfflineInformation(offlineInformation: OfflineInformation)

    /**
     * Save first name
     * @param firstName first name
     */
    suspend fun saveMyFirstName(firstName: String)

    /**
     * Save last name
     * @param lastName last name
     */
    suspend fun saveMyLastName(lastName: String)

    /**
     * Set last public handle
     *
     * @param handle
     */
    suspend fun setLastPublicHandle(handle: Long)

    /**
     * Set last public handle time stamp
     */
    suspend fun setLastPublicHandleTimeStamp()

    /**
     * Set last public handle type
     *
     * @param type
     */
    suspend fun setLastPublicHandleType(type: Int)

    /**
     * Gets chat settings.
     */
    suspend fun getChatSettings(): ChatSettings?

    /**
     * Sets chat settings.
     *
     * @param chatSettings [ChatSettings]
     */
    suspend fun setChatSettings(chatSettings: ChatSettings)

    /**
     * Saves the user credentials of the current logged in account.
     *
     * @param userCredentials [UserCredentials]
     */
    suspend fun saveCredentials(userCredentials: UserCredentials)

    /**
     * Clear account credentials.
     */
    suspend fun clearCredentials()

    /**
     * Clear preferences
     */
    suspend fun clearPreferences()

    /**
     * Sets first time.
     */
    suspend fun setFirstTime(firstTime: Boolean)

    /**
     * Get first time
     */
    suspend fun getFirstTime(): Boolean?

    /**
     * Clears offline files.
     */
    suspend fun clearOffline()

    /**
     * Clears contacts.
     */
    suspend fun clearContacts()

    /**
     * Clears non contacts.
     */
    suspend fun clearNonContacts()

    /**
     * Clears chat items.
     */
    suspend fun clearChatItems()

    /**
     * Add a completed transfer
     *
     * @param transfer the completed transfer to add
     */
    suspend fun addCompletedTransfer(transfer: CompletedTransfer)

    /**
     * Clears completed transfers.
     */
    suspend fun clearCompletedTransfers()

    /**
     * clears attributes.
     */
    suspend fun clearAttributes()

    /**
     * Deletes sync records.
     */
    suspend fun deleteAllSyncRecordsTypeAny()

    /**
     * Clears chat settings.
     */
    suspend fun clearChatSettings()

    /**
     * Clears backups.
     */
    suspend fun clearBackups()

    /**
     * Load offline nodes
     *
     * @param path
     * @param searchQuery
     * @return List of [OfflineInformation]
     */
    suspend fun loadOfflineNodes(
        path: String,
        searchQuery: String?,
    ): List<OfflineInformation>

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
     * Update backup
     * @param backup [Backup]
     */
    suspend fun updateBackup(backup: Backup)

    /**
     * Delete oldest completed transfers
     */
    suspend fun deleteOldestCompletedTransfers()

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
}
