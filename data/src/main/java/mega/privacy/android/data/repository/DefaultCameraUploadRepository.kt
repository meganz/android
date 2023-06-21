package mega.privacy.android.data.repository

import android.content.Context
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.AndroidDeviceGateway
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.CameraUploadMediaGateway
import mega.privacy.android.data.gateway.FileAttributeGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.VideoCompressorGateway
import mega.privacy.android.data.gateway.WorkerGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.MediaStoreFileTypeUriMapper
import mega.privacy.android.data.mapper.SyncStatusIntMapper
import mega.privacy.android.data.mapper.VideoAttachmentMapper
import mega.privacy.android.data.mapper.VideoQualityIntMapper
import mega.privacy.android.data.mapper.VideoQualityMapper
import mega.privacy.android.data.mapper.camerauploads.BackupStateIntMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsHandlesMapper
import mega.privacy.android.data.mapper.camerauploads.HeartbeatStatusIntMapper
import mega.privacy.android.data.mapper.camerauploads.SyncRecordTypeIntMapper
import mega.privacy.android.data.mapper.camerauploads.UploadOptionIntMapper
import mega.privacy.android.data.mapper.camerauploads.UploadOptionMapper
import mega.privacy.android.data.worker.NewMediaWorker
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.CameraUploadFolderIconUpdate
import mega.privacy.android.domain.entity.CameraUploadMedia
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.exception.LocalStorageException
import mega.privacy.android.domain.exception.UnknownException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.CameraUploadRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import java.io.IOException
import java.util.Queue
import javax.inject.Inject
import kotlin.coroutines.Continuation

/**
 * Default implementation of [CameraUploadRepository]
 *
 * @property localStorageGateway [MegaLocalStorageGateway]
 * @property megaApiGateway [MegaApiGateway]
 * @property fileAttributeGateway [FileAttributeGateway]
 * @property fileGateway [FileGateway]
 * @property cameraUploadMediaGateway [CameraUploadMediaGateway]
 * @property cacheGateway [CacheGateway]
 * @property syncRecordTypeIntMapper [SyncRecordTypeIntMapper]
 * @property heartbeatStatusIntMapper [HeartbeatStatusIntMapper]
 * @property mediaStoreFileTypeUriMapper [MediaStoreFileTypeUriMapper]
 * @property ioDispatcher [CoroutineDispatcher]
 * @property appEventGateway [AppEventGateway]
 * @property workerGateway [WorkerGateway]
 * @property videoQualityMapper [VideoQualityMapper]
 * @property syncStatusIntMapper [SyncStatusIntMapper]
 * @property backupStateIntMapper [BackupStateIntMapper]
 * @property cameraUploadsHandlesMapper [CameraUploadsHandlesMapper]
 * @property uploadOptionMapper [UploadOptionMapper]
 * @property uploadOptionIntMapper [UploadOptionIntMapper]
 */
