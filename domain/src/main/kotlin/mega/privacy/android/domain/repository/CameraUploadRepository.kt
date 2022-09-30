package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncTimeStamp

/**
 * Camera Upload Repository
 */
interface CameraUploadRepository {

    /**
     * Get Invalid Handle
     */
    fun getInvalidHandle(): Long

    /**
     * Get Camera Uploads Primary handle
     */
    suspend fun getPrimarySyncHandle(): Long?

    /**
     * Get Camera Uploads Secondary handle
     */
    suspend fun getSecondarySyncHandle(): Long?

    /**
     * Set Camera Uploads Primary handle
     */
    suspend fun setPrimarySyncHandle(primaryHandle: Long)

    /**
     * Set Camera Uploads Secondary handle
     */
    suspend fun setSecondarySyncHandle(secondaryHandle: Long)

    /**
     * Is camera upload sync by wifi only
     *
     * @return true if sync is by wifi
     */
    suspend fun isSyncByWifi(): Boolean

    /**
     * Is camera upload sync by wifi only
     * if not set (null), then sync by wifi only
     *
     * @return sync by wifi preference
     */
    suspend fun isSyncByWifiDefault(): Boolean

    /**
     * Get camera upload sync timestamp
     *
     * @return sync timestamp
     */
    suspend fun getSyncTimeStamp(type: SyncTimeStamp): Long

    /**
     * Set camera upload sync timestamp
     *
     * @return
     */
    suspend fun setSyncTimeStamp(timeStamp: Long, type: SyncTimeStamp)

    /**
     * Get sync file upload
     *
     * @return sync file upload type
     */
    suspend fun getSyncFileUpload(): String?

    /**
     * Set photos sync file upload
     *
     * @return
     */
    suspend fun setPhotosSyncFileUpload()

    /**
     * Get all pending sync records to prepare for upload
     *
     * @return list of sync records
     */
    suspend fun getPendingSyncRecords(): List<SyncRecord>

    /**
     * Get sync record by fingerprint or null
     *
     * @return existing sync record
     */
    suspend fun getSyncRecordByFingerprint(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopy: Boolean,
    ): SyncRecord?

    /**
     * Get sync record by new path or null
     *
     * @return existing sync record
     */
    suspend fun getSyncRecordByNewPath(path: String): SyncRecord?

    /**
     * Get sync record by local path or null
     *
     * @return existing sync record
     */
    suspend fun getSyncRecordByLocalPath(path: String, isSecondary: Boolean): SyncRecord?

    /**
     * Delete all sync records
     *
     * @return
     */
    suspend fun deleteAllSyncRecords(syncRecordType: Int)

    /**
     * Delete sync record by path
     *
     * @return
     */
    suspend fun deleteSyncRecord(path: String?, isSecondary: Boolean)

    /**
     * Save sync record
     *
     * @return
     */
    suspend fun saveSyncRecord(record: SyncRecord)

    /**
     * Delete sync record by local path
     *
     * @return
     */
    suspend fun deleteSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean)

    /**
     * Delete sync record by fingerprint
     *
     * @return
     */
    suspend fun deleteSyncRecordByFingerprint(
        originalPrint: String,
        newPrint: String,
        isSecondary: Boolean,
    )

    /**
     * Should clear camera upload sync records
     *
     * @return
     */
    suspend fun shouldClearSyncRecords(clearSyncRecords: Boolean)

    /**
     * Should clear camera upload sync records
     *
     * @return true if camera upload sync records should be cleared
     */
    suspend fun shouldClearSyncRecords(): Boolean

    /**
     * Does file name exist in database
     *
     * @return true if file name exists
     */
    suspend fun doesFileNameExist(fileName: String, isSecondary: Boolean, type: Int): Boolean

    /**
     * Does local path exist
     *
     * @return true if local path exists
     */
    suspend fun doesLocalPathExist(fileName: String, isSecondary: Boolean, type: Int): Boolean

    /**
     * Do user credentials exist
     *
     * @return true if user credentials exist
     */
    suspend fun doCredentialsExist(): Boolean

    /**
     * Do preferences exist
     *
     * @return true if preferences exist
     */
    suspend fun doPreferencesExist(): Boolean

    /**
     * Is camera upload sync enabled
     *
     * @return true if camera upload sync enabled
     */
    suspend fun isSyncEnabled(): Boolean

    /**
     * Get camera upload local path
     *
     * @return camera upload local path
     */
    suspend fun getSyncLocalPath(): String?

    /**
     * Set camera upload sync local path
     *
     * @return
     */
    suspend fun setSyncLocalPath(localPath: String)

    /**
     * Get camera upload secondary folder local path
     *
     * @return camera upload secondary folder local path
     */
    suspend fun getSecondaryFolderPath(): String?

    /**
     * Set secondary camera upload enabled
     *
     * @return
     */
    suspend fun setSecondaryEnabled(secondaryCameraUpload: Boolean)

    /**
     * Set camera upload secondary folder path
     *
     * @return
     */
    suspend fun setSecondaryFolderPath(secondaryFolderPath: String)

    /**
     * Get remove GPS preference
     * if not set (null), then remove GPS
     *
     * @return remove GPS preference
     */
    suspend fun getRemoveGpsDefault(): Boolean

    /**
     * Get upload video quality
     *
     * @return upload video quality
     */
    suspend fun getUploadVideoQuality(): String?

    /**
     * Get keep file names preference
     *
     * @return keep file names preference
     */
    suspend fun getKeepFileNames(): Boolean

    /**
     * Is camera folder on external SD card
     *
     * @return true if camera folder is on external SD card
     */
    suspend fun isFolderExternalSd(): Boolean

    /**
     * Get external SD card URI string
     *
     * @return external SD card URI
     */
    suspend fun getUriExternalSd(): String?

    /**
     * Is secondary media folder enabled
     *
     * @return true if secondary media folder enabled
     */
    suspend fun isSecondaryMediaFolderEnabled(): Boolean

    /**
     * Is media folder on external SD card
     *
     * @return true if media folder is on external SD card
     */
    suspend fun isMediaFolderExternalSd(): Boolean

    /**
     * Get media folder on external SD card URI
     *
     * @return media folder on external SD card URI
     */
    suspend fun getUriMediaFolderExternalSd(): String?

    /**
     * Get maximum timestamp or null
     *
     * @return maximum timestamp
     */
    suspend fun getMaxTimestamp(isSecondary: Boolean, syncRecordType: Int): Long

    /**
     * Get video sync records by status
     *
     * @return list of video sync records
     */
    suspend fun getVideoSyncRecordsByStatus(syncStatusType: Int): List<SyncRecord>

    /**
     * Get uploaded video quality
     *
     * @return uploaded video quality
     */
    suspend fun getVideoQuality(): String

    /**
     * Convert on charging
     *
     * @return true if conversion on charging
     */
    suspend fun convertOnCharging(): Boolean

    /**
     * Get charging on size string
     *
     * @return charging on size string
     */
    suspend fun getChargingOnSizeString(): String

    /**
     * Get charging on size int
     *
     * @return charging on size int
     */
    suspend fun getChargingOnSize(): Int

    /**
     * Update sync record status by local path
     *
     * @return
     */
    suspend fun updateSyncRecordStatusByLocalPath(
        syncStatusType: Int,
        localPath: String?,
        isSecondary: Boolean,
    )

    /**
     * The method is to backup time stamps, primary upload folder and secondary folder in share preference after
     * database records being cleaned
     */
    suspend fun backupTimestampsAndFolderHandle()
}
