package mega.privacy.android.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.R
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.AndroidDeviceGateway
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.CameraUploadsMediaGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.WorkManagerGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.CameraUploadsSettingsPreferenceGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.MediaStoreFileTypeUriMapper
import mega.privacy.android.data.mapper.VideoQualityIntMapper
import mega.privacy.android.data.mapper.VideoQualityMapper
import mega.privacy.android.data.mapper.camerauploads.BackupStateIntMapper
import mega.privacy.android.data.mapper.camerauploads.BackupStateMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsHandlesMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsStatusInfoMapper
import mega.privacy.android.data.mapper.camerauploads.HeartbeatStatusIntMapper
import mega.privacy.android.data.mapper.camerauploads.UploadOptionIntMapper
import mega.privacy.android.data.mapper.camerauploads.UploadOptionMapper
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.CameraUploadsFolderDestinationUpdate
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.CameraUploadRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.Continuation

/**
 * Default implementation of [CameraUploadRepository]
 */
internal class DefaultCameraUploadRepository @Inject constructor(
    private val localStorageGateway: MegaLocalStorageGateway,
    private val megaApiGateway: MegaApiGateway,
    private val cameraUploadsMediaGateway: CameraUploadsMediaGateway,
    private val cacheGateway: CacheGateway,
    private val heartbeatStatusIntMapper: HeartbeatStatusIntMapper,
    private val mediaStoreFileTypeUriMapper: MediaStoreFileTypeUriMapper,
    private val appEventGateway: AppEventGateway,
    private val workManagerGateway: WorkManagerGateway,
    private val videoQualityIntMapper: VideoQualityIntMapper,
    private val videoQualityMapper: VideoQualityMapper,
    private val backupStateMapper: BackupStateMapper,
    private val backupStateIntMapper: BackupStateIntMapper,
    private val cameraUploadsHandlesMapper: CameraUploadsHandlesMapper,
    private val uploadOptionMapper: UploadOptionMapper,
    private val uploadOptionIntMapper: UploadOptionIntMapper,
    private val deviceGateway: AndroidDeviceGateway,
    private val megaLocalRoomGateway: MegaLocalRoomGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
    private val cameraUploadsSettingsPreferenceGateway: CameraUploadsSettingsPreferenceGateway,
    private val cameraUploadsStatusInfoMapper: CameraUploadsStatusInfoMapper,
) : CameraUploadRepository {

    override fun getInvalidHandle(): Long = megaApiGateway.getInvalidHandle()

    override fun getInvalidBackupType(): Int = megaApiGateway.getInvalidBackupType()

    override suspend fun getPrimarySyncHandle(): Long? = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.getCameraUploadsHandle()
    }

    override suspend fun getSecondarySyncHandle(): Long? = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.getMediaUploadsHandle()
    }

    override suspend fun setPrimarySyncHandle(primaryHandle: Long) = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.setCameraUploadsHandle(primaryHandle)
    }

    override suspend fun setSecondarySyncHandle(secondaryHandle: Long) = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.setMediaUploadsHandle(secondaryHandle)
    }

    override suspend fun isCameraUploadsByWifi() = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.isUploadByWifi()
    }

    override suspend fun setCameraUploadsByWifi(wifiOnly: Boolean) {
        cameraUploadsSettingsPreferenceGateway.setUploadsByWifi(wifiOnly)
    }

    override suspend fun getUploadOption() = withContext(ioDispatcher) {
        uploadOptionMapper(cameraUploadsSettingsPreferenceGateway.getFileUploadOption())
    }

    override suspend fun setUploadOption(uploadOption: UploadOption) = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.setFileUploadOption(
            uploadOptionIntMapper(
                uploadOption
            )
        )
    }

    override suspend fun doCredentialsExist(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.doCredentialsExist()
    }

    override suspend fun doPreferencesExist(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.doPreferencesExist()
    }

    override suspend fun doesSyncEnabledExist(): Boolean = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.isCameraUploadsEnabled() != null
    }

    override suspend fun isCameraUploadsEnabled(): Boolean? = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.isCameraUploadsEnabled()
    }

    override suspend fun setCameraUploadsEnabled(enable: Boolean) = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.setCameraUploadsEnabled(enable)
    }

    override suspend fun getPrimaryFolderLocalPath(): String? = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.getCameraUploadsLocalPath()
    }

    override suspend fun setPrimaryFolderLocalPath(localPath: String) = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.setCameraUploadsLocalPath(localPath)
    }

    override suspend fun setSecondaryFolderLocalPath(localPath: String) =
        withContext(ioDispatcher) {
            cameraUploadsSettingsPreferenceGateway.setMediaUploadsLocalPath(localPath)
        }

    override suspend fun setSecondaryEnabled(secondaryCameraUpload: Boolean) =
        withContext(ioDispatcher) {
            cameraUploadsSettingsPreferenceGateway.setMediaUploadsEnabled(secondaryCameraUpload)
        }

    override suspend fun getSecondaryFolderLocalPath() = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.getMediaUploadsLocalPath()
    }

    override suspend fun areLocationTagsEnabled(): Boolean? = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.areLocationTagsEnabled()
    }

    override suspend fun setLocationTagsEnabled(enable: Boolean) = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.setLocationTagsEnabled(enable)
    }

    override suspend fun getUploadVideoQuality(): VideoQuality? = withContext(ioDispatcher) {
        videoQualityMapper(cameraUploadsSettingsPreferenceGateway.getUploadVideoQuality())
    }

    override suspend fun setUploadVideoQuality(videoQuality: VideoQuality) =
        withContext(ioDispatcher) {
            cameraUploadsSettingsPreferenceGateway.setUploadVideoQuality(
                videoQualityIntMapper(
                    videoQuality
                )
            )
        }

    override suspend fun areUploadFileNamesKept(): Boolean? = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.areUploadFileNamesKept()
    }

    override suspend fun setUploadFileNamesKept(keepFileNames: Boolean) =
        withContext(ioDispatcher) {
            cameraUploadsSettingsPreferenceGateway.setUploadFileNamesKept(keepFileNames)
        }

    override suspend fun isSecondaryMediaFolderEnabled() = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.isMediaUploadsEnabled()
    }

    @Deprecated(
        "Replace with data flow after refactoring of CameraUploadsPreferencesActivity ",
        replaceWith = ReplaceWith("BroadcastCameraUploadFolderIconUpdateUseCase")
    )

    override suspend fun sendUpdateFolderDestinationBroadcast(
        nodeHandle: Long,
        isSecondary: Boolean,
    ) = withContext(ioDispatcher) {
        appEventGateway.broadcastCameraUploadsFolderDestination(
            CameraUploadsFolderDestinationUpdate(
                nodeHandle = nodeHandle,
                cameraUploadFolderType = if (isSecondary) {
                    CameraUploadFolderType.Secondary
                } else {
                    CameraUploadFolderType.Primary
                }
            )
        )
    }

    override suspend fun getMediaList(
        mediaStoreFileType: MediaStoreFileType,
        selectionQuery: String?,
    ): List<CameraUploadsMedia> = withContext(ioDispatcher) {
        val queue = cameraUploadsMediaGateway.getMediaList(
            mediaStoreFileTypeUriMapper(mediaStoreFileType),
            selectionQuery
        )
        Timber.d("$mediaStoreFileType count from media store database: ${queue.size}")
        queue
    }

    override suspend fun isChargingRequiredForVideoCompression() = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.isChargingRequiredForVideoCompression()
    }

    override suspend fun setChargingRequiredForVideoCompression(chargingRequired: Boolean) =
        withContext(ioDispatcher) {
            cameraUploadsSettingsPreferenceGateway.setChargingRequiredForVideoCompression(
                chargingRequired
            )
        }

    override suspend fun getVideoCompressionSizeLimit() = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.getVideoCompressionSizeLimit()
    }

    override suspend fun setVideoCompressionSizeLimit(size: Int) = withContext(ioDispatcher) {
        cameraUploadsSettingsPreferenceGateway.setVideoCompressionSizeLimit(size)
    }

    override suspend fun setupPrimaryFolder(primaryHandle: Long) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = onSetupFolderRequestFinish(
                    continuation,
                    false,
                )
            )
            megaApiGateway.setCameraUploadsFolders(
                primaryHandle,
                getInvalidHandle(),
                listener
            )
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun setupSecondaryFolder(secondaryHandle: Long) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = onSetupFolderRequestFinish(
                    continuation,
                    true,
                )
            )
            megaApiGateway.setCameraUploadsFolders(
                getInvalidHandle(),
                secondaryHandle,
                listener
            )
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    private fun onSetupFolderRequestFinish(
        continuation: Continuation<Long>,
        isSecondary: Boolean,
    ) = { request: MegaRequest, error: MegaError ->
        if (error.errorCode == MegaError.API_OK) {
            continuation.resumeWith(
                Result.success(
                    if (!isSecondary) request.nodeHandle else request.parentHandle
                )
            )
        } else {
            continuation.failWithError(error, "onSetupFolderRequestFinish")
        }
    }

    override suspend fun getCameraUploadsSyncHandles() = withContext(ioDispatcher) {
        val request = suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request: MegaRequest, error: MegaError ->
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            // camera upload handles can be retrieved
                            continuation.resumeWith(Result.success(request))
                        }

                        MegaError.API_ENOENT -> {
                            // camera upload handles do not exist
                            continuation.resumeWith(Result.success(null))
                        }

                        else -> {
                            continuation.failWithError(error, "getCameraUploadsSyncHandles")
                        }
                    }
                }
            )
            megaApiGateway.getUserAttribute(
                MegaApiJava.USER_ATTR_CAMERA_UPLOADS_FOLDER,
                listener
            )
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
        return@withContext request?.let { megaRequest ->
            var primaryHandle = getInvalidHandle()
            var secondaryHandle = getInvalidHandle()
            cameraUploadsHandlesMapper(megaRequest.megaStringMap).let { handles ->
                handles.first?.let { primaryHandle = convertBase64ToHandle(it) }
                handles.second?.let { secondaryHandle = convertBase64ToHandle(it) }
            }
            return@let Pair(primaryHandle, secondaryHandle)
        }
    }

    override suspend fun clearCacheDirectory() = withContext(ioDispatcher) {
        cacheGateway.clearCacheDirectory()
    }

    override suspend fun convertBase64ToHandle(base64: String): Long = withContext(ioDispatcher) {
        megaApiGateway.base64ToHandle(base64)
    }

    override fun monitorCameraUploadsFolderDestination(): Flow<CameraUploadsFolderDestinationUpdate> =
        appEventGateway.monitorCameraUploadsFolderDestination()

    override suspend fun broadcastCameraUploadsFolderDestination(data: CameraUploadsFolderDestinationUpdate) =
        appEventGateway.broadcastCameraUploadsFolderDestination(data)

    override fun monitorBatteryInfo() = deviceGateway.monitorBatteryInfo

    override fun monitorChargingStoppedInfo() = deviceGateway.monitorChargingStoppedState

    override suspend fun renameNode(nodeHandle: Long, newName: String): Unit =
        withContext(ioDispatcher) {
            val node = megaApiGateway.getMegaNodeByHandle(nodeHandle)
            node?.let {
                suspendCancellableCoroutine { continuation ->
                    val listener =
                        continuation.getRequestListener("renameNode") { return@getRequestListener }
                    megaApiGateway.renameNode(it, newName, listener)
                    continuation.invokeOnCancellation {
                        megaApiGateway.removeRequestListener(listener)
                    }
                }
            }
        }

    override suspend fun startCameraUploads() = withContext(ioDispatcher) {
        workManagerGateway.startCameraUploads()
    }

    override suspend fun stopCameraUploads() = withContext(ioDispatcher) {
        workManagerGateway.stopCameraUploads()
    }

    override suspend fun scheduleCameraUploads() = withContext(ioDispatcher) {
        workManagerGateway.scheduleCameraUploads()
    }

    override suspend fun stopCameraUploadsAndBackupHeartbeat() = withContext(ioDispatcher) {
        workManagerGateway.cancelCameraUploadAndHeartbeatWorkRequest()
    }

    override suspend fun listenToNewMedia(forceEnqueue: Boolean) = withContext(ioDispatcher) {
        workManagerGateway.enqueueNewMediaWorkerRequest(forceEnqueue)
    }

    override suspend fun getCuBackUpId() = withContext(ioDispatcher) {
        megaLocalRoomGateway.getCuBackUpId()
    }

    override suspend fun getMuBackUpId() = withContext(ioDispatcher) {
        megaLocalRoomGateway.getMuBackUpId()
    }

    override suspend fun sendBackupHeartbeat(
        backupId: Long,
        heartbeatStatus: HeartbeatStatus,
        ups: Int,
        downs: Int,
        ts: Long,
        lastNode: Long,
    ) = withContext(ioDispatcher) {
        Timber.d("Sending BackupHeartBeat $backupId, $heartbeatStatus")
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("sendBackupHeartbeat") { }
            megaApiGateway.sendBackupHeartbeat(
                backupId = backupId,
                status = heartbeatStatus.value,
                progress = heartbeatStatusIntMapper(heartbeatStatus),
                ups = ups,
                downs = downs,
                ts = ts,
                lastNode = lastNode,
                listener = listener,
            )
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun sendBackupHeartbeatSync(
        backupId: Long,
        progress: Int,
        ups: Int,
        downs: Int,
        timeStamp: Long,
        lastNode: Long,
    ) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            Timber.d("Sending backup Heartbeat for $backupId")
            val listener = continuation.getRequestListener("sendBackupHeartbeatSync") { }
            megaApiGateway.sendBackupHeartbeat(
                backupId = backupId,
                status = HeartbeatStatus.SYNCING.value,
                progress = progress,
                ups = ups,
                downs = downs,
                ts = timeStamp,
                lastNode = lastNode,
                listener = listener
            )
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun getCuBackUp() = withContext(ioDispatcher) {
        megaLocalRoomGateway.getCuBackUp()
    }

    override suspend fun getMuBackUp() = withContext(ioDispatcher) {
        megaLocalRoomGateway.getMuBackUp()
    }

    override suspend fun getBackupById(id: Long) = withContext(ioDispatcher) {
        megaLocalRoomGateway.getBackupById(id)
    }

    override suspend fun updateRemoteBackupState(
        backupId: Long,
        backupState: BackupState,
    ) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("updateRemoteBackupState") {
                // Return the Backup State, represented as getAccess() in the SDK
                backupStateMapper(it.access)
            }
            // Any values that should not be changed should be marked as null, -1 or -1L
            megaApiGateway.updateBackup(
                backupId = backupId,
                backupType = getInvalidBackupType(),
                targetNode = TARGET_NODE_NO_CHANGE,
                localFolder = null,
                backupName = null,
                state = backupStateIntMapper(backupState),
                subState = SUB_STATE_NO_CHANGE,
                listener = listener,
            )
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun updateLocalBackup(backup: Backup) = withContext(ioDispatcher) {
        megaLocalRoomGateway.updateBackup(backup)
    }

    override suspend fun setCoordinates(nodeId: NodeId, latitude: Double, longitude: Double) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener("setCoordinates") {
                    return@getRequestListener
                }
                megaApiGateway.setCoordinates(nodeId, latitude, longitude, listener)
                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }
        }

    override suspend fun isCharging() = deviceGateway.isCharging().also {
        Timber.d("Is Device charging $it")
    }

    override suspend fun getBackupFolderId(cameraUploadFolderType: CameraUploadFolderType): Long? =
        withContext(ioDispatcher) {
            if (cameraUploadFolderType == CameraUploadFolderType.Primary) {
                megaLocalRoomGateway.getCuBackUpId()
            } else {
                megaLocalRoomGateway.getMuBackUpId()
            }
        }

    override suspend fun removeBackupFolder(backupId: Long) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request: MegaRequest, error: MegaError ->
                    continuation.resumeWith(Result.success(request.parentHandle to error.errorCode))
                },
            )
            megaApiGateway.removeBackup(backupId, listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun deleteBackupById(backupId: Long) = withContext(ioDispatcher) {
        megaLocalRoomGateway.deleteBackupById(backupId)
    }

    override suspend fun setBackupAsOutdated(backupId: Long) = withContext(ioDispatcher) {
        megaLocalRoomGateway.setBackupAsOutdated(backupId)
    }

    override fun monitorCameraUploadsStatusInfo() =
        workManagerGateway.monitorCameraUploadsStatusInfo().mapNotNull { workInfoList ->
            with(workInfoList.first()) {
                cameraUploadsStatusInfoMapper(progress, state)
            }
        }.catch {
            Timber.e(it)
        }.flowOn(ioDispatcher)

    override fun monitorCameraUploadsSettingsActions() =
        appEventGateway.monitorCameraUploadsSettingsActions()

    override suspend fun broadCastCameraUploadSettingsActions(action: CameraUploadsSettingsAction) =
        appEventGateway.broadCastCameraUploadSettingsActions(action)

    override fun getCameraUploadsName() = context.getString(R.string.section_photo_sync)

    override fun getMediaUploadsName() = context.getString(R.string.section_secondary_media_uploads)

    override fun getMediaSelectionQuery(parentPath: String): String =
        cameraUploadsMediaGateway.getMediaSelectionQuery(parentPath)

    override suspend fun insertOrUpdateCameraUploadsRecords(records: List<CameraUploadsRecord>) =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.insertOrUpdateCameraUploadsRecords(records)
        }

    override suspend fun getAllCameraUploadsRecords() =
        withContext(ioDispatcher) {
            megaLocalRoomGateway.getAllCameraUploadsRecords()
        }

    override suspend fun getCameraUploadsRecordsBy(
        uploadStatus: List<CameraUploadsRecordUploadStatus>,
        types: List<CameraUploadsRecordType>,
        folderTypes: List<CameraUploadFolderType>,
    ) = withContext(ioDispatcher) {
        megaLocalRoomGateway
            .getCameraUploadsRecordsBy(uploadStatus, types, folderTypes)
    }

    override suspend fun setRecordUploadStatus(
        mediaId: Long,
        timestamp: Long,
        folderType: CameraUploadFolderType,
        uploadStatus: CameraUploadsRecordUploadStatus,
    ) = withContext(ioDispatcher) {
        megaLocalRoomGateway.updateCameraUploadsRecordUploadStatus(
            mediaId,
            timestamp,
            folderType,
            uploadStatus,
        )
    }

    override suspend fun setRecordGeneratedFingerprint(
        mediaId: Long,
        timestamp: Long,
        folderType: CameraUploadFolderType,
        generatedFingerprint: String,
    ) = withContext(ioDispatcher) {
        megaLocalRoomGateway.setCameraUploadsRecordGeneratedFingerprint(
            mediaId,
            timestamp,
            folderType,
            generatedFingerprint,
        )
    }

    override suspend fun clearRecords(
        folderTypes: List<CameraUploadFolderType>,
    ) = withContext(ioDispatcher) {
        megaLocalRoomGateway.deleteCameraUploadsRecords(folderTypes)
    }

    private companion object {
        private const val SUB_STATE_NO_CHANGE = -1
        private const val TARGET_NODE_NO_CHANGE = -1L
    }
}
