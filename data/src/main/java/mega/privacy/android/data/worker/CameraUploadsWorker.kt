package mega.privacy.android.data.worker

import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkInfo.Companion.STOP_REASON_CANCELLED_BY_APP
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mega.privacy.android.data.R
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.ARE_UPLOADS_PAUSED
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CHECK_FILE_UPLOAD
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.COMPRESSION_ERROR
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.COMPRESSION_PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.COMPRESSION_SUCCESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CURRENT_FILE_INDEX
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.CURRENT_PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.FINISHED
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.FOLDER_TYPE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.FOLDER_UNAVAILABLE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.NOT_ENOUGH_STORAGE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.OUT_OF_SPACE
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.PROGRESS
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.START
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.STATUS_INFO
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.STORAGE_OVER_QUOTA
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_COUNT
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_TO_UPLOAD
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_UPLOADED
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_UPLOADED_BYTES
import mega.privacy.android.data.constant.CameraUploadsWorkerStatusConstant.TOTAL_UPLOAD_BYTES
import mega.privacy.android.data.extensions.collectChunked
import mega.privacy.android.data.wrapper.CameraUploadsNotificationManagerWrapper
import mega.privacy.android.data.wrapper.CookieEnabledCheckWrapper
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsState
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsTransferProgress
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TimeSystemRepository
import mega.privacy.android.domain.usecase.account.IsStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.backup.InitializeBackupsUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreCameraUploadsFoldersInRubbishBinUseCase
import mega.privacy.android.domain.usecase.camerauploads.BroadcastCameraUploadsSettingsActionUseCase
import mega.privacy.android.domain.usecase.camerauploads.CheckOrCreateCameraUploadsNodeUseCase
import mega.privacy.android.domain.usecase.camerauploads.CreateCameraUploadsTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.camerauploads.DeleteCameraUploadsTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.camerauploads.DisableCameraUploadsUseCase
import mega.privacy.android.domain.usecase.camerauploads.DisableMediaUploadsSettingsUseCase
import mega.privacy.android.domain.usecase.camerauploads.DoesCameraUploadsRecordExistsInTargetNodeUseCase
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.camerauploads.ExtractGpsCoordinatesUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPendingCameraUploadsRecordsUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadFolderHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.HandleLocalIpChangeUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingRequiredUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsMediaUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsPrimaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsSecondaryFolderSetUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsWifiNotSatisfiedUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorIsChargingRequiredToUploadContentUseCase
import mega.privacy.android.domain.usecase.camerauploads.ProcessCameraUploadsMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.RenameCameraUploadsRecordsUseCase
import mega.privacy.android.domain.usecase.camerauploads.SendBackupHeartBeatSyncUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetSecondaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateCameraUploadsBackupHeartbeatStatusUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateCameraUploadsBackupStatesUseCase
import mega.privacy.android.domain.usecase.camerauploads.UploadCameraUploadsRecordsUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.ResetTotalUploadsUseCase
import mega.privacy.android.domain.usecase.workers.ScheduleCameraUploadUseCase
import timber.log.Timber
import java.time.Instant
import java.util.Hashtable
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Worker to run Camera Uploads
 */
