package mega.privacy.android.app.data.gateway.api

import mega.privacy.android.app.data.model.UserCredentials
import mega.privacy.android.app.main.megachat.NonContactInfo
import mega.privacy.android.domain.entity.SyncRecord

/**
 * MegaDBHandlerGateway gateway
 *
 * The gateway interface to the Mega DB handler functionality
 */
interface MegaLocalStorageGateway {

    /**
     * Camera Uploads handle
     */
    suspend fun getCamSyncHandle(): Long?

    /**
     * Media Uploads handle
     */
    suspend fun getMegaHandleSecondaryFolder(): Long?

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
     * Get user credentials
     *
     * @return user credentials or null
     */
    suspend fun getUserCredentials(): UserCredentials?

    /**
     * Is sync by wifi only
     * @return if sync is by wifi only
     */
    fun isSyncByWifi(): Boolean

    /**
     * Is sync by wifi default
     * @return if sync is by wifi default
     */
    fun isSyncByWifiDefault(): Boolean

    /**
     * Get all pending sync records
     * @return pending sync records
     */
    fun getPendingSyncRecords(): List<SyncRecord>

    /**
     * Set photos sync upload
     */
    fun setPhotosSyncUpload()

    /**
     * Get sync file upload
     */
    fun getCameraSyncFileUpload(): String?

    /**
     * Get video quality
     */
    fun getVideoQuality(): String

    /**
     * Delete sync records by type
     */
    fun deleteAllSyncRecords(syncRecordType: Int)

    /**
     * Delete sync records by path
     */
    fun deleteSyncRecordByPath(path: String?, isSecondary: Boolean)

    /**
     * Delete sync records by local path
     */
    fun deleteSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean)

    /**
     * Delete sync records by fingerprint
     */
    fun deleteSyncRecordByFingerPrint(
        originalPrint: String,
        newPrint: String,
        isSecondary: Boolean,
    )

    /**
     * Get sync record by fingerprint
     * @return sync record
     */
    fun getSyncRecordByFingerprint(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopy: Boolean,
    ): SyncRecord?

    /**
     * Get sync record by new path
     * @return sync record
     */
    fun getSyncRecordByNewPath(path: String): SyncRecord?

    /**
     * Get sync record by local path
     * @return sync record
     */
    fun getSyncRecordByLocalPath(path: String, isSecondary: Boolean): SyncRecord?

    /**
     * Should clear sync records
     */
    fun shouldClearSyncRecords(clearSyncRecords: Boolean)

    /**
     * Does file name exist
     */
    fun doesFileNameExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean

    /**
     * Does local path exist
     */
    fun doesLocalPathExist(
        fileName: String,
        isSecondary: Boolean,
        type: Int,
    ): Boolean

    /**
     * Save sync record
     */
    fun saveSyncRecord(record: SyncRecord)

    /**
     * Get photo time stamp
     */
    fun getPhotoTimeStamp(): Long

    /**
     * Get video time stamp
     */
    fun getVideoTimeStamp(): Long

    /**
     * Get secondary photo time stamp
     */
    fun getSecondaryPhotoTimeStamp(): Long

    /**
     * Get secondary video time stamp
     */
    fun getSecondaryVideoTimeStamp(): Long

    /**
     * Set photo time stamp
     */
    fun setPhotoTimeStamp(timeStamp: Long)

    /**
     * Set video time stamp
     */
    fun setVideoTimeStamp(timeStamp: Long)

    /**
     * Set secondary photo time stamp
     */
    fun setSecondaryPhotoTimeStamp(timeStamp: Long)

    /**
     * Set secondary video time stamp
     */
    fun setSecondaryVideoTimeStamp(timeStamp: Long)

    /**
     * Do user credentials exist
     */
    fun doCredentialsExist(): Boolean

    /**
     * Do user preferences exist
     */
    fun doPreferencesExist(): Boolean

    /**
     * Is camera upload sync enabled
     */
    fun isSyncEnabled(): Boolean

    /**
     * Get camera upload sync local path
     */
    fun getSyncLocalPath(): String?

    /**
     * Set camera upload sync local path
     */
    fun setSyncLocalPath(localPath: String)

    /**
     * Set secondary folder path
     */
    fun setSecondaryFolderPath(secondaryFolderPath: String)

    /**
     * Set secondary upload enabled
     */
    fun setSecondaryEnabled(secondaryCameraUpload: Boolean)

    /**
     * Get secondary upload folder path
     */
    fun getSecondaryFolderPath(): String?

    /**
     * Get remove GPS default setting
     */
    fun getRemoveGpsDefault(): Boolean

    /**
     * Get video quality for camera upload
     */
    fun getUploadVideoQuality(): String?

    /**
     * Keep file names for camera upload or not
     */
    fun getKeepFileNames(): Boolean

    /**
     * Is folder path on external SD card
     */
    fun isFolderExternalSd(): Boolean

    /**
     * Get external SD card URI
     */
    fun getUriExternalSd(): String

    /**
     * Is secondary media folder enabled
     */
    fun isSecondaryMediaFolderEnabled(): Boolean

    /**
     * Is media upload folder path on external SD card
     */
    fun isMediaFolderExternalSd(): Boolean

    /**
     * Get media folder external SD card URI
     */
    fun getUriMediaFolderExternalSd(): String

    /**
     * Should clear sync records
     */
    fun shouldClearSyncRecords(): Boolean

    /**
     * Get maximum time stamp by upload sync record type
     */
    fun getMaxTimestamp(isSecondary: Boolean, syncRecordType: Int): Long

    /**
     * Get video sync record with status type
     */
    fun getVideoSyncRecordsByStatus(syncStatusType: Int): List<SyncRecord>

    /**
     * Get charging on size string
     */
    fun getChargingOnSizeString(): String

    /**
     * Convert on charging or not
     */
    fun convertOnCharging(): Boolean

    /**
     * Update sync record status by local path
     */
    fun updateSyncRecordStatusByLocalPath(
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
}