internal class DefaultCameraUploadRepository @Inject constructor(
    private val localStorageGateway: MegaLocalStorageGateway,
    private val megaApiGateway: MegaApiGateway,
    private val fileAttributeGateway: FileAttributeGateway,
    private val fileGateway: FileGateway,
    private val cameraUploadMediaGateway: CameraUploadMediaGateway,
    private val cacheGateway: CacheGateway,
    private val syncRecordTypeIntMapper: SyncRecordTypeIntMapper,
    private val heartbeatStatusIntMapper: HeartbeatStatusIntMapper,
    private val mediaStoreFileTypeUriMapper: MediaStoreFileTypeUriMapper,
    private val appEventGateway: AppEventGateway,
    private val workerGateway: WorkerGateway,
    private val videoQualityIntMapper: VideoQualityIntMapper,
    private val videoQualityMapper: VideoQualityMapper,
    private val syncStatusIntMapper: SyncStatusIntMapper,
    private val backupStateIntMapper: BackupStateIntMapper,
    private val cameraUploadsHandlesMapper: CameraUploadsHandlesMapper,
    private val videoCompressorGateway: VideoCompressorGateway,
    private val videoAttachmentMapper: VideoAttachmentMapper,
    private val uploadOptionMapper: UploadOptionMapper,
    private val uploadOptionIntMapper: UploadOptionIntMapper,
    private val deviceGateway: AndroidDeviceGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @ApplicationContext private val context: Context,
) : CameraUploadRepository {

    override fun getInvalidHandle(): Long = megaApiGateway.getInvalidHandle()

    override fun getInvalidBackupType(): Int = megaApiGateway.getInvalidBackupType()

    override suspend fun getPrimarySyncHandle(): Long? = withContext(ioDispatcher) {
        localStorageGateway.getCamSyncHandle()
    }

    override suspend fun getSecondarySyncHandle(): Long? = withContext(ioDispatcher) {
        localStorageGateway.getMegaHandleSecondaryFolder()
    }

    override suspend fun setPrimarySyncHandle(primaryHandle: Long) = withContext(ioDispatcher) {
        localStorageGateway.setPrimarySyncHandle(primaryHandle)
    }

    override suspend fun setSecondarySyncHandle(secondaryHandle: Long) = withContext(ioDispatcher) {
        localStorageGateway.setSecondarySyncHandle(secondaryHandle)
    }

    override suspend fun isCameraUploadsByWifi() = withContext(ioDispatcher) {
        localStorageGateway.isCameraUploadsByWifi()
    }

    override suspend fun setCameraUploadsByWifi(wifiOnly: Boolean) {
        localStorageGateway.setCameraUploadsByWifi(wifiOnly)
    }

    override suspend fun getPendingSyncRecords(): List<SyncRecord> = withContext(ioDispatcher) {
        localStorageGateway.getPendingSyncRecords()
    }

    override suspend fun getUploadOption() = withContext(ioDispatcher) {
        uploadOptionMapper(localStorageGateway.getCameraSyncFileUpload())
    }

    override suspend fun setUploadOption(uploadOption: UploadOption) = withContext(ioDispatcher) {
        localStorageGateway.setCameraSyncFileUpload(uploadOptionIntMapper(uploadOption))
    }

    override suspend fun deleteAllSyncRecords(syncRecordType: SyncRecordType) =
        withContext(ioDispatcher) {
            localStorageGateway.deleteAllSyncRecords(syncRecordTypeIntMapper(syncRecordType))
        }

    override suspend fun deleteSyncRecord(path: String?, isSecondary: Boolean) =
        withContext(ioDispatcher) {
            localStorageGateway.deleteSyncRecordByPath(path, isSecondary)
        }

    override suspend fun deleteSyncRecordByLocalPath(localPath: String?, isSecondary: Boolean) =
        withContext(ioDispatcher) {
            localStorageGateway.deleteSyncRecordByLocalPath(localPath, isSecondary)
        }

    override suspend fun deleteSyncRecordByFingerprint(
        originalPrint: String,
        newPrint: String,
        isSecondary: Boolean,
    ) =
        withContext(ioDispatcher) {
            localStorageGateway.deleteSyncRecordByFingerPrint(
                originalPrint,
                newPrint,
                isSecondary
            )
        }

    override suspend fun getSyncRecordByFingerprint(
        fingerprint: String?,
        isSecondary: Boolean,
        isCopy: Boolean,
    ): SyncRecord? =
        withContext(ioDispatcher) {
            localStorageGateway.getSyncRecordByFingerprint(fingerprint, isSecondary, isCopy)
        }

    override suspend fun getSyncRecordByNewPath(path: String): SyncRecord? =
        withContext(ioDispatcher) {
            localStorageGateway.getSyncRecordByNewPath(path)
        }

    override suspend fun getSyncRecordByLocalPath(path: String, isSecondary: Boolean): SyncRecord? =
        withContext(ioDispatcher) {
            localStorageGateway.getSyncRecordByLocalPath(path, isSecondary)
        }

    override suspend fun doesFileNameExist(
        fileName: String,
        isSecondary: Boolean,
        type: SyncRecordType,
    ): Boolean = withContext(ioDispatcher) {
        localStorageGateway.doesFileNameExist(fileName, isSecondary, syncRecordTypeIntMapper(type))
    }

    override suspend fun doesLocalPathExist(
        fileName: String,
        isSecondary: Boolean,
        type: SyncRecordType,
    ): Boolean = withContext(ioDispatcher) {
        localStorageGateway.doesLocalPathExist(fileName, isSecondary, syncRecordTypeIntMapper(type))
    }

    override suspend fun saveSyncRecord(record: SyncRecord) = withContext(ioDispatcher) {
        localStorageGateway.saveSyncRecord(record)
    }

    override suspend fun getSyncTimeStamp(type: SyncTimeStamp): Long? {
        return withContext(ioDispatcher) {
            when (type) {
                SyncTimeStamp.PRIMARY_PHOTO -> localStorageGateway.getPhotoTimeStamp()
                    ?.toLongOrNull()

                SyncTimeStamp.PRIMARY_VIDEO -> localStorageGateway.getVideoTimeStamp()
                    ?.toLongOrNull()

                SyncTimeStamp.SECONDARY_PHOTO -> localStorageGateway.getSecondaryPhotoTimeStamp()
                    ?.toLongOrNull()

                SyncTimeStamp.SECONDARY_VIDEO -> localStorageGateway.getSecondaryVideoTimeStamp()
                    ?.toLongOrNull()
            }
        }
    }

    override suspend fun setSyncTimeStamp(timeStamp: Long, type: SyncTimeStamp) {
        try {
            withContext(ioDispatcher) {
                when (type) {
                    SyncTimeStamp.PRIMARY_PHOTO -> localStorageGateway.setPhotoTimeStamp(timeStamp)
                    SyncTimeStamp.PRIMARY_VIDEO -> localStorageGateway.setVideoTimeStamp(timeStamp)
                    SyncTimeStamp.SECONDARY_PHOTO -> localStorageGateway.setSecondaryPhotoTimeStamp(
                        timeStamp
                    )

                    SyncTimeStamp.SECONDARY_VIDEO -> localStorageGateway.setSecondaryVideoTimeStamp(
                        timeStamp
                    )
                }
            }
        } catch (e: IOException) {
            Timber.e(e)
            throw LocalStorageException(e.message, e.cause)
        } catch (e: Exception) {
            Timber.e(e)
            throw UnknownException(e.message, e.cause)
        }
    }

    override suspend fun doCredentialsExist(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.doCredentialsExist()
    }

    override suspend fun doPreferencesExist(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.doPreferencesExist()
    }

    override suspend fun doesSyncEnabledExist(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.doesSyncEnabledExist()
    }

    override suspend fun isCameraUploadsEnabled(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.isCameraUploadsEnabled()
    }

    override suspend fun setCameraUploadsEnabled(enable: Boolean) {
        localStorageGateway.setCameraUploadsEnabled(enable)
    }

    override suspend fun getPrimaryFolderLocalPath(): String = withContext(ioDispatcher) {
        localStorageGateway.getPrimaryFolderLocalPath()
    }

    override suspend fun setPrimaryFolderLocalPath(localPath: String) = withContext(ioDispatcher) {
        localStorageGateway.setPrimaryFolderLocalPath(localPath)
    }

    override suspend fun setSecondaryFolderLocalPath(localPath: String) =
        withContext(ioDispatcher) {
            localStorageGateway.setSecondaryFolderLocalPath(localPath)
        }

    override suspend fun setSecondaryEnabled(secondaryCameraUpload: Boolean) =
        withContext(ioDispatcher) {
            localStorageGateway.setSecondaryEnabled(secondaryCameraUpload)
        }

    override suspend fun getSecondaryFolderLocalPath() = withContext(ioDispatcher) {
        localStorageGateway.getSecondaryFolderLocalPath()
    }

    override suspend fun areLocationTagsEnabled(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.areLocationTagsEnabled()
    }

    override suspend fun setLocationTagsEnabled(enable: Boolean) = withContext(ioDispatcher) {
        localStorageGateway.setLocationTagsEnabled(enable)
    }

    override suspend fun getUploadVideoQuality(): VideoQuality? = withContext(ioDispatcher) {
        videoQualityMapper(localStorageGateway.getUploadVideoQuality())
    }

    override suspend fun setUploadVideoQuality(videoQuality: VideoQuality) =
        withContext(ioDispatcher) {
            localStorageGateway.setUploadVideoQuality(videoQualityIntMapper(videoQuality))
        }

    override suspend fun setUploadVideoSyncStatus(syncStatus: SyncStatus) =
        withContext(ioDispatcher) {
            localStorageGateway.setUploadVideoSyncStatus(syncStatusIntMapper(syncStatus))
        }

    override suspend fun areUploadFileNamesKept(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.areUploadFileNamesKept()
    }

    override suspend fun setUploadFileNamesKept(keepFileNames: Boolean) =
        withContext(ioDispatcher) {
            localStorageGateway.setUploadFileNamesKept(keepFileNames)
        }

    override suspend fun isPrimaryFolderInSDCard(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.isPrimaryFolderInSDCard()
    }

    override suspend fun setPrimaryFolderInSDCard(isInSDCard: Boolean) = withContext(ioDispatcher) {
        localStorageGateway.setPrimaryFolderInSDCard(isInSDCard)
    }

    override suspend fun getPrimaryFolderSDCardUriPath() = withContext(ioDispatcher) {
        localStorageGateway.getPrimaryFolderSDCardUriPath()
    }

    override suspend fun setPrimaryFolderSDCardUriPath(path: String) = withContext(ioDispatcher) {
        localStorageGateway.setPrimaryFolderSDCardUriPath(path)
    }

    override suspend fun isSecondaryMediaFolderEnabled() = withContext(ioDispatcher) {
        localStorageGateway.isSecondaryMediaFolderEnabled()
    }

    override suspend fun isSecondaryFolderInSDCard() = withContext(ioDispatcher) {
        localStorageGateway.isSecondaryFolderInSDCard()
    }

    override suspend fun getSecondaryFolderSDCardUriPath(): String = withContext(ioDispatcher) {
        localStorageGateway.getSecondaryFolderSDCardUriPath()
    }

    override suspend fun setSecondaryFolderSDCardUriPath(path: String) = withContext(ioDispatcher) {
        localStorageGateway.setSecondaryFolderSDCardUriPath(path)
    }

    override suspend fun shouldClearSyncRecords() = withContext(ioDispatcher) {
        localStorageGateway.shouldClearSyncRecords()
    }

    @Deprecated(
        "Replace with data flow after refactoring of CameraUploadsPreferencesActivity ",
        replaceWith = ReplaceWith("BroadcastCameraUploadFolderIconUpdateUseCase")
    )
    override suspend fun sendUpdateFolderIconBroadcast(nodeHandle: Long, isSecondary: Boolean) =
        withContext(ioDispatcher) {
            cameraUploadMediaGateway.sendUpdateFolderIconBroadcast(nodeHandle, isSecondary)
            appEventGateway.broadcastCameraUploadFolderIconUpdate(
                CameraUploadFolderIconUpdate(
                    nodeHandle = nodeHandle,
                    cameraUploadFolderType = if (isSecondary) {
                        CameraUploadFolderType.Secondary
                    } else {
                        CameraUploadFolderType.Primary
                    }
                )
            )
        }

    override suspend fun sendUpdateFolderDestinationBroadcast(
        nodeHandle: Long,
        isSecondary: Boolean,
    ) = withContext(ioDispatcher) {
        cameraUploadMediaGateway.sendUpdateFolderDestinationBroadcast(nodeHandle, isSecondary)
    }

    override suspend fun getMediaQueue(
        mediaStoreFileType: MediaStoreFileType,
        parentPath: String?,
        isVideo: Boolean,
        selectionQuery: String?,
    ): Queue<CameraUploadMedia> = withContext(ioDispatcher) {
        val queue = cameraUploadMediaGateway.getMediaQueue(
            mediaStoreFileTypeUriMapper(mediaStoreFileType),
            parentPath,
            isVideo,
            selectionQuery
        )
        Timber.d("$mediaStoreFileType count from media store database: ${queue.size}")
        queue
    }

    override suspend fun getMaxTimestamp(
        isSecondary: Boolean,
        syncRecordType: SyncRecordType,
    ): Long =
        withContext(ioDispatcher) {
            localStorageGateway.getMaxTimestamp(
                isSecondary,
                syncRecordTypeIntMapper(syncRecordType)
            )
        }

    override suspend fun getVideoSyncRecordsByStatus(syncStatusType: SyncStatus): List<SyncRecord> =
        withContext(ioDispatcher) {
            localStorageGateway.getVideoSyncRecordsByStatus(syncStatusIntMapper(syncStatusType))
        }

    override suspend fun isChargingRequiredForVideoCompression() = withContext(ioDispatcher) {
        localStorageGateway.isChargingRequiredForVideoCompression()
    }

    override suspend fun setChargingRequiredForVideoCompression(chargingRequired: Boolean) =
        withContext(ioDispatcher) {
            localStorageGateway.setChargingRequiredForVideoCompression(chargingRequired)
        }

    override suspend fun getVideoCompressionSizeLimit() = withContext(ioDispatcher) {
        localStorageGateway.getVideoCompressionSizeLimit()
    }

    override suspend fun setVideoCompressionSizeLimit(size: Int) = withContext(ioDispatcher) {
        localStorageGateway.setVideoCompressionSizeLimit(size)
    }

    override suspend fun updateSyncRecordStatusByLocalPath(
        syncStatusType: Int,
        localPath: String?,
        isSecondary: Boolean,
    ) = withContext(ioDispatcher) {
        localStorageGateway.updateSyncRecordStatusByLocalPath(
            syncStatusType,
            localPath,
            isSecondary
        )
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

    override suspend fun getVideoGPSCoordinates(filePath: String): Pair<Float, Float> =
        withContext(ioDispatcher) {
            fileAttributeGateway.getVideoGPSCoordinates(filePath)
        }

    override suspend fun getPhotoGPSCoordinates(filePath: String): Pair<Float, Float> =
        withContext(ioDispatcher) {
            fileAttributeGateway.getPhotoGPSCoordinates(filePath)
        }

    override suspend fun saveShouldClearCamSyncRecords(clearCamSyncRecords: Boolean) =
        withContext(ioDispatcher) {
            localStorageGateway.saveShouldClearCamSyncRecords(clearCamSyncRecords)
        }

    override suspend fun clearCacheDirectory() = withContext(ioDispatcher) {
        cacheGateway.clearCacheDirectory()
    }

    override suspend fun deleteAllPrimarySyncRecords() = withContext(ioDispatcher) {
        localStorageGateway.deleteAllPrimarySyncRecords()
    }

    override suspend fun deleteAllSecondarySyncRecords() = withContext(ioDispatcher) {
        localStorageGateway.deleteAllSecondarySyncRecords()
    }

    override suspend fun convertBase64ToHandle(base64: String): Long = withContext(ioDispatcher) {
        megaApiGateway.base64ToHandle(base64)
    }

    override fun monitorCameraUploadPauseState() = appEventGateway.monitorCameraUploadPauseState

    override fun monitorCameraUploadProgress(): Flow<Pair<Int, Int>> =
        appEventGateway.monitorCameraUploadProgress

    override suspend fun broadcastUploadPauseState() = appEventGateway.broadcastUploadPauseState()

    override suspend fun broadcastCameraUploadProgress(progress: Int, pending: Int) =
        appEventGateway.broadcastCameraUploadProgress(progress, pending)

    override fun monitorCameraUploadFolderIconUpdate(): Flow<CameraUploadFolderIconUpdate> =
        appEventGateway.monitorCameraUploadFolderIconUpdate()

    override suspend fun broadcastCameraUploadFolderIconUpdate(data: CameraUploadFolderIconUpdate) =
        appEventGateway.broadcastCameraUploadFolderIconUpdate(data)

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

    override suspend fun fireCameraUploadJob() = withContext(ioDispatcher) {
        workerGateway.fireCameraUploadJob()
    }

    override suspend fun fireStopCameraUploadJob(aborted: Boolean) = withContext(ioDispatcher) {
        workerGateway.fireStopCameraUploadJob(aborted)
    }

    override suspend fun scheduleCameraUploadJob() = withContext(ioDispatcher) {
        workerGateway.scheduleCameraUploadJob()
    }

    override suspend fun rescheduleCameraUpload() = withContext(ioDispatcher) {
        workerGateway.rescheduleCameraUpload()
    }

    override suspend fun stopCameraUploadSyncHeartbeatWorkers() = withContext(ioDispatcher) {
        workerGateway.cancelCameraUploadAndHeartbeatWorkRequest()
    }

    override fun compressVideos(
        root: String,
        quality: VideoQuality,
        records: List<SyncRecord>,
    ): Flow<VideoCompressionState> {
        videoCompressorGateway.run {
            setOutputRoot(root)
            setVideoQuality(quality)
            addItems(videoAttachmentMapper(records))
        }
        return videoCompressorGateway.start().flowOn(ioDispatcher)
    }

    override suspend fun listenToNewMedia() {
        withContext(ioDispatcher) {
            if (isCameraUploadsEnabled()) {
                NewMediaWorker.scheduleWork(context, false)
            }
        }
    }

    override suspend fun getCuBackUpId() = withContext(ioDispatcher) {
        localStorageGateway.getCuBackUpId()
    }

    override suspend fun getMuBackUpId() = withContext(ioDispatcher) {
        localStorageGateway.getMuBackUpId()
    }

    override suspend fun sendBackupHeartbeat(
        backupId: Long,
        heartbeatStatus: HeartbeatStatus,
        ups: Int,
        downs: Int,
        ts: Long,
        lastNode: Long,
    ) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("sendBackupHeartbeat") { }
            megaApiGateway.sendBackupHeartbeat(
                backupId,
                heartbeatStatus.value,
                heartbeatStatusIntMapper(heartbeatStatus),
                ups,
                downs,
                ts,
                lastNode,
                listener
            )
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }
    }

    override suspend fun updateBackup(
        backupId: Long,
        backupType: Int,
        targetNode: Long,
        localFolder: String?,
        backupName: String,
        state: BackupState,
    ) = withContext(ioDispatcher) {
        suspendCancellableCoroutine { continuation ->
            val listener = continuation.getRequestListener("updateBackup") { it.parentHandle }
            megaApiGateway.updateBackup(
                backupId,
                backupType,
                targetNode,
                localFolder,
                backupName,
                backupStateIntMapper(state),
                MegaError.API_OK,
                listener,
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
        localStorageGateway.getCuBackUp()
    }

    override suspend fun getMuBackUp() = withContext(ioDispatcher) {
        localStorageGateway.getMuBackUp()
    }

    override suspend fun getBackupById(id: Long) = withContext(ioDispatcher) {
        localStorageGateway.getBackupById(id)
    }

    override suspend fun updateLocalBackup(backup: Backup) = withContext(ioDispatcher) {
        localStorageGateway.updateBackup(backup)
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

    override fun getSelectionQuery(currentTimeStamp: Long, localPath: String) =
        """((${MediaStore.MediaColumns.DATE_MODIFIED}*1000) > $currentTimeStamp OR (${MediaStore.MediaColumns.DATE_ADDED}*1000) > $currentTimeStamp) AND ${MediaStore.MediaColumns.DATA} LIKE '${localPath}%'"""

    override suspend fun isCharging() = deviceGateway.isCharging().also {
        Timber.d("Is Device charging $it")
    }

    override suspend fun getBackupFolderId(cameraUploadFolderType: CameraUploadFolderType): Long? =
        withContext(ioDispatcher) {
            if (cameraUploadFolderType == CameraUploadFolderType.Primary) {
                localStorageGateway.getCuBackUpId()
            } else {
                localStorageGateway.getMuBackUpId()
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
        localStorageGateway.deleteBackupById(backupId)
    }

    override suspend fun setBackupAsOutdated(backupId: Long) = withContext(ioDispatcher) {
        localStorageGateway.setBackupAsOutdated(backupId)
    }
}