@HiltWorker
class CameraUploadsWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val isStorageOverQuotaUseCase: IsStorageOverQuotaUseCase,
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
    private val isPrimaryFolderPathValidUseCase: IsPrimaryFolderPathValidUseCase,
    private val isSecondaryFolderSetUseCase: IsSecondaryFolderSetUseCase,
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val isWifiNotSatisfiedUseCase: IsWifiNotSatisfiedUseCase,
    private val setPrimaryFolderLocalPathUseCase: SetPrimaryFolderLocalPathUseCase,
    private val setSecondaryFolderLocalPathUseCase: SetSecondaryFolderLocalPathUseCase,
    private val isChargingRequiredUseCase: IsChargingRequiredUseCase,
    private val getUploadFolderHandleUseCase: GetUploadFolderHandleUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorPausedTransfersUseCase: MonitorPausedTransfersUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase,
    private val monitorIsChargingRequiredToUploadContentUseCase: MonitorIsChargingRequiredToUploadContentUseCase,
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val handleLocalIpChangeUseCase: HandleLocalIpChangeUseCase,
    private val cancelTransferByTagUseCase: CancelTransferByTagUseCase,
    private val getTransferByTagUseCase: GetTransferByTagUseCase,
    private val checkOrCreateCameraUploadsNodeUseCase: CheckOrCreateCameraUploadsNodeUseCase,
    private val establishCameraUploadsSyncHandlesUseCase: EstablishCameraUploadsSyncHandlesUseCase,
    private val resetTotalUploadsUseCase: ResetTotalUploadsUseCase,
    private val disableMediaUploadSettingsUseCase: DisableMediaUploadsSettingsUseCase,
    private val createCameraUploadsTemporaryRootDirectoryUseCase: CreateCameraUploadsTemporaryRootDirectoryUseCase,
    private val deleteCameraUploadsTemporaryRootDirectoryUseCase: DeleteCameraUploadsTemporaryRootDirectoryUseCase,
    private val scheduleCameraUploadUseCase: ScheduleCameraUploadUseCase,
    private val updateCameraUploadsBackupStatesUseCase: UpdateCameraUploadsBackupStatesUseCase,
    private val sendBackupHeartBeatSyncUseCase: SendBackupHeartBeatSyncUseCase,
    private val updateCameraUploadsBackupHeartbeatStatusUseCase: UpdateCameraUploadsBackupHeartbeatStatusUseCase,
    private val monitorStorageOverQuotaUseCase: MonitorStorageOverQuotaUseCase,
    private val broadcastStorageOverQuotaUseCase: BroadcastStorageOverQuotaUseCase,
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
    private val getUploadVideoQualityUseCase: GetUploadVideoQualityUseCase,
    private val disableCameraUploadsUseCase: DisableCameraUploadsUseCase,
    private val getFileByPathUseCase: GetFileByPathUseCase,
    private val fileSystemRepository: FileSystemRepository,
    private val timeSystemRepository: TimeSystemRepository,
    private val crashReporter: CrashReporter,
    private val monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val handleTransferEventUseCase: HandleTransferEventUseCase,
    private val clearActiveTransfersIfFinishedUseCase: ClearActiveTransfersIfFinishedUseCase,
    private val correctActiveTransfersUseCase: CorrectActiveTransfersUseCase,
    @LoginMutex private val loginMutex: Mutex,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val LOW_BATTERY_LEVEL = 20
        private const val ON_TRANSFER_UPDATE_REFRESH_MILLIS = 1000
    }

    /**
     * True if the battery level of the device is above
     * the required battery level to run the CU
     */
    private var deviceAboveMinimumBatteryLevel: Boolean = true

    /**
     * True if the Device meets the charging constraint that was set
     */
    private var isChargingConstraintSatisfied: Boolean = true

    /**
     * True if the wifi constraint is met
     */
    private var isWifiConstraintSatisfied: Boolean = true

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
     * Total video size to upload in MB
     */
    private var totalVideoSize = 0L

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
     * Job to monitor both monitor battery level status and monitor is charging required to upload
     * content flows
     */
    private var monitorBatteryLevelAndChargingStatusesJob: Job? = null

    /**
     * Job to monitor Camera Uploads Transfers
     */
    private var monitorCameraUploadsTransfers: Job? = null

    /**
     * Job to monitor transfer over quota status flow
     */
    private var monitorStorageOverQuotaStatusJob: Job? = null

    /**
     * Job to monitor deletion of parent nodes
     */
    private var monitorParentNodesDeletedJob: Job? = null

    /**
     * Job to retrieve files to upload
     */
    private val retrieveFilesJob = Job()

    /**
     * Job to upload files
     */
    private var uploadJob: Job? = null

    /**
     * Flag to detect if the worker received an internal signal to abort
     */
    private var finishedReason: CameraUploadsFinishedReason? = null

    /**
     * The restart mode after the work completion, whether successful or aborted
     * Default to [CameraUploadsRestartMode.Reschedule]
     */
    private var restartMode = CameraUploadsRestartMode.Reschedule

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        runCatching {
            Timber.d("Start CU Worker")
            crashReporter.log("${CameraUploadsWorker::class.java.simpleName} Started")
            // Signal to not kill the worker if the app is killed
            setForegroundAsync(getForegroundInfo())

            monitorConnectivityStatusJob = monitorConnectivityStatus()
            monitorBatteryLevelAndChargingStatusesJob = monitorBatteryLevelAndChargingStatusesJob()
            monitorStorageOverQuotaStatusJob = monitorStorageOverQuotaStatus()
            monitorParentNodesDeletedJob = monitorParentNodesDeleted()
            monitorCameraUploadsTransfers = monitorCameraUploadsTransfers()

            handleLocalIpChangeUseCase(shouldRetryChatConnections = false)

            if (canRunCameraUploads()) {
                Timber.d("Starting upload process")
                sendStartUploadStatus()

                val primaryUploadNodeId =
                    NodeId(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary))
                val secondaryUploadNodeId =
                    NodeId(getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary))

                val records = async(retrieveFilesJob) {
                    scanFiles()
                    return@async getAndPrepareRecords(
                        primaryUploadNodeId,
                        secondaryUploadNodeId,
                    )
                }.await()

                records?.let {
                    monitorUploadPauseStatusJob = monitorUploadPauseStatus()
                    sendBackupHeartbeatJob = sendPeriodicBackupHeartBeat()
                    uploadJob = launch {
                        uploadFiles(it, primaryUploadNodeId, secondaryUploadNodeId, tempRoot)
                    }
                    uploadJob?.join()
                }
            }
        }.onFailure { throwable ->
            withContext(NonCancellable) {
                Timber.e(
                    t = throwable,
                    message = "Camera Uploads process aborted".apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                            "$this, worker stopReason is $stopReason"
                    }
                )

                // Known finished reason should have been already handled in abortWork
                // If finishedReason is null, it means the worker was stopped by an error unhandled by the worker logic
                if (finishedReason == null) {
                    // The actual stop reason will be automatically sent through monitorCameraUploadsStatusInfo
                    // There is no need to know the exact stop reason here
                    finishedReason = CameraUploadsFinishedReason.UNKNOWN

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (stopReason == STOP_REASON_CANCELLED_BY_APP) {
                            restartMode = CameraUploadsRestartMode.Stop
                        }
                    }
                }
            }
        }

        return@withContext withContext(NonCancellable) {
            cleanResources()
            sendFinishedStatus(finishedReason ?: CameraUploadsFinishedReason.COMPLETED)
            endWork(finishedReason ?: CameraUploadsFinishedReason.COMPLETED, restartMode).also {
                crashReporter.log("${CameraUploadsWorker::class.java.simpleName} Finished")
            }
        }
    }

    /**
     *
     * Post work process
     * Clean resources and send backup state
     * Reschedule the work if needed
     *
     * @param finishedReason the reason why the worker finished
     * @param restartMode the restart mode to apply after the completion of the worker
     * @return Result.success if the worker completed successfully, Result.retry if the worker
     *         needs to be restarted immediately, Result.failure otherwise
     */
    private suspend fun endWork(
        finishedReason: CameraUploadsFinishedReason,
        restartMode: CameraUploadsRestartMode,
    ): Result =
        when (finishedReason) {
            CameraUploadsFinishedReason.COMPLETED -> {
                resetTotalUploads()
                sendTransfersUpToDateInfoToBackupCenter()
                scheduleCameraUploads()
                Result.success()
            }

            else -> {
                cancelAllTransfers()
                resetTotalUploads()
                sendTransfersInterruptedInfoToBackupCenter()
                when (restartMode) {
                    CameraUploadsRestartMode.RestartImmediately -> {
                        Result.retry()
                    }

                    CameraUploadsRestartMode.Reschedule -> {
                        scheduleCameraUploads()
                        Result.failure()
                    }

                    CameraUploadsRestartMode.StopAndDisable -> {
                        disableCameraUploads()
                        Result.failure()
                    }

                    CameraUploadsRestartMode.Stop -> {
                        Result.failure()
                    }
                }
            }
        }.also {
            Timber.d("Camera Uploads process finished with $finishedReason with restart mode: $restartMode")
        }

    override suspend fun getForegroundInfo() =
        cameraUploadsNotificationManagerWrapper.getForegroundInfo()

    private fun CoroutineScope.monitorUploadPauseStatus() = launch {
        monitorPausedTransfersUseCase().collect {
            updateBackupState(if (it) BackupState.PAUSE_UPLOADS else BackupState.ACTIVE)
            displayUploadProgress()
        }
    }

    private fun CoroutineScope.monitorConnectivityStatus() = launch {
        monitorConnectivityUseCase().collect {
            isWifiConstraintSatisfied = it && isWifiNotSatisfiedUseCase().not()
            if (!isWifiConstraintSatisfied) {
                abortWork(reason = CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET)
            }
        }
    }

    private fun CoroutineScope.monitorBatteryLevelAndChargingStatusesJob() = launch {
        combine(
            monitorBatteryInfoUseCase(),
            monitorIsChargingRequiredToUploadContentUseCase(),
            transform = { batteryInfo, chargingRequired ->
                Pair(batteryInfo, chargingRequired)
            },
        ).collect { (batteryInfo, chargingRequired) ->
            val isCharging = batteryInfo.isCharging
            deviceAboveMinimumBatteryLevel = (batteryInfo.level > LOW_BATTERY_LEVEL || isCharging)
            isChargingConstraintSatisfied = if (chargingRequired) isCharging else true

            when {
                !deviceAboveMinimumBatteryLevel -> abortWork(reason = CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW)
                !isChargingConstraintSatisfied -> abortWork(reason = CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET)
            }
        }
    }

    private fun CoroutineScope.monitorStorageOverQuotaStatus() = launch {
        monitorStorageOverQuotaUseCase().collect {
            if (it) {
                sendStorageOverQuotaStatus()
                abortWork(reason = CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA)
            }
        }
    }


    private fun CoroutineScope.monitorParentNodesDeleted() = launch {
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
                    reason = CameraUploadsFinishedReason.TARGET_NODES_DELETED,
                    restartMode = CameraUploadsRestartMode.RestartImmediately,
                )
            }
        }
    }

    /**
     * Monitors and processes only the Camera Uploads Transfers
     */
    private fun CoroutineScope.monitorCameraUploadsTransfers() = launch {
        runCatching { correctActiveTransfersUseCase(TransferType.CU_UPLOAD) }
            .onFailure { Timber.e(it) }
        monitorTransferEventsUseCase()
            .filter { it.transfer.transferType == TransferType.CU_UPLOAD }
            .collectChunked(
                chunkDuration = 2.seconds,
                flushOnIdleDuration = 200.milliseconds,
            ) { transferEvents ->
                handleCameraUploadsTransferEvents(transferEvents)
            }
    }

    /**
     * Processes the Camera Uploads Transfer events. The operation is marked as [NonCancellable] so
     * that Transfer handling operations will complete, even when [MonitorTransferEventsUseCase] ends
     *
     * @param transferEvents a list of Camera Uploads Transfer Events
     */
    private suspend fun handleCameraUploadsTransferEvents(transferEvents: List<TransferEvent>) {
        withContext(NonCancellable) {
            launch {
                handleTransferEventUseCase(events = transferEvents.toTypedArray())
            }
        }
    }

    /**
     * Send the back up heart beat periodically
     */
    private fun CoroutineScope.sendPeriodicBackupHeartBeat() =
        sendBackupHeartBeatSyncUseCase { state.value }
            .catch { Timber.e(it) }
            .launchIn(this)

    /**
     * Schedule the camera uploads to run at a later time
     */
    private suspend fun scheduleCameraUploads() {
        runCatching { scheduleCameraUploadUseCase() }
            .onFailure { Timber.e(it) }
    }

    /**
     * Disable the camera uploads
     */
    private suspend fun disableCameraUploads() {
        runCatching { disableCameraUploadsUseCase() }
            .onFailure { Timber.e(it) }
    }

    /**
     * Cancels all [Transfer] initiated by the process,
     */
    private suspend fun cancelAllTransfers() {
        runCatching {
            with(state.value) {
                (primaryCameraUploadsState.uploadTags + secondaryCameraUploadsState.uploadTags).forEach {
                    getTransferByTagUseCase(it)?.let { transfer ->
                        if (!transfer.isFinished)
                            cancelTransferByTagUseCase(it)
                    }
                }
            }
        }.onFailure { Timber.e(it) }
    }

    /**
     * Reset totals uploads
     */
    private suspend fun resetTotalUploads() {
        runCatching { resetTotalUploadsUseCase() }
            .onFailure { Timber.e(it) }
        runCatching { clearActiveTransfersIfFinishedUseCase() }
            .onFailure { Timber.e(it) }
    }

    /**
     * Checks if Camera Uploads can run by checking multiple conditions.
     * Abort the worker if an abort reason is raised.
     *
     * @return true if the Camera Uploads can run, false otherwise
     */
    private suspend fun canRunCameraUploads(): Boolean = when {
        !isLoginSuccessful() -> CameraUploadsFinishedReason.LOGIN_FAILED
        !isCameraUploadsEnabled() -> CameraUploadsFinishedReason.DISABLED
        !hasMediaPermission() -> CameraUploadsFinishedReason.MEDIA_PERMISSION_NOT_GRANTED
        !isLocalPrimaryFolderValid() -> CameraUploadsFinishedReason.LOCAL_PRIMARY_FOLDER_NOT_VALID
        !isWifiConstraintSatisfied() -> CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET
        !isDeviceAboveMinimumBatteryLevel() -> CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW
        !isChargingConstraintSatisfied() -> CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET
        isStorageQuotaExceeded() -> CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA
        else -> {
            if (isMediaUploadsEnabledUseCase())
                isLocalSecondaryFolderValid()

            when {
                !synchronizeUploadNodeHandles() -> CameraUploadsFinishedReason.ERROR_DURING_PROCESS
                !checkOrCreatePrimaryUploadNodes() -> CameraUploadsFinishedReason.ERROR_DURING_PROCESS
                isMediaUploadsEnabledUseCase() && !checkOrCreateSecondaryUploadNodes() -> CameraUploadsFinishedReason.ERROR_DURING_PROCESS
                !initializeBackup() -> CameraUploadsFinishedReason.ERROR_DURING_PROCESS
                !createTempCacheFile() -> CameraUploadsFinishedReason.ERROR_DURING_PROCESS
                else -> null
            }
        }
    }?.let { reasonToAbort ->
        val restartMode = when (reasonToAbort) {
            // disable
            CameraUploadsFinishedReason.DISABLED,
            CameraUploadsFinishedReason.MEDIA_PERMISSION_NOT_GRANTED,
            CameraUploadsFinishedReason.LOCAL_PRIMARY_FOLDER_NOT_VALID,
            -> CameraUploadsRestartMode.StopAndDisable

            // reschedule
            else -> CameraUploadsRestartMode.Reschedule
        }
        abortWork(reasonToAbort, restartMode)
        false
    } ?: true


    /**
     * Checks if the Camera Uploads from [isCameraUploadsEnabledUseCase] is enabled
     *
     * @return true if enabled, and false if otherwise
     */
    private suspend fun isCameraUploadsEnabled(): Boolean = isCameraUploadsEnabledUseCase().also {
        if (!it) Timber.e("Camera Uploads disabled")
    }

    /**
     * Check if the media permission is granted
     * Disable the Camera Uploads if the media permission is granted
     *
     * @return true if the media permission is granted
     */
    private fun hasMediaPermission() = hasMediaPermissionUseCase().also {
        if (!it) Timber.e("Media permission not granted")
    }

    /**
     * Checks if the Wi-Fi constraint from the negated [isWifiNotSatisfiedUseCase] is satisfied
     *
     * @return true if the Wi-Fi constraint is satisfied, and false if otherwise
     */
    private fun isWifiConstraintSatisfied(): Boolean = isWifiConstraintSatisfied.also {
        if (!it) Timber.e("Wi-Fi or Mobile network required")
    }

    /**
     * Check if the account has enough cloud storage space
     *
     * @return true if the device has not enough cloud storage space
     */
    private suspend fun isStorageQuotaExceeded() = isStorageOverQuotaUseCase().also {
        if (it) Timber.e("Storage Quota exceeded")
    }

    /**
     * Check if the device battery is above the required battery level
     *
     * @return true if the device battery is above the required battery level
     */
    private fun isDeviceAboveMinimumBatteryLevel() = deviceAboveMinimumBatteryLevel.also {
        if (!it) Timber.e("Device Battery level requirement not met")
    }

    /**
     * Checks if the Device meets the charging constraint that was set (e.g. the Device is currently
     * charged, and the Option to require Device charging to upload content is enabled)
     *
     * @return true if the Device charging constraint has been met
     */
    private fun isChargingConstraintSatisfied() =
        isChargingConstraintSatisfied.also {
            if (!it) Timber.e("The Device does not meet the charging constraint")
        }

    /**
     * Retrieve the Camera Uploads Nodes from the server and update the local preferences
     *
     * @return true if the Camera Uploads Nodes are retrieved and local preferences are updated successfully
     */
    private suspend fun synchronizeUploadNodeHandles(): Boolean {
        return runCatching {
            establishCameraUploadsSyncHandlesUseCase()
        }.onFailure {
            Timber.e(it)
        }.isSuccess
    }

    /**
     * Initialize backup
     *
     * @return true if the backup is initialized successfully
     */
    private suspend fun initializeBackup(): Boolean {
        return runCatching { initializeBackupsUseCase() }.onFailure {
            Timber.e(it)
        }.isSuccess
    }

    /**
     * Checks if the Primary Folder exists and is valid
     *
     * @return true if the Primary Folder exists and is valid, and false if otherwise
     */
    private suspend fun isLocalPrimaryFolderValid(): Boolean =
        isPrimaryFolderPathValidUseCase(getPrimaryFolderPathUseCase()).also {
            if (!it) {
                Timber.e("The local Primary Folder does not exist or is invalid")
                handleInvalidLocalPrimaryFolder()
            }
        }

    /**
     * Checks if the Secondary Folder exists and is valid
     *
     * @return true if the Secondary Folder exists and is valid, and false if otherwise
     */
    private suspend fun isLocalSecondaryFolderValid(): Boolean =
        isSecondaryFolderSetUseCase().also {
            if (!it) {
                Timber.e("The local Secondary Folder does not exist or is invalid")
                handleInvalidLocalSecondaryFolder()
            }
        }

    /**
     * Check if the Primary Upload Node is valid.
     * When the Primary Upload Node does not exist, this function will create the corresponding node
     *
     * @return true if the Primary Upload Node is valid, or the creation of the corresponding node is successful
     */
    private suspend fun checkOrCreatePrimaryUploadNodes(): Boolean {
        return runCatching {
            checkOrCreateCameraUploadsNodeUseCase(
                folderName = context.getString(R.string.section_photo_sync),
                folderType = CameraUploadFolderType.Primary,
            )
        }.onFailure {
            Timber.e(it)
        }.isSuccess
    }

    /**
     * Check if the Secondary Upload Node is valid.
     * When the Secondary Upload Node (if enabled) does not exist, this function will create the corresponding node
     *
     * @return true if the Secondary Upload Node (if enabled) are valid, or the creation of the corresponding node is successful
     */
    private suspend fun checkOrCreateSecondaryUploadNodes(): Boolean {
        return runCatching {
            checkOrCreateCameraUploadsNodeUseCase(
                folderName = context.getString(R.string.section_secondary_media_uploads),
                folderType = CameraUploadFolderType.Secondary,
            )
        }.onFailure {
            Timber.e(it)
        }.isSuccess
    }

    /**
     * Retrieve the files from the media store and insert them in the database
     */
    //@Karma
    private suspend fun scanFiles() {
        Timber.d("Get Pending Files from Media Store")
        sendCheckUploadStatus()
        processCameraUploadsMediaUseCase(tempRoot = tempRoot)
    }

    /**
     * Get pending records from the database and populate them with required information for upload
     * - Retrieve the pending camera uploads records from the database
     * - Filter the camera uploads based on video compression size condition
     * - Rename the camera uploads records
     * - Check the existence of a node corresponding to the [CameraUploadsRecord]
     * - Extract the gps coordinates and set in the respective [CameraUploadsRecord]
     *
     * @param primaryUploadNodeId the primary target [NodeId]
     * @param secondaryUploadNodeId the secondary target [NodeId]
     * @return the list of pending [CameraUploadsRecord] to upload
     */
    //@Karma
    private suspend fun getAndPrepareRecords(
        primaryUploadNodeId: NodeId,
        secondaryUploadNodeId: NodeId,
    ): List<CameraUploadsRecord>? {
        Timber.d("Get Pending Files from Database")
        return getPendingCameraUploadsRecords()
            .takeIf { it.isNotEmpty() }
            ?.let { pendingRecords ->
                Timber.d("Check compression requirements for ${pendingRecords.size} files")
                filterCameraUploadsRecords(pendingRecords)
            }
            ?.let { filteredRecords ->
                Timber.d("Retrieve existence in target node for ${filteredRecords.size} files")
                getExistenceInTargetNode(
                    filteredRecords,
                    primaryUploadNodeId,
                    secondaryUploadNodeId
                )
            }
            ?.let { recordsWithExistenceInTargetNode ->
                Timber.d("Check renaming for ${recordsWithExistenceInTargetNode.size} files")
                renameCameraUploadsRecords(
                    recordsWithExistenceInTargetNode,
                    primaryUploadNodeId,
                    secondaryUploadNodeId,
                )
            }
            ?.let { renamedRecordsWithExistenceInTargetNode ->
                Timber.d("Retrieve gps coordinates for ${renamedRecordsWithExistenceInTargetNode.size} files")
                getGpsCoordinates(renamedRecordsWithExistenceInTargetNode)
            }.also {
                val existsInTargetNodeCount =
                    it?.filter { record -> record.existsInTargetNode == true }?.size
                val existsInCloudDriveCount =
                    it?.filter { record -> record.existsInTargetNode == false && record.existingNodeId != null }?.size
                val doesNotExistInCloudDriveCount =
                    it?.filter { record -> record.existingNodeId == null }?.size
                Timber.d("$existsInTargetNodeCount files already exist in target node")
                Timber.d("$existsInCloudDriveCount files already exists in cloud drive")
                Timber.d("$doesNotExistInCloudDriveCount files does not exist in target node")
                if (it == null) Timber.d("No pending files to upload")
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
        tempRoot: String,
    ) = uploadCameraUploadsRecords(
        records,
        primaryUploadNodeId,
        secondaryUploadNodeId,
        tempRoot
    )
        .catch { throwable ->
            Timber.e(throwable)
            abortWork(reason = CameraUploadsFinishedReason.ERROR_DURING_PROCESS)
        }
        .collect { progressEvent ->
            processProgressEvent(progressEvent)
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
        val videoQuality = getUploadVideoQualityUseCase()
        return if (videoQuality == VideoQuality.ORIGINAL) {
            records
        } else {
            totalVideoSize = records
                .filter { it.type == CameraUploadsRecordType.TYPE_VIDEO }
                .mapNotNull { getFileByPathUseCase(it.filePath)?.length() }
                .sumOf { it / (1024 * 1024) } // Convert to MB

            if (totalVideoSize == 0L || canCompressVideo(totalVideoSize)) {
                records
            } else {
                Timber.d("Compression queue bigger than setting, show notification to user.")
                sendVideoCompressionErrorStatus()
                records.filter { it.type == CameraUploadsRecordType.TYPE_PHOTO }
            }
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
            -> processToUploadEvent(progressEvent)

            is CameraUploadsTransferProgress.ToCopy,
            -> processToCopyEvent(progressEvent)

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

            is CameraUploadsTransferProgress.Compressing.Cancel,
            -> sendVideoCompressionErrorStatus()

            is CameraUploadsTransferProgress.Compressing.Successful,
            -> processCompressingSuccessful()

            is CameraUploadsTransferProgress.Error,
            -> processError(progressEvent.error)
        }
    }

    /**
     * Process a progress event of type [CameraUploadsTransferProgress.ToUpload]
     *
     * Update to upload transfer count
     *
     * @param progressEvent
     */
    private suspend fun processToUploadEvent(
        progressEvent: CameraUploadsTransferProgress.ToUpload,
    ) {
        updateToUploadCount(
            filePath = progressEvent.record.tempFilePath
                .takeIf { fileSystemRepository.doesFileExist(it) }
                ?: progressEvent.record.filePath,
            folderType = progressEvent.record.folderType,
            tag = progressEvent.transferEvent.transfer.tag,
        )
    }


    /**
     * Process a progress event of type [CameraUploadsTransferProgress.ToCopy]
     *
     * Update to upload transfer count
     *
     * @param progressEvent
     */
    private suspend fun processToCopyEvent(
        progressEvent: CameraUploadsTransferProgress.ToCopy,
    ) {
        updateToUploadCount(
            filePath = progressEvent.record.tempFilePath
                .takeIf { fileSystemRepository.doesFileExist(it) }
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
        sendVideoCompressionOutOfSpaceStatus()
        abortWork(reason = CameraUploadsFinishedReason.INSUFFICIENT_LOCAL_STORAGE_SPACE)
    }

    /**
     * Process a progress event of type [CameraUploadsTransferProgress.Compressing.Successful]
     *
     * Cancel the compression notification
     */
    private suspend fun processCompressingSuccessful() {
        sendCompressionSuccessUploadStatus()
    }

    /**
     * Process a progress event of type [CameraUploadsTransferProgress.Error]
     *
     * @param error the throwable returned in the progress event
     */
    private suspend fun processError(error: Throwable) {
        when (error) {
            is NotEnoughStorageException -> {
                sendNotEnoughStorageStatus()
                abortWork(CameraUploadsFinishedReason.INSUFFICIENT_LOCAL_STORAGE_SPACE)
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

    /**
     * Perform the following operations when the local Primary Folder is invalid:
     * - Display an error notification
     * - Reset the Primary Folder local path
     * - Notify the app that the Primary Folder local path has changed
     * - Disable the Camera Uploads functionality
     */
    private suspend fun handleInvalidLocalPrimaryFolder() {
        sendFolderUnavailableStatus(CameraUploadFolderType.Primary)
        setPrimaryFolderLocalPathUseCase("")
        broadcastCameraUploadsSettingsActionUseCase(CameraUploadsSettingsAction.DisableCameraUploads)
    }

    /**
     * Perform the following operations when the local Secondary Folder is invalid:
     * - Display an error notification
     * - Reset the Secondary Folder local path
     * - Notify the app that the Secondary Folder local path has changed
     * - Disable the Media Uploads functionality
     */
    private suspend fun handleInvalidLocalSecondaryFolder() {
        sendFolderUnavailableStatus(CameraUploadFolderType.Secondary)
        disableMediaUploadSettingsUseCase()
        setSecondaryFolderLocalPathUseCase("")
        broadcastCameraUploadsSettingsActionUseCase(CameraUploadsSettingsAction.DisableMediaUploads)
    }

    /**
     * When the user is not logged in, perform a Complete Fast Login procedure
     *
     * @return [Boolean] true if the login process successful otherwise false
     */
    private suspend fun isLoginSuccessful(): Boolean {
        Timber.d("Waiting for the user to complete the Fast Login procedure")

        // arbitrary retry value
        var retry = 3
        while (loginMutex.isLocked && retry > 0) {
            Timber.d("Wait for the login lock to be available")
            delay(1000)
            retry--
        }

        return if (!loginMutex.isLocked) {
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
     * Create the temporary cache folder for the CU process
     *
     * @return true if the creation of the temporary cache folder succeed, false otherwise
     */
    private suspend fun createTempCacheFile(): Boolean {
        return runCatching {
            tempRoot = createCameraUploadsTemporaryRootDirectoryUseCase()
        }.onFailure {
            Timber.e(it)
        }.isSuccess
    }

    /**
     * Delete the temporary cache folder for the CU process
     *
     * @return true if the creation of the temporary cache folder succeed, false otherwise
     */
    private suspend fun deleteTempCacheFile(): Boolean {
        return runCatching {
            deleteCameraUploadsTemporaryRootDirectoryUseCase()
        }.onFailure {
            Timber.e(it)
        }.isSuccess
    }

    /**
     * Abort the worker
     * This function is called if an error is caught inside the CameraUploadsWorker
     * and will cancel the child jobs
     *
     * @param reason the reason why the worker is aborted
     * @param restartMode the restart mode to apply after the completion of the worker
     */
    private suspend fun abortWork(
        reason: CameraUploadsFinishedReason,
        restartMode: CameraUploadsRestartMode = CameraUploadsRestartMode.Reschedule,
    ) {
        if (finishedReason != null) return

        Timber.e("Camera Uploads process aborted: $reason")

        finishedReason = reason
        this@CameraUploadsWorker.restartMode = restartMode

        listOf(
            retrieveFilesJob,
            uploadJob,
        ).forEach { it?.cancelAndJoin() }
    }

    /**
     * Clean the resources and cancel the monitor jobs
     */
    private suspend fun cleanResources() {
        cancelJobs()
        deleteTempCacheFile()
    }

    private suspend fun cancelJobs() {
        listOf(
            retrieveFilesJob,
            uploadJob,
            monitorUploadPauseStatusJob,
            monitorConnectivityStatusJob,
            monitorBatteryLevelAndChargingStatusesJob,
            monitorStorageOverQuotaStatusJob,
            monitorParentNodesDeletedJob,
            sendBackupHeartbeatJob,
            monitorCameraUploadsTransfers,
        ).forEach { it?.cancelAndJoin() }
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

    /**
     *  Update total to upload count
     *
     *  @param filePath
     *  @param folderType
     *  @param tag the tag associated to the transfer, null if no transfer (ie. copy)
     */
    private suspend fun updateToUploadCount(
        filePath: String,
        folderType: CameraUploadFolderType,
        tag: Int? = null,
    ) {
        val bytes = getFileByPathUseCase(filePath)?.length() ?: 0
        increaseTotalToUpload(
            cameraUploadFolderType = folderType,
            bytesToUpload = bytes,
            tag = tag,
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
            bytesUploaded = getFileByPathUseCase(filePath)?.length() ?: 0,
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
            tag = transfer.tag,
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
     *  @param tag the tag associated to the transfer
     */
    private suspend fun updateUploadedCount(
        cameraUploadFolderType: CameraUploadFolderType,
        isFinished: Boolean,
        nodeHandle: Long?,
        recordId: Long,
        bytesUploaded: Long,
        tag: Int? = null,
    ) {
        increaseTotalUploaded(
            cameraUploadFolderType = cameraUploadFolderType,
            isFinished = isFinished,
            nodeHandle = nodeHandle,
            recordId = recordId,
            bytesUploaded = bytesUploaded,
            tag = tag,
        )
        displayUploadProgress()
    }

    private suspend fun canCompressVideo(queueSize: Long): Boolean =
        (monitorBatteryInfoUseCase().first().isCharging || !isChargingRequiredUseCase(queueSize)).also {
            if (!it) Timber.d("Charging required for video compression of $queueSize MB")
        }

    private suspend fun displayUploadProgress() {
        runCatching {
            // refresh UI every 1 seconds to avoid too much workload on main thread
            lastUpdated = with(timeSystemRepository.getCurrentTimeInMillis()) {
                if (this - lastUpdated >= ON_TRANSFER_UPDATE_REFRESH_MILLIS) this
                else return
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
                    showUploadProgress(
                        totalUploaded,
                        totalToUpload,
                        totalUploadedBytes,
                        totalUploadBytes,
                        progressPercent.floatValue,
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
        progress: Float,
        areUploadsPaused: Boolean,
    ) {
        runCatching {
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
        }.onFailure {
            Timber.w(it)
        }
    }

    /**
     *  Display a notification for video compression progress
     */
    private suspend fun showVideoCompressionProgress(
        progress: Float,
        currentFileIndex: Int,
        totalCount: Int,
    ) {
        runCatching {
            setProgress(
                workDataOf(
                    STATUS_INFO to COMPRESSION_PROGRESS,
                    CURRENT_PROGRESS to progress,
                    CURRENT_FILE_INDEX to currentFileIndex,
                    TOTAL_COUNT to totalCount,
                )
            )
        }.onFailure {
            Timber.w(it)
        }
    }

    /**
     *  Notify observers that the Camera Uploads has started
     */
    private suspend fun sendStartUploadStatus() {
        runCatching {
            setProgress(workDataOf(STATUS_INFO to START))
        }.onFailure {
            Timber.w(it)
        }
    }

    /**
     *  Notify observers that the Camera Uploads has started
     */
    private suspend fun sendFinishedStatus(reason: CameraUploadsFinishedReason) {
        runCatching {
            setProgress(
                workDataOf(
                    STATUS_INFO to FINISHED,
                    CameraUploadsWorkerStatusConstant.FINISHED_REASON to reason.name,
                )
            )
        }.onFailure {
            Timber.w(it)
        }
    }


    /**
     * Notify observers that the video compression has succeeded
     */
    private suspend fun sendCompressionSuccessUploadStatus() {
        runCatching {
            setProgress(workDataOf(STATUS_INFO to COMPRESSION_SUCCESS))
        }.onFailure {
            Timber.w(it)
        }
    }

    /**
     * Notify observers that the Camera Uploads is in Check upload step
     */
    private suspend fun sendCheckUploadStatus() {
        runCatching {
            setProgress(workDataOf(STATUS_INFO to CHECK_FILE_UPLOAD))
        }.onFailure {
            Timber.w(it)
        }
    }

    /**
     * Notify observers that the cloud storage does not have enough space
     */
    private suspend fun sendStorageOverQuotaStatus() {
        runCatching {
            setProgress(workDataOf(STATUS_INFO to STORAGE_OVER_QUOTA))
        }.onFailure {
            Timber.w(it)
        }
    }

    /**
     * Notify observers that the device does not have enough local storage
     * for video compression
     */
    private suspend fun sendVideoCompressionOutOfSpaceStatus() {
        runCatching {
            setProgress(workDataOf(STATUS_INFO to OUT_OF_SPACE))
        }.onFailure {
            Timber.w(it)
        }
    }

    /**
     * Notify observers that the device does not have enough local storage
     * for creating temporary files
     */
    private suspend fun sendNotEnoughStorageStatus() {
        runCatching {
            setProgress(workDataOf(STATUS_INFO to NOT_ENOUGH_STORAGE))
        }.onFailure {
            Timber.w(it)
        }
    }

    /**
     * Notify observers that an error happened during video compression
     */
    private suspend fun sendVideoCompressionErrorStatus() {
        runCatching {
            setProgress(workDataOf(STATUS_INFO to COMPRESSION_ERROR))
        }.onFailure {
            Timber.w(it)
        }
    }

    /**
     * Notify observers that Camera Uploads cannot launch due to the Folder being unavailable
     *
     * @param cameraUploadsFolderType
     */
    private suspend fun sendFolderUnavailableStatus(cameraUploadsFolderType: CameraUploadFolderType) {
        runCatching {
            setProgress(
                workDataOf(
                    STATUS_INFO to FOLDER_UNAVAILABLE,
                    FOLDER_TYPE to cameraUploadsFolderType.ordinal
                )
            )
        }.onFailure {
            Timber.w(it)
        }
    }

    /**
     *  Update backup state
     *
     *  @param backupState
     */
    private suspend fun updateBackupState(backupState: BackupState) {
        runCatching {
            updateCameraUploadsBackupStatesUseCase(backupState)
        }.onFailure {
            Timber.e(it)
        }
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
     * @param tag the tag associated to the transfer
     */
    private suspend fun increaseTotalToUpload(
        cameraUploadFolderType: CameraUploadFolderType,
        bytesToUpload: Long,
        tag: Int?,
    ) = stateUpdateMutex.withLock {
        when (cameraUploadFolderType) {
            CameraUploadFolderType.Primary -> state.value.primaryCameraUploadsState
            CameraUploadFolderType.Secondary -> state.value.secondaryCameraUploadsState
        }.let { state ->
            updateState(
                cameraUploadFolderType = cameraUploadFolderType,
                toUploadCount = state.toUploadCount + 1,
                bytesToUploadCount = state.bytesToUploadCount + bytesToUpload,
                tag = tag
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
     * @param tag the tag associated to the transfer
     */
    private suspend fun increaseTotalUploaded(
        cameraUploadFolderType: CameraUploadFolderType,
        isFinished: Boolean,
        bytesUploaded: Long,
        nodeHandle: Long?,
        recordId: Long,
        tag: Int?,
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
                    tag = tag,
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
        tag: Int? = null,
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

                val tagList =
                    ArrayList(this.uploadTags).apply {
                        tag?.let {
                            if (bytesFinishedUploadedCount == null) {
                                if (!this.contains(tag)) this.add(tag)
                            } else {
                                if (this.contains(tag)) this.remove(tag)
                            }
                        }
                    }.toList()

                copy(
                    lastTimestamp = lastTimestamp ?: this.lastTimestamp,
                    lastHandle = lastHandle ?: this.lastHandle,
                    toUploadCount = toUploadCount ?: this.toUploadCount,
                    uploadedCount = uploadedCount ?: this.uploadedCount,
                    bytesToUploadCount = bytesToUploadCount ?: this.bytesToUploadCount,
                    bytesInProgressUploadedTable = bytesTable
                        ?: this.bytesInProgressUploadedTable,
                    bytesFinishedUploadedCount = bytesFinishedUploadedCount
                        ?: this.bytesFinishedUploadedCount,
                    uploadTags = tagList
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
