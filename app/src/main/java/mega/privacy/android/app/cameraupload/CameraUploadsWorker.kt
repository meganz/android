package mega.privacy.android.app.cameraupload


import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mega.privacy.android.data.R
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.ARE_UPLOADS_PAUSED
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CHECK_FILE_UPLOAD
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.COMPRESSION_ERROR
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.COMPRESSION_PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CURRENT_FILE_INDEX
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CURRENT_PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.FOLDER_TYPE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.FOLDER_UNAVAILABLE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.NOT_ENOUGH_STORAGE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.OUT_OF_SPACE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.STATUS_INFO
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.STORAGE_OVER_QUOTA
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_COUNT
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_TO_UPLOAD
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_UPLOADED
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_UPLOADED_BYTES
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_UPLOAD_BYTES
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.USE_CAMERA_UPLOADS_RECORDS
import mega.privacy.android.data.featuretoggle.DataFeatures
import mega.privacy.android.data.wrapper.CameraUploadsNotificationManagerWrapper
import mega.privacy.android.data.wrapper.CookieEnabledCheckWrapper
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsState
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsTransferProgress
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.BroadcastCameraUploadProgress
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.CompressVideos
import mega.privacy.android.domain.usecase.CompressedVideoPending
import mega.privacy.android.domain.usecase.CreateCameraUploadFolder
import mega.privacy.android.domain.usecase.CreateCameraUploadTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.CreateTempFileAndRemoveCoordinatesUseCase
import mega.privacy.android.domain.usecase.DeleteSyncRecord
import mega.privacy.android.domain.usecase.DeleteSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetPendingSyncRecords
import mega.privacy.android.domain.usecase.GetVideoSyncRecordsByStatus
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.domain.usecase.IsNotEnoughQuota
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.IsWifiNotSatisfiedUseCase
import mega.privacy.android.domain.usecase.MonitorBatteryInfo
import mega.privacy.android.domain.usecase.MonitorChargingStoppedState
import mega.privacy.android.domain.usecase.ResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
import mega.privacy.android.domain.usecase.SetSyncRecordPendingByPath
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.backup.InitializeBackupsUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreCameraUploadsFoldersInRubbishBinUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.BroadcastCameraUploadsSettingsActionUseCase
import mega.privacy.android.domain.usecase.camerauploads.BroadcastStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.camerauploads.DeleteCameraUploadsTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.camerauploads.DoesCameraUploadsRecordExistsInTargetNodeUseCase
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.camerauploads.ExtractGpsCoordinatesUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetDefaultNodeHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPendingCameraUploadsRecordsUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadFolderHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.HandleLocalIpChangeUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsPrimaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsSecondaryFolderSetUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.camerauploads.ProcessCameraUploadsMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.ProcessMediaForUploadUseCase
import mega.privacy.android.domain.usecase.camerauploads.RenameCameraUploadsRecordsUseCase
import mega.privacy.android.domain.usecase.camerauploads.SendBackupHeartBeatSyncUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCoordinatesUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetOriginalFingerprintUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetSecondaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupPrimaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateCameraUploadsBackupHeartbeatStatusUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateCameraUploadsBackupStatesUseCase
import mega.privacy.android.domain.usecase.camerauploads.UploadCameraUploadsRecordsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.CreateImageOrVideoThumbnailUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DeletePreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DeleteThumbnailUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.completed.AddCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.CancelAllUploadTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.ResetTotalUploadsUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.StartUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.time.Instant
import java.util.Hashtable
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Worker to run Camera Uploads
 */
