package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.BroadcastReceiverGateway
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.CameraUploadMediaGateway
import mega.privacy.android.data.gateway.FileAttributeGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.MediaStoreFileTypeUriMapper
import mega.privacy.android.data.mapper.SyncRecordTypeIntMapper
import mega.privacy.android.data.mapper.SyncStatusIntMapper
import mega.privacy.android.data.mapper.VideoQualityMapper
import mega.privacy.android.domain.entity.CameraUploadMedia
import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.exception.LocalStorageException
import mega.privacy.android.domain.exception.UnknownException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.CameraUploadRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import java.io.IOException
import java.util.Queue
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts
import kotlin.coroutines.Continuation

/**
 * Default implementation of [CameraUploadRepository]
 *
 * @property localStorageGateway [MegaLocalStorageGateway]
 * @property megaApiGateway [MegaApiGateway]
 * @property fileAttributeGateway [FileAttributeGateway]
 * @property cameraUploadMediaGateway [CameraUploadMediaGateway]
 * @property cacheGateway [CacheGateway]
 * @property syncRecordTypeIntMapper [SyncRecordTypeIntMapper]
 * @property mediaStoreFileTypeUriMapper [MediaStoreFileTypeUriMapper]
 * @property ioDispatcher CoroutineDispatcher
 */
