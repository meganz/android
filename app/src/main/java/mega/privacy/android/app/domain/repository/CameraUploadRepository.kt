package mega.privacy.android.app.domain.repository

import mega.privacy.android.app.data.repository.DefaultCameraUploadRepository
import mega.privacy.android.app.domain.entity.SyncRecord

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
     * Get sync record if it exists or null
     *
     * @return existing sync record
     */
    fun getSyncRecordOrNull(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopy: Boolean,
    ): SyncRecord?

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
    fun deleteSyncRecordLocalPath(localPath: String?, isSecondary: Boolean)

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
}
