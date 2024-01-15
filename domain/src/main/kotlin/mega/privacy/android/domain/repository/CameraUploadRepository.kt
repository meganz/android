package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.entity.CameraUploadsFolderDestinationUpdate
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption

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
    suspend fun isCameraUploadsByWifi(): Boolean?

    /**
     * Sets whether Camera Uploads should only run through Wi-Fi / Wi-Fi or Mobile Data
     *
     * @param wifiOnly If true, Camera Uploads will only run through Wi-Fi
     * If false, Camera Uploads can run through either Wi-Fi or Mobile Data
     */
    suspend fun setCameraUploadsByWifi(wifiOnly: Boolean)

    /**
     * Retrieves the upload option of Camera Uploads
     *
     * @return The corresponding [UploadOption]
     */
    suspend fun getUploadOption(): UploadOption?

    /**
     * Sets the upload option of Camera Uploads
     *
     * @param uploadOption The [UploadOption] to set
     */
    suspend fun setUploadOption(uploadOption: UploadOption)

    /**
     * Do user credentials exist
     *
     * @return true if user credentials exist
     */
    suspend fun hasCredentials(): Boolean

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
    suspend fun isCameraUploadsEnabled(): Boolean?

    /**
     * Set camera uploads enabled
     */
    suspend fun setCameraUploadsEnabled(enable: Boolean)

    /**
     * Retrieves the Primary Folder local path
     *
     * @return A [String] that contains the Primary Folder local path
     */
    suspend fun getPrimaryFolderLocalPath(): String?

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
    suspend fun getSecondaryFolderLocalPath(): String?

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
    suspend fun areLocationTagsEnabled(): Boolean?

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
     * Checks whether the File Names are kept or not when uploading content
     *
     * @return true if the File Names should be left as is, and false if otherwise
     */
    suspend fun areUploadFileNamesKept(): Boolean?

    /**
     * Sets whether the File Names of files to be uploaded will be kept or not
     *
     * @param keepFileNames true if the File Names should now be left as is, and false if otherwise
     */
    suspend fun setUploadFileNamesKept(keepFileNames: Boolean)

    /**
     * Is secondary media folder enabled
     *
     * @return true if secondary media folder enabled
     */
    suspend fun isSecondaryMediaFolderEnabled(): Boolean?

    /**
     * Checks whether compressing videos require the device to be charged or not
     *
     * @return true if the device needs to be charged to compress videos, and false if otherwise
     */
    suspend fun isChargingRequiredForVideoCompression(): Boolean?

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
    suspend fun getVideoCompressionSizeLimit(): Int?

    /**
     * Sets the maximum video file size that can be compressed
     *
     * @param size The maximum video file size that can be compressed
     */
    suspend fun setVideoCompressionSizeLimit(size: Int)

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
     * @param selectionQuery db query
     *
     * @return list of camera upload media
     */
    suspend fun getMediaList(
        mediaStoreFileType: MediaStoreFileType,
        selectionQuery: String?,
    ): List<CameraUploadsMedia>

    /**
     * clear all the contents of Internal cache directory
     */
    suspend fun clearCacheDirectory()

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
    fun monitorCameraUploadsFolderDestination(): Flow<CameraUploadsFolderDestinationUpdate>

    /**
     * Broadcast camera upload folder icon updates
     */
    suspend fun broadcastCameraUploadsFolderDestination(data: CameraUploadsFolderDestinationUpdate)

    /**
     * rename camera uploads folder name
     *
     * @param nodeHandle handle for node to change name
     * @param newName new name for camera upload folder
     */
    suspend fun renameNode(nodeHandle: Long, newName: String)

    /**
     * Queue a one time work request of camera upload to upload immediately.
     * The worker will not be queued if a camera uploads worker is already running
     */
    suspend fun startCameraUploads()

    /**
     * Cancel all camera uploads workers
     */
    suspend fun stopCameraUploads()

    /**
     * Schedule the camera uploads worker
     */
    suspend fun scheduleCameraUploads()

    /**
     * Stop the camera uploads work by tag.
     * Stop regular camera uploads sync heartbeat work by tag.
     *
     */
    suspend fun stopCameraUploadsAndBackupHeartbeat()

    /**
     * Listen to new media
     *
     * @param forceEnqueue True if the worker should be enqueued even if it is already running
     *                     Used for enqueueing the same worker from itself
     */
    suspend fun listenToNewMedia(forceEnqueue: Boolean)

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

    /**
     * Get Camera Uploads Name
     */
    fun getCameraUploadsName(): String

    /**
     * Get Media Uploads Name
     */
    fun getMediaUploadsName(): String

    /**
     * Get the selection query to filter the media based on the parent path
     *
     * @param parentPath path that contains the media
     */
    fun getMediaSelectionQuery(parentPath: String): String

    /**
     * Save a list of [CameraUploadsRecord] in the database
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
     * Get the records from the database based on given upload status, types and folder types
     *
     * @param uploadStatus a list of upload status to filter the result
     * @param types a list of types (Photos, Videos or both) to filter the result
     * @param folderTypes a list of folder types (Primary, Secondary or both) to filter the result
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
    suspend fun setRecordUploadStatus(
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
    suspend fun setRecordGeneratedFingerprint(
        mediaId: Long,
        timestamp: Long,
        folderType: CameraUploadFolderType,
        generatedFingerprint: String,
    )

    /**
     * Clear the camera uploads record given the folder types
     *
     * @param folderTypes a list of folder type (Primary, Secondary, or both)
     */
    suspend fun clearRecords(folderTypes: List<CameraUploadFolderType>)
}
