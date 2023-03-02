package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.entity.CameraUploadMedia
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import java.util.Queue

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
     * Setup Primary Camera Upload Folder
     */
    suspend fun setupPrimaryFolder(primaryHandle: Long): Long

    /**
     * Setup Secondary Camera Upload Folder
     */
    suspend fun setupSecondaryFolder(secondaryHandle: Long): Long

    /**
     * Checks if content in Camera Uploads should be uploaded through Wi-Fi only,
     * or through Wi-Fi or Mobile Data
     *
     * @return If true, will only upload on Wi-Fi. It will also return true if the option has not
     * been set (null)
     * Otherwise, will upload through Wi-Fi or Mobile Data
     */
    suspend fun isSyncByWifi(): Boolean

    /**
     * Get camera upload sync timestamp
     *
     * @return sync timestamp
     */
    suspend fun getSyncTimeStamp(type: SyncTimeStamp): Long?

    /**
     * Set camera upload sync timestamp
     *
     * @return
     */
    suspend fun setSyncTimeStamp(timeStamp: Long, type: SyncTimeStamp)

    /**
     * Retrieves the upload option of Camera Uploads
     *
     * @return The corresponding [UploadOption]
     */
    suspend fun getUploadOption(): UploadOption

    /**
     * Sets the upload option of Camera Uploads
     *
     * @param uploadOption The [UploadOption] to set
     */
    suspend fun setUploadOption(uploadOption: UploadOption)

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
    suspend fun deleteAllSyncRecords(syncRecordType: SyncRecordType)

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
     * @return true if camera upload sync records should be cleared
     */
    suspend fun shouldClearSyncRecords(): Boolean

    /**
     * Does file name exist in database
     *
     * @return true if file name exists
     */
    suspend fun doesFileNameExist(
        fileName: String,
        isSecondary: Boolean,
        type: SyncRecordType,
    ): Boolean

    /**
     * Does local path exist
     *
     * @return true if local path exists
     */
    suspend fun doesLocalPathExist(
        fileName: String,
        isSecondary: Boolean,
        type: SyncRecordType,
    ): Boolean

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
     * Do preferences exist
     *
     * @return true if preferences exist
     */
    suspend fun doesSyncEnabledExist(): Boolean

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
     * Checks the value in the Database, as to whether Location Tags should be added or not
     * when uploading Photos
     *
     * @return true if Location Tags should be added when uploading Photos, and false if otherwise
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
     * Get upload video quality
     *
     * @return upload video quality
     */
    suspend fun getUploadVideoQuality(): VideoQuality?

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
    suspend fun getMaxTimestamp(isSecondary: Boolean, syncRecordType: SyncRecordType): Long

    /**
     * Get video sync records by status
     *
     * @return list of video sync records
     */
    suspend fun getVideoSyncRecordsByStatus(syncStatusType: SyncStatus): List<SyncRecord>

    /**
     * Reset total uploads
     */
    suspend fun resetTotalUploads()

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
     * Update camera upload folder (node list) icon
     *
     * @param nodeHandle    updated node handle
     * @param isSecondary   if updated node handle is secondary media
     */
    suspend fun sendUpdateFolderIconBroadcast(nodeHandle: Long, isSecondary: Boolean)

    /**
     * Update camera upload folder destination in settings
     *
     * @param nodeHandle    updated node handle
     * @param isSecondary   if updated node handle is secondary media
     */
    suspend fun sendUpdateFolderDestinationBroadcast(nodeHandle: Long, isSecondary: Boolean)

    /**
     * Get the media queue for a given media type
     *
     * @param mediaStoreFileType different media store file type
     * @param parentPath local path of camera upload
     * @param isVideo if media is video
     * @param selectionQuery db query
     *
     * @return queue of camera upload media
     */
    suspend fun getMediaQueue(
        mediaStoreFileType: MediaStoreFileType,
        parentPath: String?,
        isVideo: Boolean,
        selectionQuery: String?,
    ): Queue<CameraUploadMedia>

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
     * Get GPS coordinates from video file
     *
     * @param filePath
     *
     * @return a pair with latitude and longitude coordinates
     */
    suspend fun getVideoGPSCoordinates(filePath: String): Pair<Float, Float>

    /**
     * Get GPS coordinates from photo file
     *
     * @param filePath
     *
     * @return a pair with latitude and longitude coordinates
     */
    suspend fun getPhotoGPSCoordinates(filePath: String): Pair<Float, Float>

    /**
     * This method is to clear Camera Sync Records from the Database
     *
     * @param clearCamSyncRecords the boolean setting whether to clean the cam record
     */
    suspend fun saveShouldClearCamSyncRecords(clearCamSyncRecords: Boolean)

    /**
     * clear all the contents of Internal cache directory
     */
    suspend fun clearCacheDirectory()

    /**
     * Delete all Primary Sync Records
     */
    suspend fun deleteAllPrimarySyncRecords()

    /**
     * Delete all Secondary Sync Records
     */
    suspend fun deleteAllSecondarySyncRecords()

    /**
     * Convert Base 64 string to handle
     */
    suspend fun convertBase64ToHandle(base64: String): Long

    /**
     * Get user attributes as a pair of primary and secondary handles
     */
    suspend fun getUserAttribute(): Pair<Long, Long>?

    /**
     * monitor upload service pause State
     */
    fun monitorCameraUploadPauseState(): Flow<Boolean>

    /**
     * Broadcast upload pause state
     */
    suspend fun broadcastUploadPauseState()

    /**
     * monitor battery info
     */
    fun monitorBatteryInfo(): Flow<BatteryInfo>

    /**
     * monitor charging stopped info
     */
    fun monitorChargingStoppedInfo(): Flow<Boolean>

    /**
     * set camera upload folder
     * @param primaryFolder handle
     * @param secondaryFolder handle
     */
    suspend fun setCameraUploadsFolders(
        primaryFolder: Long,
        secondaryFolder: Long,
    )

    /**
     * rename camera uploads folder name
     *
     * @param nodeHandle handle for node to change name
     * @param newName new name for camera upload folder
     */
    suspend fun renameNode(nodeHandle: Long, newName: String)


    /**
     * number of pending uploads
     */
    @Deprecated(
        "Function related to statistics will be reviewed in future updates to\n" +
                " * provide more data and avoid race conditions. They could change or be removed in the current form.",
    )
    fun getNumberOfPendingUploads(): Int

    /**
     * compress videos
     * @param records list of [SyncRecord]
     * @return flow of [VideoCompressionState]
     */
    fun compressVideos(
        root: String,
        quality: VideoQuality,
        records: List<SyncRecord>,
    ): Flow<VideoCompressionState>
}