@OptIn(ExperimentalContracts::class)
internal class DefaultCameraUploadRepository @Inject constructor(
    private val localStorageGateway: MegaLocalStorageGateway,
    private val megaApiGateway: MegaApiGateway,
    private val fileAttributeGateway: FileAttributeGateway,
    private val cameraUploadMediaGateway: CameraUploadMediaGateway,
    private val cacheGateway: CacheGateway,
    private val syncRecordTypeIntMapper: SyncRecordTypeIntMapper,
    private val mediaStoreFileTypeUriMapper: MediaStoreFileTypeUriMapper,
    private val appEventGateway: AppEventGateway,
    private val broadcastReceiverGateway: BroadcastReceiverGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val videoQualityMapper: VideoQualityMapper,
    private val syncStatusIntMapper: SyncStatusIntMapper,
) : CameraUploadRepository {

    override fun getInvalidHandle(): Long = megaApiGateway.getInvalidHandle()

    override suspend fun getPrimarySyncHandle(): Long? = withContext(ioDispatcher) {
        localStorageGateway.getCamSyncHandle()
    }

    override suspend fun getSecondarySyncHandle(): Long? = withContext(ioDispatcher) {
        localStorageGateway.getMegaHandleSecondaryFolder()
    }

    override suspend fun setPrimarySyncHandle(primaryHandle: Long) = withContext(ioDispatcher) {
        localStorageGateway.setCamSyncHandle(primaryHandle)
    }

    override suspend fun setSecondarySyncHandle(secondaryHandle: Long) = withContext(ioDispatcher) {
        localStorageGateway.setMegaHandleSecondaryFolder(secondaryHandle)
    }

    override suspend fun isSyncByWifi() = withContext(ioDispatcher) {
        localStorageGateway.isSyncByWifi()
    }

    override suspend fun isSyncByWifiDefault() = withContext(ioDispatcher) {
        localStorageGateway.isSyncByWifiDefault()
    }

    override suspend fun getPendingSyncRecords(): List<SyncRecord> = withContext(ioDispatcher) {
        localStorageGateway.getPendingSyncRecords()
    }

    override suspend fun setPhotosSyncFileUpload() = withContext(ioDispatcher) {
        localStorageGateway.setPhotosSyncUpload()
    }

    override suspend fun getSyncFileUpload(): String? = withContext(ioDispatcher) {
        localStorageGateway.getCameraSyncFileUpload()
    }

    override suspend fun getVideoQuality(): String = withContext(ioDispatcher) {
        localStorageGateway.getVideoQuality()
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

    override suspend fun isSyncEnabled(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.isSyncEnabled()
    }

    override suspend fun getSyncLocalPath(): String? = withContext(ioDispatcher) {
        localStorageGateway.getSyncLocalPath()
    }

    override suspend fun setSyncLocalPath(localPath: String) = withContext(ioDispatcher) {
        localStorageGateway.setSyncLocalPath(localPath)
    }

    override suspend fun setSecondaryFolderPath(secondaryFolderPath: String) =
        withContext(ioDispatcher) {
            localStorageGateway.setSecondaryFolderPath(secondaryFolderPath)
        }

    override suspend fun setPrimaryFolderHandle(primaryHandle: Long) = withContext(ioDispatcher) {
        localStorageGateway.setPrimaryFolderHandle(primaryHandle)
    }

    override suspend fun setSecondaryFolderHandle(secondaryHandle: Long) =
        withContext(ioDispatcher) {
            localStorageGateway.setSecondaryFolderHandle(secondaryHandle)
        }

    override suspend fun setSecondaryEnabled(secondaryCameraUpload: Boolean) =
        withContext(ioDispatcher) {
            localStorageGateway.setSecondaryEnabled(secondaryCameraUpload)
        }

    override suspend fun getSecondaryFolderPath(): String? = withContext(ioDispatcher) {
        localStorageGateway.getSecondaryFolderPath()
    }

    override suspend fun getRemoveGpsDefault(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.getRemoveGpsDefault()
    }

    override suspend fun getUploadVideoQuality(): VideoQuality? = withContext(ioDispatcher) {
        videoQualityMapper(localStorageGateway.getUploadVideoQuality())
    }

    override suspend fun getKeepFileNames(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.getKeepFileNames()
    }

    override suspend fun isFolderExternalSd(): Boolean = withContext(ioDispatcher) {
        localStorageGateway.isFolderExternalSd()
    }

    override suspend fun getUriExternalSd(): String? = withContext(ioDispatcher) {
        localStorageGateway.getUriExternalSd()
    }

    override suspend fun isSecondaryMediaFolderEnabled() = withContext(ioDispatcher) {
        localStorageGateway.isSecondaryMediaFolderEnabled()
    }

    override suspend fun isMediaFolderExternalSd() = withContext(ioDispatcher) {
        localStorageGateway.isMediaFolderExternalSd()
    }

    override suspend fun getUriMediaFolderExternalSd(): String? = withContext(ioDispatcher) {
        localStorageGateway.getUriMediaFolderExternalSd()
    }

    override suspend fun shouldClearSyncRecords() = withContext(ioDispatcher) {
        localStorageGateway.shouldClearSyncRecords()
    }

    override suspend fun sendUpdateFolderIconBroadcast(nodeHandle: Long, isSecondary: Boolean) =
        withContext(ioDispatcher) {
            cameraUploadMediaGateway.sendUpdateFolderIconBroadcast(nodeHandle, isSecondary)
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

    override suspend fun getChargingOnSizeString(): String = withContext(ioDispatcher) {
        localStorageGateway.getChargingOnSizeString()
    }

    override suspend fun getChargingOnSize() = withContext(ioDispatcher) {
        localStorageGateway.getChargingOnSizeString().toInt()
    }

    override suspend fun convertOnCharging() = withContext(ioDispatcher) {
        localStorageGateway.convertOnCharging()
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
            continuation.failWithError(error)
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

    override suspend fun broadcastUploadPauseState() = appEventGateway.broadcastUploadPauseState()

    override fun monitorBatteryInfo() = broadcastReceiverGateway.monitorBatteryInfo

    override fun monitorChargingStoppedInfo() = broadcastReceiverGateway.monitorChargingStoppedState

    override suspend fun setCameraUploadsFolders(primaryFolder: Long, secondaryFolder: Long) =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener { return@getRequestListener }
                megaApiGateway.setCameraUploadsFolders(primaryFolder, secondaryFolder, listener)
                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }
        }

    override suspend fun renameNode(nodeHandle: Long, newName: String): Unit =
        withContext(ioDispatcher) {
            val node = megaApiGateway.getMegaNodeByHandle(nodeHandle)
            node?.let {
                suspendCancellableCoroutine { continuation ->
                    val listener = continuation.getRequestListener { return@getRequestListener }
                    megaApiGateway.renameNode(it, newName, listener)
                    continuation.invokeOnCancellation {
                        megaApiGateway.removeRequestListener(listener)
                    }
                }
            }
        }

    @Deprecated("Function related to statistics will be reviewed in future updates to\n" +
            "     * provide more data and avoid race conditions. They could change or be removed in the current form.")
    override fun getNumberOfPendingUploads() = megaApiGateway.numberOfPendingUploads
}
