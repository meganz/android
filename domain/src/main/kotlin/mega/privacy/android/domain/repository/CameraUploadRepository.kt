package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.entity.CameraUploadFolderIconUpdate
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
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
     * Save sync records
     *
     * @return
     */
    suspend fun saveSyncRecords(records: List<SyncRecord>)

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
    ): Boolean

    /**
     * Does local path exist
     *
     * @return true if local path exists
     */
    suspend fun doesLocalPathExist(
        fileName: String,
        isSecondary: Boolean,
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
     * Is camera uploads enabled
     *
     * @return true if camera upload sync enabled
     */
    suspend fun isCameraUploadsEnabled(): Boolean

    /**
     * Set camera uploads enabled
     */
    suspend fun setCameraUploadsEnabled(enable: Boolean)

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
     * @return true if the Primary Folder is located in an external SD Card, and false if otherwise
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
     * Sets the new Primary Folder SD Card URI Path
     *
     * @param path the new Primary Folder SD Card URI path
     */
    suspend fun setPrimaryFolderSDCardUriPath(path: String)

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
    @Deprecated(
        message = "Replace with data flow after refactoring of CameraUploadsPreferencesActivity ",
        replaceWith = ReplaceWith("broadcastCameraUploadFolderIconUpdate")
    )
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
    ): Queue<CameraUploadsMedia>

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
     * Monitor camera upload progress
     *
     * @return a flow of Pair of
     *         [Int] value representing progress between 0 and 100;
     *         [Int] value representing pending elements waiting for upload
     */
    fun monitorCameraUploadProgress(): Flow<Pair<Int, Int>>

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
     * Monitor camera upload folder icon updates
     */
    fun monitorCameraUploadFolderIconUpdate(): Flow<CameraUploadFolderIconUpdate>

    /**
     * Broadcast camera upload folder icon updates
     */
    suspend fun broadcastCameraUploadFolderIconUpdate(data: CameraUploadFolderIconUpdate)

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
     * @param shouldReschedule true if the Camera Uploads should be rescheduled at a later time
     */
    suspend fun stopCameraUploads(shouldReschedule: Boolean)

    /**
     * Schedule job of camera upload
     *
     * @return The result of schedule job
     */
    suspend fun scheduleCameraUploadJob()

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
     * @param heartbeatStatus   heartbeat status
     * @param ups      Number of pending upload transfers
     * @param downs    Number of pending download transfers
     * @param ts       Last action timestamp
     * @param lastNode Last node handle to be synced
     */
    suspend fun sendBackupHeartbeat(
        backupId: Long, heartbeatStatus: HeartbeatStatus, ups: Int, downs: Int,
        ts: Long, lastNode: Long,
    )

    /**
     * Send heartbeat sync with current progress
     * @param backupId  backup id identifying the backup
     * @param progress  current progress of uploads
     * @param ups       Number of pending upload transfers
     * @param downs     Number of pending download transfers
     * @param timeStamp Last action timestamp
     * @param lastNode  Last node handle to be synced
     */
    suspend fun sendBackupHeartbeatSync(
        backupId: Long,
        progress: Int,
        ups: Int,
        downs: Int,
        timeStamp: Long,
        lastNode: Long,
    )

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
     * Get upload backup by Id
     * @return [Backup]
     */
    suspend fun getBackupById(id: Long): Backup?


    /**
     * Updates a specific [Backup] locally in the Database
     *
     * @param backup The [Backup] to be updated in the Database
     */
    suspend fun updateLocalBackup(backup: Backup)

    /**
     * Updates the [BackupState] of a specific [Backup] remotely
     *
     * @param backupId The Backup ID that identifies the Backup to be updated
     * @param backupState The new [backupState] of the Backup to be updated
     *
     * @return The [BackupState] returned when the API Response is successful
     */
    suspend fun updateRemoteBackupState(
        backupId: Long,
        backupState: BackupState,
    ): BackupState

    /**
     * Updates the Backup Name of a specific [Backup] remotely
     *
     * @param backupId The Backup ID that identifies the Backup to be updated
     * @param backupName The new Backup Name to be updated
     */
    suspend fun updateRemoteBackupName(
        backupId: Long,
        backupName: String,
    )

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

    /**
     * @param currentTimeStamp
     * @param localPath
     * @return selection query
     */
    fun getSelectionQuery(currentTimeStamp: Long, localPath: String): String

    /**
     * isCharging
     * @return [Boolean] whether device is charging or not
     */
    suspend fun isCharging(): Boolean

    /**
     * Get backup folder Id
     *
     * @param cameraUploadFolderType chooses between primary and secondary backup folder
     */
    suspend fun getBackupFolderId(cameraUploadFolderType: CameraUploadFolderType): Long?

    /**
     * Remove backup folder
     *
     * @param backupId id of the folder to be removed
     */
    suspend fun removeBackupFolder(backupId: Long): Pair<Long, Int>


    /**
     * Remove backup folder
     *
     * @param backupId backup folder id to be removed
     */
    suspend fun deleteBackupById(backupId: Long)

    /**
     * Set up backup as outdated
     *
     * @param backupId backup folder id to be removed
     */
    suspend fun setBackupAsOutdated(backupId: Long)

    /**
     * monitor camera uploads status info
     *
     * @return flow of [CameraUploadsStatusInfo]
     */
    fun monitorCameraUploadsStatusInfo(): Flow<CameraUploadsStatusInfo>

    /**
     * Monitor CameraUploadSettingsAction.
     *
     * @return Flow [CameraUploadsSettingsAction]
     */
    fun monitorCameraUploadsSettingsActions(): Flow<CameraUploadsSettingsAction>

    /**
     * Broadcast CameraUploadSettingsAction.
     *
     * @param action [CameraUploadsSettingsAction]
     */
    suspend fun broadCastCameraUploadSettingsActions(action: CameraUploadsSettingsAction)

}
