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
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.node.NodeId
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
     * Get Invalid Backup type
     */
    fun getInvalidBackupType(): Int

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
     * @return If true, will only upload on Wi-Fi. Otherwise, will upload through Wi-Fi or Mobile Data
     */
    suspend fun isCameraUploadsByWifi(): Boolean

    /**
     * Sets whether Camera Uploads should only run through Wi-Fi / Wi-Fi or Mobile Data
     *
     * @param wifiOnly If true, Camera Uploads will only run through Wi-Fi
     * If false, Camera Uploads can run through either Wi-Fi or Mobile Data
     */
    suspend fun setCameraUploadsByWifi(wifiOnly: Boolean)

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
     * Retrieves the Primary Folder local path
     *
     * @return A [String] that contains the Primary Folder local path
     */
    suspend fun getPrimaryFolderLocalPath(): String

    /**
     * Sets the new Primary Folder local path
     *
     * @param localPath The new Primary Folder local path
     */
    suspend fun setPrimaryFolderLocalPath(localPath: String)

    /**
     * Retrieves the Secondary Folder local path
     *
     * @return A [String] that contains the Primary Folder local path, or an empty [String] if it
     * does not exist
     */
    suspend fun getSecondaryFolderLocalPath(): String

    /**
     * Set secondary camera upload enabled
     *
     * @return
     */
    suspend fun setSecondaryEnabled(secondaryCameraUpload: Boolean)

    /**
     * Sets the new Secondary Folder local path
     *
     * @param localPath The new Secondary Folder local path
     */
    suspend fun setSecondaryFolderLocalPath(localPath: String)

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
     * Sets the new Video Quality
     *
     * @param videoQuality The new [VideoQuality]
     */
    suspend fun setUploadVideoQuality(videoQuality: VideoQuality)

    /**
     * Sets the new Video Sync Status
     *
     * @param syncStatus The new [SyncStatus]
     */
    suspend fun setUploadVideoSyncStatus(syncStatus: SyncStatus)

    /**
     * Checks whether the File Names are kept or not when uploading content
     *
     * @return true if the File Names should be left as is, and false if otherwise
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
     * Sets whether the Primary Folder is located in an external SD Card or not
     *
     * @param isInSDCard Whether the Primary Folder is located in an external SD Card or not
     */
    suspend fun setPrimaryFolderInSDCard(isInSDCard: Boolean)

    /**
     * Retrieves the Primary Folder SD Card URI path
     *
     * @return A [String] that contains the Primary Folder SD Card URI path
     */
    suspend fun getPrimaryFolderSDCardUriPath(): String

    /**
     * Is secondary media folder enabled
     *
     * @return true if secondary media folder enabled
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
     * @return A [String] that contains the Secondary Folder SD Card URI path
     */
    suspend fun getSecondaryFolderSDCardUriPath(): String

    /**
     * Sets the new Secondary Folder SD Card URI Path
     *
     * @param path the new Secondary Folder SD Card URI path
     */
    suspend fun setSecondaryFolderSDCardUriPath(path: String)

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
     * Checks whether compressing videos require the device to be charged or not
     *
     * @return true if the device needs to be charged to compress videos, and false if otherwise
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
     * Retrieves the Camera Uploads Sync Handles from the API
     *
     * @return A potentially nullable [Pair] of Camera Uploads Sync Handles
     *
     * [Pair.first] represents the Primary Folder Sync Handle for the Camera Uploads folder
     * [Pair.second] represents the Secondary Folder Sync Handle for the Media Uploads folder
     */
    suspend fun getCameraUploadsSyncHandles(): Pair<Long, Long>?

    /**
     * monitor upload service pause State
     */
    fun monitorCameraUploadPauseState(): Flow<Boolean>

    /**
     * Monitor camera upload progress
     *
     * @return a flow of Pair of
     *         [Int] value representing progress between 0 and 100;
     *         [Int] value representing pending elements waiting for upload
     */
    fun monitorCameraUploadProgress(): Flow<Pair<Int, Int>>

    /**
     * Broadcast upload pause state
     */
    suspend fun broadcastUploadPauseState()

    /**
     * Broadcast camera upload progress
     *
     * @param progress represents progress between 0 and 100
     * @param pending represents elements waiting for upload
     */
    suspend fun broadcastCameraUploadProgress(progress: Int, pending: Int)

    /**
     * monitor battery info
     */
    fun monitorBatteryInfo(): Flow<BatteryInfo>

    /**
     * monitor charging stopped info
     */
    fun monitorChargingStoppedInfo(): Flow<Boolean>

    /**
     * rename camera uploads folder name
     *
     * @param nodeHandle handle for node to change name
     * @param newName new name for camera upload folder
     */
    suspend fun renameNode(nodeHandle: Long, newName: String)

    /**
     * Fire a one time work request of camera upload to upload immediately;
     * It will also schedule the camera upload job inside of CameraUploadsService
     *
     */
    suspend fun fireCameraUploadJob()

    /**
     * Fire a request to stop camera upload service.
     *
     * @param aborted true if the Camera Uploads has been aborted prematurely
     */
    suspend fun fireStopCameraUploadJob(aborted: Boolean = true)

    /**
     * Schedule job of camera upload
     *
     * @return The result of schedule job
     */
    suspend fun scheduleCameraUploadJob()

    /**
     * Restart Camera Uploads by executing StopCameraUploadWorker and StartCameraUploadWorker
     * sequentially through Work Chaining
     *
     */
    suspend fun fireRestartCameraUploadJob()

    /**
     * Reschedule Camera Upload with time interval
     */
    suspend fun rescheduleCameraUpload()

    /**
     * Stop the camera upload work by tag.
     * Stop regular camera upload sync heartbeat work by tag.
     *
     */
    suspend fun stopCameraUploadSyncHeartbeatWorkers()

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

    /**
     * Listen to new media
     *
     */
    suspend fun listenToNewMedia()

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
     * Send heartbeat associated with an existing backup
     * @param backupId backup id identifying the backup
     * @param status   backup state
     * @param progress backup progress
     * @param ups      Number of pending upload transfers
     * @param downs    Number of pending download transfers
     * @param ts       Last action timestamp
     * @param lastNode Last node handle to be synced
     */
    suspend fun sendBackupHeartbeat(
        backupId: Long, status: Int, progress: Int, ups: Int, downs: Int,
        ts: Long, lastNode: Long,
    )

    /**
     * Update the information about a registered backup for Backup Centre
     * @param backupId    backup id identifying the backup to be updated
     * @param backupType  back up type requested for the service
     * @param targetNode  MEGA folder to hold the backups
     * @param localFolder Local path of the folder
     * @param state       backup state
     * @param subState    backup subState
     */
    suspend fun updateBackup(
        backupId: Long, backupType: Int, targetNode: Long, localFolder: String?,
        backupName: String, state: Int, subState: Int,
    ): Long

    /**
     * Get upload backup by Id
     * @return [Backup]
     */
    suspend fun getBackupById(id: Long): Backup?


    /**
     * Update backup
     * @param backup [Backup]
     */
    suspend fun updateLocalBackup(backup: Backup)

    /**
     * Set the GPS coordinates of image files as a node attribute.
     * @param nodeId    Handle associated with a node that will receive the information.
     * @param latitude  Latitude in signed decimal degrees notation
     * @param longitude Longitude in signed decimal degrees notation
     */
    suspend fun setCoordinates(
        nodeId: NodeId,
        latitude: Double,
        longitude: Double,
    )
}
