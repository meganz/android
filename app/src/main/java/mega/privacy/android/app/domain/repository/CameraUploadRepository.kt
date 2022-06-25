package mega.privacy.android.app.domain.repository

import mega.privacy.android.app.data.repository.DefaultCameraUploadRepository
import mega.privacy.android.app.domain.entity.SyncRecord
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaTransfer

/**
 * Camera Upload Repository
 */
interface CameraUploadRepository {

    /**
     * Is camera upload sync by wifi only
     *
     * @return true if sync is by wifi
     */
    fun isSyncByWifi(): Boolean

    /**
     * Is camera upload sync by wifi only
     * if not set (null), then sync by wifi only
     *
     * @return sync by wifi preference
     */
    fun isSyncByWifiDefault(): Boolean

    /**
     * Get camera upload sync timestamp
     *
     * @return sync timestamp
     */
    fun getSyncTimeStamp(): Long

    /**
     * Get camera upload video sync timestamp
     *
     * @return video sync timestamp
     */
    fun getVideoSyncTimeStamp(): Long

    /**
     * Get camera upload secondary sync timestamp
     *
     * @return secondary sync timestamp string
     */
    fun getSecondarySyncTimeStamp(): String?

    /**
     * Get camera upload secondary video sync timestamp
     *
     * @return secondary video sync timestamp string
     */
    fun getSecondaryVideoSyncTimeStamp(): String?

    /**
     * Set camera upload sync timestamp
     *
     * @return
     */
    fun setSyncTimeStamp(timestamp: Long, type: DefaultCameraUploadRepository.SyncTimeStamp)

    /**
     * Manage the sync file upload preference
     *
     * @return
     */
    fun manageSyncFileUpload(
        handlePreference: (preference: Int) -> Unit,
        noPreference: () -> Unit,
    )

    /**
     * Get all pending sync records to prepare for upload
     *
     * @return list of sync records
     */
    fun getPendingSyncRecords(): List<SyncRecord>

    /**
     * Get sync record by fingerprint or null
     *
     * @return existing sync record
     */
    fun getSyncRecordByFingerprint(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopy: Boolean,
    ): SyncRecord?

    /**
     * Get sync record by new path or null
     *
     * @return existing sync record
     */
    fun getSyncRecordByNewPath(path: String): SyncRecord?

    /**
     * Get sync record by local path or null
     *
     * @return existing sync record
     */
    fun getSyncRecordByLocalPath(path: String, isSecondary: Boolean): SyncRecord?

    /**
     * Delete all sync records
     *
     * @return
     */
    fun deleteAllSyncRecords(syncRecordType: Int)

    /**
     * Delete sync record by path
     *
     * @return
     */
    fun deleteSyncRecord(path: String?, isSecondary: Boolean)

    /**
     * Save sync record
     *
     * @return
     */
    fun saveSyncRecord(record: SyncRecord)

    /**
     * Delete sync record by local path
     *
     * @return
     */
    fun deleteSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean)

    /**
     * Delete sync record by fingerprint
     *
     * @return
     */
    fun deleteSyncRecordByFingerprint(originalPrint: String, newPrint: String, isSecondary: Boolean)

    /**
     * Should clear camera upload sync records
     *
     * @return
     */
    fun shouldClearSyncRecords(clearSyncRecords: Boolean)

    /**
     * Does file name exist in database
     *
     * @return true if file name exists
     */
    fun doesFileNameExist(fileName: String, isSecondary: Boolean, type: Int): Boolean

    /**
     * Does local path exist
     *
     * @return true if local path exists
     */
    fun doesLocalPathExist(fileName: String, isSecondary: Boolean, type: Int): Boolean

    /**
     * Do user credentials exist
     *
     * @return true if user credentials exist
     */
    fun doCredentialsExist(): Boolean

    /**
     * Do preferences exist
     *
     * @return true if preferences exist
     */
    fun doPreferencesExist(): Boolean

    /**
     * Is camera upload sync enabled
     *
     * @return true if camera upload sync enabled
     */
    fun isSyncEnabled(): Boolean

    /**
     * Get camera upload local path
     *
     * @return camera upload local path
     */
    fun getCameraUploadLocalPath(): String?

    /**
     * Set camera upload sync local path
     *
     * @return
     */
    fun setSyncLocalPath(localPath: String)

    /**
     * Get camera upload secondary folder local path
     *
     * @return camera upload secondary folder local path
     */
    fun getSecondaryFolderPath(): String?

    /**
     * Set secondary camera upload enabled
     *
     * @return
     */
    fun setSecondaryEnabled(secondaryCameraUpload: Boolean)

    /**
     * Set camera upload secondary folder path
     *
     * @return
     */
    fun setSecondaryFolderPath(secondaryFolderPath: String)

    /**
     * Get remove GPS preference
     * if not set (null), then remove GPS
     *
     * @return remove GPS preference
     */
    fun getRemoveGpsDefault(): Boolean

    /**
     * Execute a fast login
     *
     * @return
     */
    fun fastLogin(listener: MegaRequestListenerInterface) // TODO SUSPEND

    /**
     * Initialize Mega Chat API
     *
     * @return
     */
    fun initializeMegaChat()

    /**
     * Should compress video
     *
     * @return
     */
    fun shouldCompressVideo(): Boolean

    /**
     * Get keep file names preference
     *
     * @return keep file names preference
     */
    fun getKeepFileNames(): Boolean

    /**
     * Is camera folder on external SD card
     *
     * @return true if camera folder is on external SD card
     */
    fun isFolderExternalSd(): Boolean

    /**
     * Get external SD card URI string
     *
     * @return external SD card URI
     */
    fun getUriExternalSd(): String

    /**
     * Is secondary media folder enabled
     *
     * @return true if secondary media folder enabled
     */
    fun isSecondaryMediaFolderEnabled(): Boolean

    /**
     * Is media folder on external SD card
     *
     * @return true if media folder is on external SD card
     */
    fun isMediaFolderExternalSd(): Boolean

    /**
     * Get media folder on external SD card URI
     *
     * @return media folder on external SD card URI
     */
    fun getUriMediaFolderExternalSd(): String

    /**
     * Should clear camera upload sync records
     *
     * @return true if camera upload sync records should be cleared
     */
    fun shouldClearSyncRecords(): Boolean

    /**
     * Add completed transfer
     *
     * @return
     */
    fun addCompletedTransfer(transfer: MegaTransfer, error: MegaError)

    /**
     * Get maximum timestamp or null
     *
     * @return maximum timestamp
     */
    fun getMaxTimestamp(isSecondary: Boolean, syncRecordType: Int): Long?

    /**
     * Get video sync records by status
     *
     * @return list of video sync records
     */
    fun getVideoSyncRecordsByStatus(syncStatusType: Int): List<SyncRecord>

    /**
     * Get uploaded video quality
     *
     * @return uploaded video quality
     */
    fun getVideoQuality(): String

    /**
     * Convert on charging
     *
     * @return true if conversion on charging
     */
    fun convertOnCharging(): Boolean

    /**
     * Get charging on size string
     *
     * @return charging on size string
     */
    fun getChargingOnSizeString(): String

    /**
     * Get charging on size int
     *
     * @return charging on size int
     */
    fun getChargingOnSize(): Int

    /**
     * Update sync record status by local path
     *
     * @return
     */
    fun updateSyncRecordStatusByLocalPath(
        syncStatusType: Int,
        localPath: String?,
        isSecondary: Boolean,
    )
}