@HiltWorker
class CameraUploadsWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val isNotEnoughQuota: IsNotEnoughQuota,
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
    private val isPrimaryFolderPathValidUseCase: IsPrimaryFolderPathValidUseCase,
    private val isSecondaryFolderSetUseCase: IsSecondaryFolderSetUseCase,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val isWifiNotSatisfiedUseCase: IsWifiNotSatisfiedUseCase,
    private val deleteSyncRecord: DeleteSyncRecord,
    private val deleteSyncRecordByLocalPath: DeleteSyncRecordByLocalPath,
    private val deleteSyncRecordByFingerprint: DeleteSyncRecordByFingerprint,
    private val setPrimaryFolderLocalPathUseCase: SetPrimaryFolderLocalPathUseCase,
    private val shouldCompressVideo: ShouldCompressVideo,
    private val setSecondaryFolderLocalPathUseCase: SetSecondaryFolderLocalPathUseCase,
    private val clearSyncRecords: ClearSyncRecords,
    private val areLocationTagsEnabledUseCase: AreLocationTagsEnabledUseCase,
    private val getPendingSyncRecords: GetPendingSyncRecords,
    private val compressedVideoPending: CompressedVideoPending,
    private val getVideoSyncRecordsByStatus: GetVideoSyncRecordsByStatus,
    private val setSyncRecordPendingByPath: SetSyncRecordPendingByPath,
    private val isChargingRequired: IsChargingRequired,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val processMediaForUploadUseCase: ProcessMediaForUploadUseCase,
    private val getUploadFolderHandleUseCase: GetUploadFolderHandleUseCase,
    private val setPrimarySyncHandle: SetPrimarySyncHandle,
    private val setSecondarySyncHandle: SetSecondarySyncHandle,
    private val getDefaultNodeHandleUseCase: GetDefaultNodeHandleUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorPausedTransfersUseCase: MonitorPausedTransfersUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorBatteryInfo: MonitorBatteryInfo,
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase,
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase,
    private val monitorChargingStoppedState: MonitorChargingStoppedState,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val handleLocalIpChangeUseCase: HandleLocalIpChangeUseCase,
    private val cancelTransferByTagUseCase: CancelTransferByTagUseCase,
    private val cancelAllUploadTransfersUseCase: CancelAllUploadTransfersUseCase,
    private val copyNodeUseCase: CopyNodeUseCase,
    private val setOriginalFingerprintUseCase: SetOriginalFingerprintUseCase,
    private val startUploadUseCase: StartUploadUseCase,
    private val createCameraUploadFolder: CreateCameraUploadFolder,
    private val setupPrimaryFolderUseCase: SetupPrimaryFolderUseCase,
    private val setupSecondaryFolderUseCase: SetupSecondaryFolderUseCase,
    private val establishCameraUploadsSyncHandlesUseCase: EstablishCameraUploadsSyncHandlesUseCase,
    private val resetTotalUploadsUseCase: ResetTotalUploadsUseCase,
    private val compressVideos: CompressVideos,
    private val resetMediaUploadTimeStamps: ResetMediaUploadTimeStamps,
    private val disableMediaUploadSettings: DisableMediaUploadSettings,
    private val createCameraUploadTemporaryRootDirectoryUseCase: CreateCameraUploadTemporaryRootDirectoryUseCase,
    private val deleteCameraUploadsTemporaryRootDirectoryUseCase: DeleteCameraUploadsTemporaryRootDirectoryUseCase,
    private val broadcastCameraUploadProgress: BroadcastCameraUploadProgress,
    private val stopCameraUploadsUseCase: StopCameraUploadsUseCase,
    private val createTempFileAndRemoveCoordinatesUseCase: CreateTempFileAndRemoveCoordinatesUseCase,
    private val updateCameraUploadsBackupStatesUseCase: UpdateCameraUploadsBackupStatesUseCase,
    private val sendBackupHeartBeatSyncUseCase: SendBackupHeartBeatSyncUseCase,
    private val updateCameraUploadsBackupHeartbeatStatusUseCase: UpdateCameraUploadsBackupHeartbeatStatusUseCase,
    private val addCompletedTransferUseCase: AddCompletedTransferUseCase,
    private val setCoordinatesUseCase: SetCoordinatesUseCase,
    private val isChargingUseCase: IsChargingUseCase,
    private val monitorStorageOverQuotaUseCase: MonitorStorageOverQuotaUseCase,
    private val broadcastStorageOverQuotaUseCase: BroadcastStorageOverQuotaUseCase,
    private val createImageOrVideoThumbnailUseCase: CreateImageOrVideoThumbnailUseCase,
    private val createImageOrVideoPreviewUseCase: CreateImageOrVideoPreviewUseCase,
    private val deleteThumbnailUseCase: DeleteThumbnailUseCase,
    private val deletePreviewUseCase: DeletePreviewUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val cameraUploadsNotificationManagerWrapper: CameraUploadsNotificationManagerWrapper,
    private val hasMediaPermissionUseCase: HasMediaPermissionUseCase,
    private val cookieEnabledCheckWrapper: CookieEnabledCheckWrapper,
    private val broadcastCameraUploadsSettingsActionUseCase: BroadcastCameraUploadsSettingsActionUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val processCameraUploadsMediaUseCase: ProcessCameraUploadsMediaUseCase,
    private val getPendingCameraUploadsRecordsUseCase: GetPendingCameraUploadsRecordsUseCase,
    private val renameCameraUploadsRecordsUseCase: RenameCameraUploadsRecordsUseCase,
    private val doesCameraUploadsRecordExistsInTargetNodeUseCase: DoesCameraUploadsRecordExistsInTargetNodeUseCase,
    private val extractGpsCoordinatesUseCase: ExtractGpsCoordinatesUseCase,
    private val uploadCameraUploadsRecordsUseCase: UploadCameraUploadsRecordsUseCase,
    private val initializeBackupsUseCase: InitializeBackupsUseCase,
    private val areCameraUploadsFoldersInRubbishBinUseCase: AreCameraUploadsFoldersInRubbishBinUseCase,
    private val fileSystemRepository: FileSystemRepository,
    @LoginMutex private val loginMutex: Mutex,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val LOW_BATTERY_LEVEL = 20
        private const val ON_TRANSFER_UPDATE_REFRESH_MILLIS = 1000
        private const val CONCURRENT_UPLOADS_LIMIT = 16

        private const val APP_DATA_CU = "CU_UPLOAD"
        private const val INVALID_NON_NULL_VALUE = "-1"
    }

    /**
     * True if the battery level of the device is above
     * the required battery level to run the CU
     */
    private var deviceAboveMinimumBatteryLevel: Boolean = true

    /**
     * Temp root path used for generating temporary files in the CU process
     * This folder is created at the beginning of the CU process
     * and deleted at the end of the process
     */
    private lateinit var tempRoot: String

    /**
     * Camera Uploads State
     */
    private val _state = MutableStateFlow(CameraUploadsState())

    /**
     * Read-only Camera Uploads State
     */
    private val state: StateFlow<CameraUploadsState> = _state.asStateFlow()

    /**
     * Mutex for updating the state in a synchronized way
     * Only one coroutine can update the state at a time
     */
    private val stateUpdateMutex = Mutex()


    /**
     * Time in milliseconds to flag the last update of the notification
     * Used to prevent updating the notification too often
     */
    @Volatile
    private var lastUpdated: Long = 0

    /**
     * Dedicated job to encapsulate video compression process
     */
    private var videoCompressionJob: Job? = null

    /**
     * Total video size to upload in MB
     */
    private var totalVideoSize = 0L

    /**
     * Reference to the CoroutineWorker coroutine scope
     */
    private var scope: CoroutineScope? = null

    /**
     * Job to monitor upload pause flow
     */
    private var monitorUploadPauseStatusJob: Job? = null

    /**
     * Job to send backup heartbeat flow
     */
    private var sendBackupHeartbeatJob: Job? = null

    /**
     * Job to monitor connectivity status flow
     */
    private var monitorConnectivityStatusJob: Job? = null

    /**
     * Job to monitor battery level status flow
     */
    private var monitorBatteryLevelStatusJob: Job? = null

    /**
     * Job to monitor charging stopped status flow
     */
    private var monitorChargingStoppedStatusJob: Job? = null

    /**
     * Job to monitor transfer over quota status flow
     */
    private var monitorStorageOverQuotaStatusJob: Job? = null

    /**
     * Job to monitor deletion of parent nodes
     */
    private var monitorParentNodesDeletedJob: Job? = null

    /**
     * In order to not overload the memory of the app,
     * limit the number of concurrent uploads to [CONCURRENT_UPLOADS_LIMIT]
     */
    private val semaphore = Semaphore(CONCURRENT_UPLOADS_LIMIT)

    /**
     * Flag to check if upload process with camera uploads records is enabled
     */
    private var useCameraUploadsRecordsEnabled: Boolean = false

    override suspend fun doWork() = coroutineScope {
        try {
            Timber.d("Start CU Worker")
            scope = this

            // Signal to not kill the worker if the app is killed
            setForegroundAsync(getForegroundInfo())

            withContext(ioDispatcher) {
                useCameraUploadsRecordsEnabled =
                    getFeatureFlagValueUseCase(DataFeatures.UseCameraUploadsRecords)
                initService()
                if (hasMediaPermissionUseCase() && isLoginSuccessful() && canRunCameraUploads()) {
                    Timber.d("Calling startWorker() successful. Starting Camera Uploads")
                    cameraUploadsNotificationManagerWrapper.cancelNotifications()

                    if (useCameraUploadsRecordsEnabled) {
                        scanFiles()

                        val primaryUploadNodeId =
                            NodeId(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
                        val secondaryUploadNodeId =
                            NodeId(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary))

                        getAndPrepareRecords(
                            primaryUploadNodeId,
                            secondaryUploadNodeId
                        ).let { records ->
                            uploadFiles(
                                records,
                                primaryUploadNodeId,
                                secondaryUploadNodeId,
                                tempRoot
                            )
                        }
                    } else {
                        checkUploadNodes()
                        upload()
                        compressVideos()
                        uploadCompressedVideos()
                    }
                    endWork()
                    Result.success()
                } else {
                    abortWork(cancelMessage = "Required conditions to start CU not met")
                    Result.failure()
                }
            }
        } catch (throwable: Throwable) {
            Timber.e(throwable, "Worker cancelled")
            abortWork(cancelMessage = throwable.message ?: "Error caught at the root level")
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo() =
        cameraUploadsNotificationManagerWrapper.getForegroundInfo()

    private fun monitorUploadPauseStatus() {
        monitorUploadPauseStatusJob = scope?.launch(ioDispatcher) {
            monitorPausedTransfersUseCase().collect {
                updateBackupState(if (it) BackupState.PAUSE_UPLOADS else BackupState.ACTIVE)
                displayUploadProgress()
            }
        }
    }

    private fun monitorConnectivityStatus() {
        monitorConnectivityStatusJob = scope?.launch(ioDispatcher) {
            monitorConnectivityUseCase().collect {
                if (!it || isWifiNotSatisfiedUseCase()) {
                    abortWork(cancelMessage = "Camera Uploads by Wifi only")
                }
            }
        }
    }

    private fun monitorBatteryLevelStatus() {
        monitorBatteryLevelStatusJob = scope?.launch(ioDispatcher) {
            monitorBatteryInfo().collect {
                deviceAboveMinimumBatteryLevel = (it.level > LOW_BATTERY_LEVEL || it.isCharging)
                if (!deviceAboveMinimumBatteryLevel) {
                    abortWork(cancelMessage = "Low Battery Level")
                }
            }
        }
    }

    private fun monitorChargingStoppedStatus() {
        monitorChargingStoppedStatusJob = scope?.launch(ioDispatcher) {
            monitorChargingStoppedState().collect {
                if (isChargingRequired(totalVideoSize)) {
                    Timber.d("Detected device stops charging")
                    videoCompressionJob?.cancel()
                }
            }
        }
    }

    private fun monitorStorageOverQuotaStatus() {
        monitorStorageOverQuotaStatusJob = scope?.launch(ioDispatcher) {
            monitorStorageOverQuotaUseCase().collect {
                if (it) {
                    showStorageOverQuotaStatus()
                    abortWork(cancelMessage = "Storage Over Quota")
                }
            }
        }
    }

    private fun monitorParentNodesDeleted() {
        monitorParentNodesDeletedJob = scope?.launch(ioDispatcher) {
            monitorNodeUpdatesUseCase().collect { nodeUpdate ->
                val primaryHandle = getUploadFolderHandleUseCase(CameraUploadFolderType.Primary)
                val secondaryHandle = getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)

                val areCameraUploadsFoldersInRubbishBin =
                    areCameraUploadsFoldersInRubbishBinUseCase(
                        primaryHandle,
                        secondaryHandle,
                        nodeUpdate
                    )

                if (areCameraUploadsFoldersInRubbishBin) {
                    abortWork(
                        cancelMessage = "Parent nodes deleted",
                        restartMode = CameraUploadsRestartMode.RestartImmediately,
                    )
                }
            }
        }
    }


    /**
     * Cancels a pending [Transfer] through [CancelTransferByTagUseCase],
     * and call [resetTotalUploadsUseCase] after every cancellation to reset the total uploads if
     * there are no more pending uploads
     *
     * @param transfer the [Transfer] to be cancelled
     */
    private suspend fun cancelPendingTransfer(transfer: Transfer) {
        runCatching { cancelTransferByTagUseCase(transfer.tag) }
            .onSuccess {
                Timber.d("Transfer cancellation successful")
                resetTotalUploadsUseCase()
            }
            .onFailure { error -> Timber.e("Transfer cancellation error: $error") }
    }

    /**
     * Cancels all pending [Transfer] items through [CancelAllUploadTransfersUseCase],
     * and call [resetTotalUploadsUseCase] afterwards
     */
    private suspend fun cancelAllPendingTransfers() {
        runCatching { cancelAllUploadTransfersUseCase() }
            .onSuccess {
                Timber.d("Cancel all transfers successful")
                resetTotalUploadsUseCase()
            }
            .onFailure { error -> Timber.e("Cancel all transfers error: $error") }
    }

    /**
     * Checks if Camera Uploads can run by checking multiple conditions
     *
     * @return true if all conditions have been met, and false if otherwise
     */
    private suspend fun canRunCameraUploads(): Boolean =
        isStorageQuotaExceeded().not()
                && isCameraUploadsSyncEnabled()
                && isWifiConstraintSatisfied()
                && isDeviceAboveMinimumBatteryLevel()
                && isPrimaryFolderValid()
                && isSecondaryFolderConfigured()
                && areCameraUploadsSyncHandlesEstablished()
                && areFoldersCheckedAndEstablished()
                && isBackupInitialized()

    /**
     * Check if the account has enough cloud storage space
     *
     * @return true if the device has not enough cloud storage space
     */
    private suspend fun isStorageQuotaExceeded() = isNotEnoughQuota().also {
        Timber.d("isNotEnoughQuota $it")
    }

    private fun isDeviceAboveMinimumBatteryLevel() = deviceAboveMinimumBatteryLevel.also {
        Timber.d("Device Battery level above $it")
    }

    private suspend fun areCameraUploadsSyncHandlesEstablished(): Boolean {
        return runCatching {
            establishCameraUploadsSyncHandlesUseCase()
        }.onFailure {
            Timber.e(it)
        }.isSuccess
    }

    private suspend fun isBackupInitialized(): Boolean {
        Timber.d("Setting up Backup")
        return runCatching { initializeBackupsUseCase() }.onFailure {
            Timber.e(it)
        }.isSuccess
    }

    private suspend fun areFoldersCheckedAndEstablished(): Boolean {
        return if (areFoldersEstablished()) {
            true
        } else {
            runCatching { establishFolders() }.onFailure {
                Timber.e("Establishing Folder $it")
            }.isSuccess.also {
                Timber.d("Establish Folder $it")
            }
        }
    }


    private suspend fun isSecondaryFolderConfigured(): Boolean {
        if (isSecondaryFolderEnabled()) {
            return hasSecondaryFolder().also {
                if (!it) {
                    Timber.e("Local Secondary Folder is disabled.")
                    handleSecondaryFolderDisabled()
                }
            }
        }
        return true
    }

    /**
     * Checks if the Camera Uploads sync from [isCameraUploadsEnabledUseCase] is enabled
     *
     * @return true if enabled, and false if otherwise
     */
    private suspend fun isCameraUploadsSyncEnabled(): Boolean =
        isCameraUploadsEnabledUseCase().also {
            if (!it) Timber.w("Camera Uploads sync disabled")
        }

    /**
     * Checks if the Wi-Fi constraint from the negated [isWifiNotSatisfiedUseCase] is satisfied
     *
     * @return true if the Wi-Fi constraint is satisfied, and false if otherwise
     */
    private suspend fun isWifiConstraintSatisfied(): Boolean =
        !isWifiNotSatisfiedUseCase().also {
            if (it) Timber.w("Cannot start, Wi-Fi required")
        }

    /**
     * Checks if the Primary Folder exists and is valid
     *
     * @return true if the Primary Folder exists and is valid, and false if otherwise
     */
    private suspend fun isPrimaryFolderValid(): Boolean =
        isPrimaryFolderPathValidUseCase(getPrimaryFolderPathUseCase()).also {
            if (!it) {
                Timber.w("The Primary Folder does not exist or is invalid")
                handlePrimaryFolderDisabled()
            }
        }

    /**
     * Checks if the Secondary Folder from [isSecondaryFolderSetUseCase] exists
     *
     * @return true if it exists, and false if otherwise
     */
    private suspend fun hasSecondaryFolder(): Boolean =
        isSecondaryFolderSetUseCase().also {
            if (!it) Timber.w("The Secondary Folder is not set")
        }

    /**
     * Checks whether both Primary and Secondary Folders are established
     *
     * @return true if the Primary Folder exists and the Secondary Folder option disabled, or
     * if the Primary Folder exists and the Secondary Folder option enabled with the folder also existing.
     *
     * false if both conditions are not met
     */
    private suspend fun areFoldersEstablished(): Boolean {
        val isPrimaryFolderEstablished = isPrimaryFolderEstablished()
        val isSecondaryFolderEnabled = isSecondaryFolderEnabled()
        return (isPrimaryFolderEstablished && !isSecondaryFolderEnabled) ||
                (isPrimaryFolderEstablished && isSecondaryFolderEstablished())
    }

    /**
     * Checks whether the Primary Folder is established
     *
     * @return true if the Primary Folder handle is a valid handle, and false if otherwise
     */
    private suspend fun isPrimaryFolderEstablished(): Boolean {
        val primarySyncHandle = getUploadFolderHandleUseCase(CameraUploadFolderType.Primary)
        Timber.d("primarySyncHandle $primarySyncHandle")
        return !(primarySyncHandle == MegaApiJava.INVALID_HANDLE
                || isNodeInRubbishOrDeletedUseCase(primarySyncHandle))
    }

    /**
     * Checks whether the Secondary Folder is established
     *
     * @return true if the Secondary Folder handle is a valid handle, and false if otherwise
     */
    private suspend fun isSecondaryFolderEstablished(): Boolean {
        val secondarySyncHandle = getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)
        Timber.d("secondarySyncHandle $secondarySyncHandle")
        return !(secondarySyncHandle == MegaApiJava.INVALID_HANDLE
                || isNodeInRubbishOrDeletedUseCase(secondarySyncHandle))
    }

    /**
     * When the Primary Folder and Secondary Folder (if enabled) does not exist, this function will establish
     * the folders
     *
     * @throws Exception if the creation of one of the upload folder failed
     */
    private suspend fun establishFolders() {
        // Setup the Primary Folder if it is missing
        if (!isPrimaryFolderEstablished()) {
            Timber.w("The Primary Folder is missing")

            val primaryHandle =
                getDefaultNodeHandleUseCase(context.getString(R.string.section_photo_sync))

            if (primaryHandle == MegaApiJava.INVALID_HANDLE) {
                Timber.d("Proceed to create the Primary Folder")
                createAndSetupPrimaryUploadFolder()
            } else {
                Timber.d("Primary Handle retrieved from getPrimaryFolderHandle(): $primaryHandle")
                setPrimarySyncHandle(primaryHandle)
            }
        }

        // If Secondary Media Uploads is enabled, setup the Secondary Folder if it is missing
        if (isSecondaryFolderEnabled() && !isSecondaryFolderEstablished()) {
            Timber.w("The local secondary folder is missing")

            val secondaryHandle =
                getDefaultNodeHandleUseCase(context.getString(R.string.section_secondary_media_uploads))

            if (secondaryHandle == MegaApiJava.INVALID_HANDLE) {
                Timber.d("Proceed to create the Secondary Folder")
                createAndSetupSecondaryUploadFolder()
            } else {
                Timber.d("Secondary Handle retrieved from getSecondaryFolderHandle(): $secondaryHandle")
                setSecondarySyncHandle(secondaryHandle)
            }
        }
    }

    /**
     * Retrieve the files from the media store and insert them in the database
     */
    //@Karma
    private suspend fun scanFiles() {
        Timber.d("Get Pending Files from Media Store")
        showCheckUploadStatus()
        processCameraUploadsMediaUseCase(tempRoot = tempRoot)
    }

    /**
     * Get pending records from the database and populate them with required information for upload
     * - Retrieve the pending camera uploads records from the database
     * - Filter the camera uploads based on video compression size condition
     * - Rename the camera uploads records
     *
     * @param primaryUploadNodeId the primary target [NodeId]
     * @param secondaryUploadNodeId the secondary target [NodeId]
     * @return the list of pending [CameraUploadsRecord] to upload
     */
    //@Karma
    private suspend fun getAndPrepareRecords(
        primaryUploadNodeId: NodeId,
        secondaryUploadNodeId: NodeId,
    ): List<CameraUploadsRecord> {
        Timber.d("Get Pending Files from Database")
        return getPendingCameraUploadsRecords()
            .let { pendingRecords ->
                Timber.d("Check compression requirements")
                filterCameraUploadsRecords(pendingRecords)
            }
            .let { filteredRecords ->
                Timber.d("Renaming ${filteredRecords.size} files")
                renameCameraUploadsRecords(
                    filteredRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                )
            }
            .let { renamedRecords ->
                Timber.d("Retrieve existence in target node for ${renamedRecords.size} files")
                getExistenceInTargetNode(
                    renamedRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId
                )
            }
            .let { renamedRecordsWithExistenceInTargetNode ->
                Timber.d("Retrieve gps coordinates for ${renamedRecordsWithExistenceInTargetNode.size} files")
                getGpsCoordinates(renamedRecordsWithExistenceInTargetNode)
            }
    }

    /**
     * Upload the [CameraUploadsRecord]
     * The upload function will trigger a flow that is collected to handle the progress update
     *
     * @param records the list of [CameraUploadsRecord] to upload
     * @param primaryUploadNodeId the primary target [NodeId]
     * @param secondaryUploadNodeId the secondary target [NodeId]
     * @param tempRoot the root path of the temporary files
     */
    //@Karma
    private suspend fun uploadFiles(
        records: List<CameraUploadsRecord>,
        primaryUploadNodeId: NodeId,
        secondaryUploadNodeId: NodeId,
        tempRoot: String
    ) = coroutineScope {
        startHeartbeat()
        uploadCameraUploadsRecords(
            records,
            primaryUploadNodeId,
            secondaryUploadNodeId,
            tempRoot
        ).flowOn(ioDispatcher)
            .catch { throwable ->
                Timber.e(throwable)
                abortWork(throwable.message ?: "Error caught when uploading")
            }
            .collect { progressEvent ->
                launch(ioDispatcher) {
                    processProgressEvent(progressEvent)
                }
            }
    }


    /**
     * Retrieve the pending camera uploads records from the database
     *
     * @return a list of [CameraUploadsRecord]
     */
    private suspend fun getPendingCameraUploadsRecords(): List<CameraUploadsRecord> =
        getPendingCameraUploadsRecordsUseCase()

    /**
     * Filter the camera uploads records based on video compression size condition
     *
     * @return a list of [CameraUploadsRecord]
     */
    private suspend fun filterCameraUploadsRecords(records: List<CameraUploadsRecord>): List<CameraUploadsRecord> {
        val videoRecords =
            records.filter { it.type == SyncRecordType.TYPE_VIDEO }
        totalVideoSize = getTotalVideoSizeInMB(videoRecords.map { it.filePath })
        Timber.d("Total videos count are ${videoRecords.size}, $totalVideoSize MB to Conversion")

        return if (shouldStartVideoCompression(totalVideoSize)) {
            records
        } else {
            Timber.d("Compression queue bigger than setting, show notification to user.")
            showVideoCompressionErrorStatus()
            records.filter { it.type == SyncRecordType.TYPE_PHOTO }
        }
    }

    /**
     * Rename the camera uploads records
     *
     * @return a list of [CameraUploadsRecord]
     */
    private suspend fun renameCameraUploadsRecords(
        records: List<CameraUploadsRecord>,
        primaryUploadNodeId: NodeId,
        secondaryUploadNodeId: NodeId,
    ): List<CameraUploadsRecord> =
        renameCameraUploadsRecordsUseCase(
            records,
            primaryUploadNodeId,
            secondaryUploadNodeId,
        )

    /**
     * Get the existence of a node corresponding to the [CameraUploadsRecord] in the
     * target node or other node
     *
     * @return a list of [CameraUploadsRecord]
     */
    private suspend fun getExistenceInTargetNode(
        records: List<CameraUploadsRecord>,
        primaryUploadNodeId: NodeId,
        secondaryUploadNodeId: NodeId,
    ) = doesCameraUploadsRecordExistsInTargetNodeUseCase(
        records,
        primaryUploadNodeId,
        secondaryUploadNodeId,
    )

    /**
     * Extract the gps coordinates and set in the respective [CameraUploadsRecord]
     *
     * @return a list of [CameraUploadsRecord]
     */
    private suspend fun getGpsCoordinates(records: List<CameraUploadsRecord>): List<CameraUploadsRecord> =
        extractGpsCoordinatesUseCase(records)

    /**
     * Upload the camera uploads records
     *
     * @param records the list of [CameraUploadsRecord] to upload
     * @param primaryUploadNodeId the primary target [NodeId]
     * @param secondaryUploadNodeId the secondary target [NodeId]
     * @param tempRoot the root path of the temporary files
     * @return a flow of [CameraUploadsTransferProgress]
     */
    private fun uploadCameraUploadsRecords(
        records: List<CameraUploadsRecord>,
        primaryUploadNodeId: NodeId,
        secondaryUploadNodeId: NodeId,
        tempRoot: String,
    ): Flow<CameraUploadsTransferProgress> {
        Timber.d("Start uploading ${records.size} files")
        return uploadCameraUploadsRecordsUseCase(
            records,
            primaryUploadNodeId,
            secondaryUploadNodeId,
            tempRoot,
        )
    }

    /**
     *  Process the progress event based on his type
     *
     *  @param progressEvent
     */
    private suspend fun processProgressEvent(progressEvent: CameraUploadsTransferProgress) {
        when (progressEvent) {
            is CameraUploadsTransferProgress.ToUpload,
            is CameraUploadsTransferProgress.ToCopy,
            -> processToCopyOrUploadEvent(progressEvent)

            is CameraUploadsTransferProgress.Copied,
            -> processCopiedEvent(progressEvent)

            is CameraUploadsTransferProgress.UploadInProgress.TransferUpdate,
            -> processUploadInProgressTransferUpdateEvent(progressEvent)

            is CameraUploadsTransferProgress.UploadInProgress.TransferTemporaryError,
            -> processUploadInProgressTransferTemporaryErrorEvent(progressEvent)

            is CameraUploadsTransferProgress.Uploaded,
            -> processUploadedEvent(progressEvent)

            is CameraUploadsTransferProgress.Compressing.Progress,
            -> processCompressingProgress(progressEvent)

            is CameraUploadsTransferProgress.Compressing.InsufficientStorage,
            -> processCompressingInsufficientStorage()

            is CameraUploadsTransferProgress.Compressing.Successful,
            -> processCompressingSuccessful()

            is CameraUploadsTransferProgress.Error,
            -> processError(progressEvent.error)
        }
    }

    /**
     * Process a progress event of type [CameraUploadsTransferProgress.ToUpload] or [CameraUploadsTransferProgress.ToCopy]
     *
     * Update to upload transfer count
     *
     * @param progressEvent
     */
    private suspend fun processToCopyOrUploadEvent(
        progressEvent: CameraUploadsTransferProgress,
    ) {
        updateToUploadCount(
            filePath = progressEvent.record.tempFilePath
                .takeIf {
                    fileSystemRepository.doesFileExist(
                        it
                    )
                }
                ?: progressEvent.record.filePath,
            folderType = progressEvent.record.folderType,
        )
    }

    /**
     * Process a progress event of type [CameraUploadsTransferProgress.Copied]
     *
     * Update uploaded transfer count
     *
     * @param progressEvent
     */
    private suspend fun processCopiedEvent(
        progressEvent: CameraUploadsTransferProgress.Copied,
    ) {
        updateUploadedCountAfterCopy(
            folderType = progressEvent.record.folderType,
            filePath = progressEvent.record.filePath,
            id = progressEvent.record.mediaId,
            nodeId = progressEvent.nodeId
        )
    }

    /**
     * Process a progress event of type [CameraUploadsTransferProgress.UploadInProgress.TransferUpdate]
     *
     * Update transfer progress count
     *
     * @param progressEvent
     */
    private suspend fun processUploadInProgressTransferUpdateEvent(
        progressEvent: CameraUploadsTransferProgress.UploadInProgress.TransferUpdate,
    ) {
        updateUploadedCountAfterTransfer(
            folderType = progressEvent.record.folderType,
            id = progressEvent.record.mediaId,
            transfer = progressEvent.transferEvent.transfer,
        )
    }

    /**
     * Process a progress event of type [CameraUploadsTransferProgress.UploadInProgress.TransferTemporaryError]
     *
     * Handle error
     *
     * @param progressEvent
     */
    private suspend fun processUploadInProgressTransferTemporaryErrorEvent(
        progressEvent: CameraUploadsTransferProgress.UploadInProgress.TransferTemporaryError,
    ) {
        progressEvent.transferEvent.error?.let { handleTransferError(it) }
    }

    /**
     * Process a progress event of type [CameraUploadsTransferProgress.Uploaded]
     *
     * - Handle error
     * - Update uploaded transfer count
     *
     * @param progressEvent
     */
    private suspend fun processUploadedEvent(
        progressEvent: CameraUploadsTransferProgress.Uploaded,
    ) {
        with(progressEvent.transferEvent) {
            error?.let { handleTransferError(it) }
            updateUploadedCountAfterTransfer(
                progressEvent.record.folderType,
                progressEvent.record.mediaId,
                transfer,
            )
        }
    }

    /**
     * Process a progress event of type [CameraUploadsTransferProgress.Compressing.Progress]
     *
     * Display or update compression progress notification
     *
     * @param progressEvent
     */
    private suspend fun processCompressingProgress(
        progressEvent: CameraUploadsTransferProgress.Compressing.Progress,
    ) {
        showVideoCompressionProgress(progressEvent.progress, 1, 1)
    }

    /**
     * Process a progress event of type [CameraUploadsTransferProgress.Compressing.InsufficientStorage]
     *
     * - Display out of space notification for compression
     * - End service
     */
    private suspend fun processCompressingInsufficientStorage() {
        showVideoCompressionOutOfSpaceStatus()
        abortWork(cancelMessage = "Not enough space to compress videos")
    }

    /**
     * Process a progress event of type [CameraUploadsTransferProgress.Compressing.Successful]
     *
     * Cancel the compression notification
     */
    private fun processCompressingSuccessful() {
        cameraUploadsNotificationManagerWrapper.cancelCompressionNotification()
    }

    /**
     * Process a progress event of type [CameraUploadsTransferProgress.Error]
     *
     * @param error the throwable returned in the progress event
     */
    private suspend fun processError(error: Throwable) {
        when (error) {
            is NotEnoughStorageException -> {
                showNotEnoughStorageStatus()
                abortWork(cancelMessage = error.message ?: "Not enough space to create temp file")
            }

            else -> Timber.e(error)
        }
    }

    /**
     * If error corresponds to [QuotaExceededMegaException], broadcast over quota error
     *
     * @param error
     */
    private suspend fun handleTransferError(error: MegaException) {
        when (error) {
            is QuotaExceededMegaException -> {
                Timber.w("Over quota error: ${error.errorCode}")
                broadcastStorageOverQuotaUseCase()
            }

            else -> Timber.w("Image Sync FAIL: ${error.errorString}")
        }
    }


    //@Karma
    private suspend fun checkUploadNodes() {
        Timber.d("Get Pending Files from Media Store Database")
        showCheckUploadStatus()

        val primaryUploadNode =
            getNodeByIdUseCase(NodeId(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary)))
        if (primaryUploadNode == null) {
            abortWork(cancelMessage = "Primary Parent folder is null")
            return
        }
        val secondaryUploadNode = if (isSecondaryFolderEnabled()) {
            Timber.d("Secondary Upload is ENABLED")
            getNodeByIdUseCase(NodeId(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)))
        } else {
            null
        }
        processMediaForUploadUseCase(
            primaryUploadNode.id,
            secondaryUploadNode?.id,
            tempRoot
        )
    }

    //@Karma
    private suspend fun upload() {
        val finalList = getPendingSyncRecords().also {
            Timber.d("Total File to upload ${it.size}")
        }
        startHeartbeat()
        if (finalList.isNotEmpty()) {
            startParallelUpload(finalList, isCompressedVideo = false)
        }
    }

    //@Karma
    private suspend fun compressVideos() {
        if (compressedVideoPending()) {
            startVideoCompression()
            cameraUploadsNotificationManagerWrapper.cancelCompressionNotification()
        }
    }

    //@Karma
    private suspend fun uploadCompressedVideos() {
        val compressedList = getVideoSyncRecordsByStatus(SyncStatus.STATUS_PENDING)
        if (compressedList.isNotEmpty()) {
            Timber.d("Start to upload ${compressedList.size} compressed videos.")
            startParallelUpload(compressedList, isCompressedVideo = true)
        }
    }

    private suspend fun startParallelUpload(
        finalList: List<SyncRecord>,
        isCompressedVideo: Boolean,
    ) = coroutineScope {
        val primaryUploadNode =
            getNodeByIdUseCase(NodeId(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary)))
        val secondaryUploadNode =
            getNodeByIdUseCase(NodeId(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)))

        val uploadFileAsyncList = mutableListOf<Job>()
        for (record in finalList) {
            val parentNodeId =
                (if (record.isSecondary) secondaryUploadNode?.id else primaryUploadNode?.id)
                    ?: continue
            val shouldBeSkipped = createTemporaryFileIfNeeded(record)
            if (shouldBeSkipped) continue
            if (record.isCopyOnly) {
                uploadFileAsyncList.add(launch {
                    semaphore.acquire()
                    Timber.d("Copy from node, file timestamp is: ${record.timestamp}")
                    updateToUploadCount(
                        filePath = record.localPath,
                        folderType = if (record.isSecondary) CameraUploadFolderType.Secondary
                        else CameraUploadFolderType.Primary,
                    )
                    copyNode(
                        record = record,
                        parentNodeId = parentNodeId,
                    )
                    semaphore.release()
                })
            } else {
                uploadFileAsyncList.add(launch {
                    semaphore.acquire()
                    getFileToUpload(record, isCompressedVideo)?.let {
                        updateToUploadCount(
                            filePath = record.newPath?.takeIf { path -> File(path).exists() }
                                ?: record.localPath,
                            folderType = if (record.isSecondary) CameraUploadFolderType.Secondary
                            else CameraUploadFolderType.Primary,
                        )
                        val lastModified = getLastModifiedTime(record)
                        startUploadUseCase(
                            localPath = it.path,
                            parentNodeId = parentNodeId,
                            fileName = record.fileName,
                            modificationTime = lastModified / 1000,
                            appData = APP_DATA_CU,
                            isSourceTemporary = false,
                            shouldStartFirst = false,
                        ).collect { globalTransfer ->
                            onGlobalTransferUpdated(globalTransfer, record)

                            if (globalTransfer is TransferEvent.TransferFinishEvent) {
                                semaphore.release()
                            }
                        }
                    } ?: run {
                        Timber.d("Local file is unavailable, delete record from database.")
                        deleteSyncRecord(record.localPath, record.isSecondary)
                        semaphore.release()
                    }
                })
            }
        }
        uploadFileAsyncList.joinAll()
    }

    private suspend fun getFileToUpload(record: SyncRecord, isCompressedVideo: Boolean): File? {
        return if (isCompressedVideo || record.type == SyncRecordType.TYPE_PHOTO
            || record.type == SyncRecordType.TYPE_VIDEO && shouldCompressVideo()
        ) {
            record.newPath?.let { File(it).takeIf { newFile -> newFile.exists() } }
                ?: File(record.localPath).takeIf { localFile -> localFile.exists() }
        } else {
            File(record.localPath).takeIf { localFile -> localFile.exists() }
        }
    }

    /**
     * create Temporary File and Remove Coordinates based on the settings
     * @param record [SyncRecord]
     *
     * @return [Boolean] indicates whether given [SyncRecord] should  be uploaded or not
     */
    private suspend fun createTemporaryFileIfNeeded(
        record: SyncRecord,
    ): Boolean {
        var shouldBeSkipped = false
        if (record.type == SyncRecordType.TYPE_PHOTO && !record.isCopyOnly) {
            if (!areLocationTagsEnabledUseCase()) {
                flow<String> {
                    createTempFileAndRemoveCoordinatesUseCase(
                        rootPath = tempRoot,
                        filePath = record.localPath,
                        destinationPath = record.newPath
                            ?: throw IllegalArgumentException("Destination path doesn't exist"),
                        timestamp = record.timestamp,
                    )
                }.retryWhen { cause, attempt ->
                    if (cause is NotEnoughStorageException) {
                        if (attempt >= 60) {
                            if (state.value.totalPendingCount == 0) {
                                showNotEnoughStorageStatus()
                                abortWork(cancelMessage = "Not enough space to create temp file")
                            } else {
                                // we will not be retying again and skip the current record
                                Timber.d("Stop retrying for $record")
                                shouldBeSkipped = true
                                return@retryWhen false
                            }
                        }
                        Timber.d("Waiting for disk space to process for $record")
                        // total delay (1 second times 60 attempts) = 60 seconds
                        delay(TimeUnit.SECONDS.toMillis(1))
                        return@retryWhen true
                    } else {
                        // not storage exception, no need to retry
                        return@retryWhen false
                    }
                }.catch {
                    Timber.e("Temporary File creation exception $it")
                    if (it is FileNotFoundException) {
                        deleteSyncRecord(record.localPath, isSecondary = record.isSecondary)
                    }
                    shouldBeSkipped = true
                }.collect()
            } else {
                // Set as don't remove GPS
                record.newPath = record.localPath
            }
        }
        return shouldBeSkipped
    }

    private suspend fun startHeartbeat() {
        updateBackupState(if (areTransfersPaused()) BackupState.PAUSE_UPLOADS else BackupState.ACTIVE)

        sendBackupHeartbeatJob =
            scope?.launch(ioDispatcher) {
                sendBackupHeartBeatSyncUseCase { state.value }
                    .catch { Timber.e(it) }
                    .collect()
            }
    }

    /**
     * Handles the [TransferEvent] emitted by [StartUploadUseCase]
     *
     * @param globalTransfer The [TransferEvent] emitted from the Use Case
     */
    private suspend fun onGlobalTransferUpdated(globalTransfer: TransferEvent, record: SyncRecord) {
        when (globalTransfer) {
            is TransferEvent.TransferFinishEvent -> onTransferFinished(globalTransfer, record)
            is TransferEvent.TransferUpdateEvent -> onTransferUpdated(
                globalTransfer,
                if (record.isSecondary) CameraUploadFolderType.Secondary
                else CameraUploadFolderType.Primary,
                record.id.toLong(),
            )

            is TransferEvent.TransferTemporaryErrorEvent -> onTransferTemporaryError(globalTransfer)
            // No further action necessary for these Scenarios
            is TransferEvent.TransferStartEvent,
            is TransferEvent.TransferDataEvent,
            is TransferEvent.TransferPaused,
            -> Unit
        }
    }

    /**
     * Handle logic for when an upload has finished
     *
     * @param globalTransfer [TransferEvent.TransferFinishEvent]
     */
    private suspend fun onTransferFinished(
        globalTransfer: TransferEvent.TransferFinishEvent,
        record: SyncRecord,
    ) {
        val transfer = globalTransfer.transfer
        val error = globalTransfer.error
        try {
            updateUploadedCountAfterTransfer(
                folderType =
                if (record.isSecondary) CameraUploadFolderType.Secondary
                else CameraUploadFolderType.Primary,
                id = record.id.toLong(),
                transfer = transfer,
            )
            transferFinished(transfer, error, record)
        } catch (th: Throwable) {
            Timber.e(th)
            th.printStackTrace()
        }
    }

    /**
     * Handle logic for when an upload has been updated
     *
     * @param globalTransfer [TransferEvent.TransferFinishEvent]
     */
    private suspend fun onTransferUpdated(
        globalTransfer: TransferEvent.TransferUpdateEvent,
        folderType: CameraUploadFolderType,
        id: Long,
    ) {
        val transfer = globalTransfer.transfer
        runCatching {
            updateUploadedCountAfterTransfer(
                folderType = folderType,
                id = id,
                transfer = transfer,
            )
        }.onFailure {
            Timber.d("Cancelled Transfer Node: ${transfer.nodeHandle}")
            cancelPendingTransfer(transfer)
        }
    }

    /**
     * Handle logic for when a temporary error has occurred during uploading
     *
     * @param globalTransfer [TransferEvent.TransferTemporaryErrorEvent]
     */
    private suspend fun onTransferTemporaryError(globalTransfer: TransferEvent.TransferTemporaryErrorEvent) {
        val error = globalTransfer.error
        Timber.e(error, "onTransferTemporaryError")
        if (error is QuotaExceededMegaException) {
            Timber.w("${if (error.value != 0L) "Transfer" else "Storage"} Over Quota Error: ${error.errorCode}")
            broadcastStorageOverQuotaUseCase()
        }
    }

    /**
     * Perform a copy operation through [CopyNodeUseCase]
     *
     * @param record the [SyncRecord] associated to the node to copy
     * @param parentNodeId the [NodeId] that the node will be moved to
     */
    private suspend fun copyNode(
        record: SyncRecord,
        parentNodeId: NodeId,
    ) {
        runCatching {
            record.nodeHandle?.let { nodeHandle ->
                getNodeByIdUseCase(NodeId(nodeHandle))?.let { nodeToCopy ->
                    val nodeId = copyNodeUseCase(
                        nodeToCopy = nodeToCopy.id,
                        newNodeParent = parentNodeId,
                        newNodeName = record.fileName,
                    )
                    (getNodeByIdUseCase(nodeId) as? TypedFileNode)?.let { retrievedNode ->
                        val fingerprint = retrievedNode.fingerprint
                        val isSecondary = retrievedNode.parentId == NodeId(
                            getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)
                        )
                        // Delete the Camera Uploads sync record by fingerprint
                        fingerprint?.let {
                            deleteSyncRecordByFingerprint(
                                originalPrint = fingerprint,
                                newPrint = fingerprint,
                                isSecondary = isSecondary,
                            )
                        }
                        updateUploadedCountAfterCopy(
                            folderType =
                            if (record.isSecondary) CameraUploadFolderType.Secondary
                            else CameraUploadFolderType.Primary,
                            filePath = record.localPath,
                            id = record.id.toLong(),
                            nodeId = nodeId,
                        )

                        Timber.d("Copy node successful")
                    }
                }
            }
        }.onFailure { error ->
            Timber.e("Copy node error: $error")
        }

    }

    private fun getLastModifiedTime(file: SyncRecord): Long =
        File(file.localPath).lastModified()

    /**
     * Executes certain behavior when the Primary Folder is disabled
     */
    private suspend fun handlePrimaryFolderDisabled() {
        showFolderUnavailableStatus(CameraUploadFolderType.Primary)
        setPrimaryFolderLocalPathUseCase(INVALID_NON_NULL_VALUE)
        setSecondaryFolderLocalPathUseCase(INVALID_NON_NULL_VALUE)
        // Refresh SettingsCameraUploadsFragment
        broadcastCameraUploadsSettingsActionUseCase(CameraUploadsSettingsAction.RefreshSettings)
        abortWork(
            cancelMessage = "Primary Folder is disabled",
            restartMode = CameraUploadsRestartMode.StopAndDisable
        )
    }

    /**
     * Executes certain behavior when the Secondary Folder is disabled
     */
    private suspend fun handleSecondaryFolderDisabled() {
        showFolderUnavailableStatus(CameraUploadFolderType.Secondary)
        // Disable Media Uploads only
        resetMediaUploadTimeStamps()
        disableMediaUploadSettings()
        // setting an invalid path
        setSecondaryFolderLocalPathUseCase(INVALID_NON_NULL_VALUE)
        broadcastCameraUploadsSettingsActionUseCase(CameraUploadsSettingsAction.DisableMediaUploads)
    }

    /**
     * When the user is not logged in, perform a Complete Fast Login procedure
     * @return [Boolean] true if the login process successful otherwise false
     */
    private suspend fun isLoginSuccessful(): Boolean {
        Timber.d("Waiting for the user to complete the Fast Login procedure")

        // arbitrary retry value
        var retry = 3
        while (loginMutex.isLocked && retry > 0) {
            Timber.d("Wait for the isLoggingIn lock to be available")
            delay(1000)
            retry--
        }

        return if (!loginMutex.isLocked) {
            // Legacy support: isLoggingIn needs to be set in order to inform other parts of the
            // app that a Login Procedure is occurring
            val result = runCatching { backgroundFastLoginUseCase() }.onFailure {
                Timber.e(it, "performCompleteFastLogin exception")
            }
            if (result.isSuccess) {
                Timber.d("Complete Fast Login procedure successful. Get cookies settings after login")
                cookieEnabledCheckWrapper.checkEnabledCookies()
            }
            result.isSuccess
        } else {
            Timber.e("isLoggingIn lock not available, cannot perform backgroundFastLogin. Stop process")
            false
        }
    }

    /**
     * Create the primary upload folder on the cloud drive
     * If the creation succeed, set up the primary folder
     *
     * @throws Exception if the creation of the primary upload folder failed
     */
    private suspend fun createAndSetupPrimaryUploadFolder() {
        createCameraUploadFolder(context.getString(R.string.section_photo_sync))?.let {
            Timber.d("Primary Folder successfully created with handle $it. Setting up Primary Folder")
            setupPrimaryFolderUseCase(it)
        } ?: throw Exception("Failed to create primary upload folder")
    }

    /**
     * Create the secondary upload folder on the cloud drive
     * If the creation succeed, set up the secondary folder in local
     *
     * @throws Exception if the creation of the secondary upload folder failed
     */
    private suspend fun createAndSetupSecondaryUploadFolder() {
        createCameraUploadFolder(context.getString(R.string.section_secondary_media_uploads))?.let {
            Timber.d("Secondary Folder successfully created with handle $it. Setting up Secondary Folder")
            setupSecondaryFolderUseCase(it)
        } ?: throw Exception("Failed to create secondary upload folder")
    }

    private suspend fun initService() {
        // Start monitoring external events
        monitorConnectivityStatus()
        monitorChargingStoppedStatus()
        monitorBatteryLevelStatus()
        monitorUploadPauseStatus()
        monitorStorageOverQuotaStatus()
        monitorParentNodesDeleted()
        handleLocalIpChangeUseCase(shouldRetryChatConnections = false)

        // Reset properties
        lastUpdated = 0
        // Clear sync records if needed
        clearSyncRecords()
        // Create temp root folder
        runCatching { tempRoot = createCameraUploadTemporaryRootDirectoryUseCase() }
            .onFailure {
                Timber.w("Root path doesn't exist")
                throw it
            }
    }

    /**
     * Abort the worker
     * This function is called if an error is caught inside the CameraUploadsWorker
     */
    private suspend fun abortWork(
        cancelMessage: String,
        restartMode: CameraUploadsRestartMode = CameraUploadsRestartMode.Reschedule,
    ) = withContext(NonCancellable) {
        Timber.e("Camera Uploads process aborted: $cancelMessage")

        cleanResources()
        sendTransfersInterruptedInfoToBackupCenter()

        scope?.coroutineContext?.cancelChildren(CancellationException(cancelMessage))

        // isStopped signals means that the worker has been cancelled from the WorkManager
        // If the process has been stopped for another reason, we can re-schedule the worker
        // to perform at a later time
        if (!isStopped) {
            Timber.d("Camera Upload stopped with restartMode : $restartMode")
            stopCameraUploadsUseCase(restartMode = restartMode)
        } else {
            Timber.d("Camera Uploads stopped and disabled")
        }
    }

    /**
     * End the worker
     * This function is called when the CameraUploadsWorker complete successfully
     */
    private suspend fun endWork() = withContext(NonCancellable) {
        Timber.d("Camera Uploads process ended successfully: Process completed")

        cleanResources()
        sendTransfersUpToDateInfoToBackupCenter()

        listOf(
            monitorUploadPauseStatusJob,
            monitorConnectivityStatusJob,
            monitorBatteryLevelStatusJob,
            monitorChargingStoppedStatusJob,
            sendBackupHeartbeatJob,
            monitorStorageOverQuotaStatusJob,
            monitorParentNodesDeletedJob,
        ).forEach { it?.cancel() }
    }


    /**
     * Clean the resources
     */
    private suspend fun cleanResources() {
        resetTotalUploadsUseCase()
        resetUploadsCounts()
        deleteCameraUploadsTemporaryRootDirectoryUseCase()
        cancelAllPendingTransfers()
        broadcastProgress(100, 0)
        with(cameraUploadsNotificationManagerWrapper) {
            cancelNotification()
            cancelCompressionNotification()
        }
    }

    /**
     * Sends the appropriate Backup States and Heartbeat Statuses on both Primary and
     * Secondary folders when the active Camera Uploads is interrupted by other means (e.g.
     * no user credentials, Wi-Fi not turned on)
     */
    private suspend fun sendTransfersInterruptedInfoToBackupCenter() {
        if (!isConnectedToInternetUseCase()) return

        // Update both Primary and Secondary Folder Backup States to TEMPORARILY_DISABLED
        updateBackupState(BackupState.TEMPORARILY_DISABLED)

        // Send an INACTIVE Heartbeat Status for both Primary and Secondary Folders
        updateBackupHeartbeatStatus(HeartbeatStatus.INACTIVE)
    }

    /**
     * Sends the appropriate Backup States and Heartbeat Statuses on both Primary and
     * Secondary folders when the user complete the CU in a normal manner
     *
     * One particular case where these states are sent is when the user "Cancel all" uploads
     */
    private suspend fun sendTransfersUpToDateInfoToBackupCenter() {
        if (!isConnectedToInternetUseCase()) return

        // Update both Primary and Secondary Heartbeat Statuses to UP_TO_DATE
        updateBackupHeartbeatStatus(HeartbeatStatus.UP_TO_DATE)
    }

    private suspend fun transferFinished(
        transfer: Transfer,
        error: MegaException?,
        record: SyncRecord,
    ) {
        try {
            val path = transfer.localPath
            if (transfer.state == TransferState.STATE_COMPLETED) {
                addCompletedTransferUseCase(transfer, error)
            }
            error?.let {
                Timber.d("Image Sync Finished, Error Code: ${it.errorCode}")
                if (error is QuotaExceededMegaException) {
                    Timber.w("Over quota error: ${error.errorCode}")
                    showStorageOverQuotaStatus()
                    broadcastStorageOverQuotaUseCase()
                } else {
                    Timber.w("Image Sync FAIL: %d___%s", transfer.nodeHandle, it.errorString)
                }
            } ?: run {
                Timber.d(
                    "Image Sync Finished" +
                            "Image Handle: ${transfer.nodeHandle}, " +
                            "Image Size: ${transfer.transferredBytes}"
                )
                val node = getNodeByIdUseCase(NodeId(transfer.nodeHandle)) as? TypedFileNode
                node?.let { nonNullNode ->
                    handleSetOriginalFingerprint(
                        nodeId = nonNullNode.id,
                        originalFingerprint = record.originFingerprint.orEmpty(),
                    )
                    record.latitude?.let { latitude ->
                        record.longitude?.let { longitude ->
                            setCoordinatesUseCase(
                                nodeId = nonNullNode.id,
                                latitude = latitude.toDouble(),
                                longitude = longitude.toDouble(),
                            )
                        }
                    }
                    File(record.localPath).takeIf { it.exists() }?.let {
                        if (deleteThumbnailUseCase(nonNullNode.id.longValue)) {
                            createImageOrVideoThumbnailUseCase(nonNullNode.id.longValue, it)
                        }
                        if (deletePreviewUseCase(nonNullNode.id.longValue)) {
                            createImageOrVideoPreviewUseCase(nonNullNode.id.longValue, it)
                        }
                    }
                }
                // delete database record
                deleteSyncRecord(path, record.isSecondary)
                // delete temp files
                if (path.startsWith(tempRoot)) {
                    val temp = File(path)
                    if (temp.exists()) {
                        temp.delete()
                    }
                }
            }
        } catch (exception: Exception) {
            Timber.e(exception, "$transfer transferFinished error")
        }
    }

    /**
     * Sets the original fingerprint by calling [setOriginalFingerprintUseCase] and logs the result
     *
     * @param nodeId the [Node] to attach the [originalFingerprint] to
     * @param originalFingerprint the fingerprint of the file before modification
     */
    private suspend fun handleSetOriginalFingerprint(nodeId: NodeId, originalFingerprint: String) {
        runCatching {
            setOriginalFingerprintUseCase(
                nodeId = nodeId,
                originalFingerprint = originalFingerprint,
            )
        }.onSuccess {
            Timber.d("Set original fingerprint successful")
        }.onFailure { error -> Timber.e("Set original fingerprint error: $error") }
    }

    /**
     *  Update total to upload count
     *
     *  @param filePath
     *  @param folderType
     */
    private suspend fun updateToUploadCount(
        filePath: String,
        folderType: CameraUploadFolderType,
    ) {
        val bytes = File(filePath).length()
        increaseTotalToUpload(
            cameraUploadFolderType = folderType,
            bytesToUpload = bytes,
        )
        displayUploadProgress()
    }

    /**
     *  Update total uploaded count after copying a node
     *
     *  @param folderType
     *  @param id
     *  @param filePath
     *  @param nodeId
     */
    private suspend fun updateUploadedCountAfterCopy(
        folderType: CameraUploadFolderType,
        id: Long,
        filePath: String,
        nodeId: NodeId,
    ) {
        updateUploadedCount(
            cameraUploadFolderType = folderType,
            isFinished = true,
            nodeHandle = nodeId.longValue,
            recordId = id,
            bytesUploaded = File(filePath).length(),
        )
    }

    /**
     *  Update total uploaded count after a transfer update or finish
     *
     *  @param folderType
     *  @param id
     *  @param transfer the [Transfer] associated to the file to transfer.
     *                  The transfer can be ongoing or finished.
     */
    private suspend fun updateUploadedCountAfterTransfer(
        folderType: CameraUploadFolderType,
        id: Long,
        transfer: Transfer,
    ) {
        updateUploadedCount(
            cameraUploadFolderType = folderType,
            isFinished = transfer.isFinished,
            nodeHandle = transfer.nodeHandle,
            recordId = id,
            bytesUploaded = transfer.transferredBytes,
        )
    }

    /**
     *  Update total uploaded count after copying a node or after a transfer update or finish
     *
     *  @param cameraUploadFolderType primary or secondary
     *  @param isFinished true if the transfer is finished. When copying, this value is always true
     *  @param nodeHandle the node handle associated to the transfer or the node copied
     *  @param recordId the recordId associated to the file uploaded.
     *                  It is used to identifies the bytes transferred for a unique filed transferred
     *  @param bytesUploaded the bytes transferred for this file
     */
    private suspend fun updateUploadedCount(
        cameraUploadFolderType: CameraUploadFolderType,
        isFinished: Boolean,
        nodeHandle: Long?,
        recordId: Long,
        bytesUploaded: Long,
    ) {
        increaseTotalUploaded(
            cameraUploadFolderType = cameraUploadFolderType,
            isFinished = isFinished,
            nodeHandle = nodeHandle,
            recordId = recordId,
            bytesUploaded = bytesUploaded,
        )
        displayUploadProgress()
    }

    /**
     * Broadcast progress
     *
     * @param progress a value between 0 and 100
     * @param pending count of items pending to be uploaded
     */
    private suspend fun broadcastProgress(progress: Int, pending: Int) {
        broadcastCameraUploadProgress(progress, pending)
    }

    private suspend fun startVideoCompression() = coroutineScope {
        val fullList = getVideoSyncRecordsByStatus(SyncStatus.STATUS_TO_COMPRESS)
        if (fullList.isNotEmpty()) {
            resetTotalUploadsUseCase()
            resetUploadsCounts()
            totalVideoSize = getTotalVideoSizeInMB(fullList.map { it.localPath })
            Timber.d("Total videos count are ${fullList.size}, $totalVideoSize MB to Conversion")
            if (shouldStartVideoCompression(totalVideoSize)) {
                videoCompressionJob = launch {
                    Timber.d("Starting compressor")
                    compressVideos(tempRoot, fullList)
                        .catch {
                            Timber.d("Video Compression fails $it")
                        }
                        .collect {
                            when (it) {
                                is VideoCompressionState.Failed -> {
                                    onCompressFailed(fullList.first { record -> record.id.toLong() == it.id })
                                }

                                VideoCompressionState.Finished -> {
                                    Timber.d("Video Compression Finished Successfully")
                                }

                                is VideoCompressionState.FinishedCompression -> {
                                    Timber.d("Video compressed path: ${it.returnedFile} success:${it.isSuccess} ")
                                }

                                VideoCompressionState.Initial -> {
                                    Timber.d("Video Compression Started")
                                }

                                VideoCompressionState.InsufficientStorage -> {
                                    onInsufficientSpace(fullList)
                                }

                                is VideoCompressionState.Progress -> {
                                    onCompressUpdateProgress(
                                        progress = it.progress,
                                        currentFileIndex = it.currentIndex,
                                        totalCount = it.totalCount
                                    )
                                }

                                is VideoCompressionState.Successful -> {
                                    onCompressSuccessful(fullList.first { record -> record.id.toLong() == it.id })
                                }
                            }
                        }
                }
                videoCompressionJob?.join()
                videoCompressionJob = null
            } else {
                Timber.d("Compression queue bigger than setting, show notification to user.")
                showVideoCompressionErrorStatus()
            }
        }
    }

    private fun getTotalVideoSizeInMB(recordFilePathList: List<String>) =
        recordFilePathList.sumOf { File(it).length() } / (1024 * 1024)

    private suspend fun shouldStartVideoCompression(queueSize: Long): Boolean {
        if (isChargingRequired(queueSize) && !isChargingUseCase()) {
            Timber.d("Should not start video compression.")
            return false
        }
        return true
    }

    /**
     * Not enough space available
     */
    private suspend fun onInsufficientSpace(records: List<SyncRecord>) {
        Timber.w("Insufficient space for video compression.")
        records.forEach { setRecordPendingOrRemove(it) }
        showVideoCompressionOutOfSpaceStatus()
        abortWork(cancelMessage = "Not enough space to compress videos")
    }

    /**
     * Update compression progress
     */
    private suspend fun onCompressUpdateProgress(
        progress: Int,
        currentFileIndex: Int,
        totalCount: Int,
    ) {
        showVideoCompressionProgress(progress, currentFileIndex, totalCount)
    }

    /**
     * Compression successful
     */
    private suspend fun onCompressSuccessful(record: SyncRecord) {
        Timber.d("Compression successfully for file with timestamp: %s", record.timestamp)
        setSyncRecordPendingByPath(record.localPath, record.isSecondary)
    }

    /**
     * Compression failed
     */
    private suspend fun onCompressFailed(record: SyncRecord) {
        setRecordPendingOrRemove(record)
    }

    private suspend fun setRecordPendingOrRemove(record: SyncRecord) {
        val localPath = record.localPath
        val isSecondary = record.isSecondary
        Timber.w("Compression failed for file with timestamp: ${record.timestamp}")
        val srcFile = File(localPath)
        if (srcFile.exists()) {
            try {
                setSyncRecordPendingByPath(localPath, isSecondary)
                Timber.d("Can not compress but got enough disk space, so should be un-supported format issue")
                record.newPath?.let { newPath ->
                    if (newPath.startsWith(tempRoot)) {
                        File(newPath).takeIf { it.exists() }?.delete()
                    }
                }
                // record will remain in DB and will be re-compressed next launch
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        } else {
            Timber.w("Compressed video not exists, remove from DB")
            deleteSyncRecordByLocalPath(localPath, isSecondary)
        }
    }

    private suspend fun displayUploadProgress() {
        runCatching {
            // refresh UI every 1 seconds to avoid too much workload on main thread
            val now = System.currentTimeMillis()
            lastUpdated = if (now - lastUpdated > ON_TRANSFER_UPDATE_REFRESH_MILLIS) {
                now
            } else {
                return
            }

            with(state.value) {
                val totalUploadBytes = totalBytesToUploadCount
                val totalUploadedBytes = totalBytesUploadedCount
                val totalUploaded = totalUploadedCount
                val totalToUpload = totalToUploadCount
                val pendingToUpload = totalPendingCount
                val progressPercent = totalProgress

                Timber.d(
                    "Total to upload: $totalToUpload " +
                            "Total uploaded: $totalUploaded " +
                            "Pending uploads: $pendingToUpload " +
                            "bytes to upload: $totalUploadBytes " +
                            "bytes uploaded: $totalUploadedBytes " +
                            "progress: $progressPercent"
                )
                if (totalToUpload > 0) {
                    broadcastProgress(progressPercent, pendingToUpload)
                    showUploadProgress(
                        totalUploaded,
                        totalToUpload,
                        totalUploadedBytes,
                        totalUploadBytes,
                        progressPercent,
                        areTransfersPaused()
                    )
                }
            }
        }.onFailure { Timber.w(it) }
    }

    /**
     *  Display a notification for upload progress
     */
    private suspend fun showUploadProgress(
        totalUploaded: Int,
        totalToUpload: Int,
        totalUploadedBytes: Long,
        totalUploadBytes: Long,
        progress: Int,
        areUploadsPaused: Boolean,
    ) {
        setProgress(
            workDataOf(
                STATUS_INFO to PROGRESS,
                TOTAL_UPLOADED to totalUploaded,
                TOTAL_TO_UPLOAD to totalToUpload,
                TOTAL_UPLOADED_BYTES to totalUploadedBytes,
                TOTAL_UPLOAD_BYTES to totalUploadBytes,
                CURRENT_PROGRESS to progress,
                ARE_UPLOADS_PAUSED to areUploadsPaused
            )
        )
    }

    /**
     *  Display a notification for video compression progress
     */
    private suspend fun showVideoCompressionProgress(
        progress: Int,
        currentFileIndex: Int,
        totalCount: Int,
    ) {
        setProgress(
            workDataOf(
                STATUS_INFO to COMPRESSION_PROGRESS,
                CURRENT_PROGRESS to progress,
                CURRENT_FILE_INDEX to currentFileIndex,
                TOTAL_COUNT to totalCount,
                USE_CAMERA_UPLOADS_RECORDS to useCameraUploadsRecordsEnabled,
            )
        )
    }

    /**
     *  Display a notification for checking files to upload
     */
    private suspend fun showCheckUploadStatus() {
        setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
    }

    /**
     *  Display a notification in case the cloud storage does not have enough space
     */
    private suspend fun showStorageOverQuotaStatus() {
        setProgress(workDataOf(STATUS_INFO to STORAGE_OVER_QUOTA))
    }

    /**
     *  Display a notification in case the device does not have enough local storage
     *  for video compression
     */
    private suspend fun showVideoCompressionOutOfSpaceStatus() {
        setProgress(workDataOf(STATUS_INFO to OUT_OF_SPACE))
    }

    /**
     *  Display a notification in case the device does not have enough local storage
     *  for creating temporary files
     */
    private suspend fun showNotEnoughStorageStatus() {
        setProgress(workDataOf(STATUS_INFO to NOT_ENOUGH_STORAGE))
    }

    /**
     *  Display a notification in case an error happened during video compression
     */
    private suspend fun showVideoCompressionErrorStatus() {
        setProgress(workDataOf(STATUS_INFO to COMPRESSION_ERROR))
    }

    /**
     * When Camera Uploads cannot launch due to the Folder being unavailable, display a Notification
     * to inform the User
     *
     * @param cameraUploadsFolderType
     */
    private suspend fun showFolderUnavailableStatus(cameraUploadsFolderType: CameraUploadFolderType) {
        setProgress(
            workDataOf(
                STATUS_INFO to FOLDER_UNAVAILABLE,
                FOLDER_TYPE to cameraUploadsFolderType.ordinal
            )
        )
    }

    /**
     *  Update backup state
     *
     *  @param backupState
     */
    private suspend fun updateBackupState(backupState: BackupState) {
        updateCameraUploadsBackupStatesUseCase(backupState)
        updateLastTimestamp()
    }

    /**
     * Update the backup heartbeat status
     *
     * @param heartbeatStatus
     */
    private suspend fun updateBackupHeartbeatStatus(heartbeatStatus: HeartbeatStatus) {
        updateLastTimestamp()
        runCatching {
            updateCameraUploadsBackupHeartbeatStatusUseCase(
                heartbeatStatus = heartbeatStatus,
                cameraUploadsState = state.value,
            )
        }.onFailure { Timber.e(it) }
    }

    /**
     * Check if transfers paused
     *
     * @return true if transfers paused
     */
    private suspend fun areTransfersPaused(): Boolean =
        monitorPausedTransfersUseCase().firstOrNull() == true

    /**
     * Update the timestamp of the last actions
     * This timestamp is updated when :
     * - the backup state is updated
     * - the heartbeat status is updated due to interrupted or finished process
     * - a transfer finished
     */
    private suspend fun updateLastTimestamp(
    ) = stateUpdateMutex.withLock {
        val timestamp = Instant.now().epochSecond
        updateState(
            cameraUploadFolderType = CameraUploadFolderType.Primary,
            lastTimestamp = timestamp,
        )
        updateState(
            cameraUploadFolderType = CameraUploadFolderType.Secondary,
            lastTimestamp = timestamp
        )
    }

    /**
     * Increase by 1 the total to upload count for the given [CameraUploadFolderType]
     * Add the [bytesToUpload] to the total bytes to upload count
     *
     * @param cameraUploadFolderType the type of the CU folder
     * @param bytesToUpload bytes to be added to the total bytes to upload count
     */
    private suspend fun increaseTotalToUpload(
        cameraUploadFolderType: CameraUploadFolderType,
        bytesToUpload: Long,
    ) = stateUpdateMutex.withLock {
        when (cameraUploadFolderType) {
            CameraUploadFolderType.Primary -> state.value.primaryCameraUploadsState
            CameraUploadFolderType.Secondary -> state.value.secondaryCameraUploadsState
        }.let { state ->
            updateState(
                cameraUploadFolderType = cameraUploadFolderType,
                toUploadCount = state.toUploadCount + 1,
                bytesToUploadCount = state.bytesToUploadCount + bytesToUpload
            )
        }
    }

    /**
     * Increase by 1 the total uploaded count for the given [CameraUploadFolderType]
     * only if the upload is finished
     * Add the [bytesUploaded] to the total bytes uploaded count
     * @param cameraUploadFolderType the type of the CU folder
     * @param isFinished true if the upload is finished
     * @param nodeHandle the node handle of the file uploaded
     * @param recordId the recordId associated to the file uploaded
     * @param bytesUploaded bytes to be added to the total bytes to uploaded count
     */
    private suspend fun increaseTotalUploaded(
        cameraUploadFolderType: CameraUploadFolderType,
        isFinished: Boolean,
        bytesUploaded: Long,
        nodeHandle: Long?,
        recordId: Long,
    ) = stateUpdateMutex.withLock {
        when (cameraUploadFolderType) {
            CameraUploadFolderType.Primary -> state.value.primaryCameraUploadsState
            CameraUploadFolderType.Secondary -> state.value.secondaryCameraUploadsState
        }.let { state ->
            if (isFinished) {
                updateState(
                    cameraUploadFolderType = cameraUploadFolderType,
                    uploadedCount = state.uploadedCount + 1,
                    lastTimestamp = Instant.now().epochSecond,
                    lastHandle = nodeHandle,
                    recordId = recordId,
                    bytesFinishedUploadedCount = state.bytesFinishedUploadedCount + bytesUploaded,
                )
            } else {
                updateState(
                    cameraUploadFolderType = cameraUploadFolderType,
                    recordId = recordId,
                    bytesUploaded = bytesUploaded,
                )
            }
        }
    }

    /**
     * Reset the total uploads counts
     */
    private suspend fun resetUploadsCounts() = stateUpdateMutex.withLock {
        updateState(
            cameraUploadFolderType = CameraUploadFolderType.Primary,
            toUploadCount = 0,
            uploadedCount = 0,
            bytesToUploadCount = 0L,
            bytesUploadedTable = Hashtable(),
            bytesFinishedUploadedCount = 0L,
        )
        updateState(
            cameraUploadFolderType = CameraUploadFolderType.Secondary,
            toUploadCount = 0,
            uploadedCount = 0,
            bytesToUploadCount = 0L,
            bytesUploadedTable = Hashtable(),
            bytesFinishedUploadedCount = 0L,
        )
    }

    /**
     * Update the state of the given [CameraUploadFolderType]
     *
     * If the value passed in parameter is null, keep the current value of the state attribute
     */
    private fun updateState(
        cameraUploadFolderType: CameraUploadFolderType,
        lastTimestamp: Long? = null,
        lastHandle: Long? = null,
        toUploadCount: Int? = null,
        uploadedCount: Int? = null,
        bytesToUploadCount: Long? = null,
        recordId: Long? = null,
        bytesUploaded: Long? = null,
        bytesUploadedTable: Hashtable<Long, Long>? = null,
        bytesFinishedUploadedCount: Long? = null,
    ) {
        when (cameraUploadFolderType) {
            CameraUploadFolderType.Primary -> state.value.primaryCameraUploadsState
            CameraUploadFolderType.Secondary -> state.value.secondaryCameraUploadsState
        }.let { state ->
            val copiedState = with(state) {
                val bytesTable =
                    recordId?.let {
                        Hashtable(this.bytesInProgressUploadedTable).apply {
                            if (bytesFinishedUploadedCount == null) {
                                bytesUploaded?.let { this[recordId] = bytesUploaded }
                            } else {
                                this.remove(recordId)
                            }
                        }
                    } ?: bytesUploadedTable

                copy(
                    lastTimestamp = lastTimestamp ?: this.lastTimestamp,
                    lastHandle = lastHandle ?: this.lastHandle,
                    toUploadCount = toUploadCount ?: this.toUploadCount,
                    uploadedCount = uploadedCount ?: this.uploadedCount,
                    bytesToUploadCount = bytesToUploadCount ?: this.bytesToUploadCount,
                    bytesInProgressUploadedTable = bytesTable
                        ?: this.bytesInProgressUploadedTable,
                    bytesFinishedUploadedCount = bytesFinishedUploadedCount
                        ?: this.bytesFinishedUploadedCount
                )
            }
            _state.update {
                when (cameraUploadFolderType) {
                    CameraUploadFolderType.Primary -> it.copy(primaryCameraUploadsState = copiedState)
                    CameraUploadFolderType.Secondary -> it.copy(secondaryCameraUploadsState = copiedState)
                }
            }
        }
    }
}
