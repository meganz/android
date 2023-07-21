package mega.privacy.android.app.cameraupload

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import mega.privacy.android.app.AndroidCompletedTransfer
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.transfers.model.mapper.LegacyCompletedTransferMapper
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.ImageProcessor
import mega.privacy.android.app.utils.PreviewUtils
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.data.gateway.PermissionGateway
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsState
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
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.BroadcastStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.camerauploads.DeleteCameraUploadsTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.camerauploads.DisableCameraUploadsUseCase
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetDefaultNodeHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadFolderHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.HandleLocalIpChangeUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsChargingUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsPrimaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsSecondaryFolderSetUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.camerauploads.ProcessMediaForUploadUseCase
import mega.privacy.android.domain.usecase.camerauploads.SendBackupHeartBeatSyncUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCoordinatesUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetOriginalFingerprintUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetSecondaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupPrimaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateCameraUploadsBackupHeartbeatStatusUseCase
import mega.privacy.android.domain.usecase.camerauploads.UpdateCameraUploadsBackupStatesUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import mega.privacy.android.domain.usecase.transfer.AddCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfer.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfer.CancelAllUploadTransfersUseCase
import mega.privacy.android.domain.usecase.transfer.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfer.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfer.ResetTotalUploadsUseCase
import mega.privacy.android.domain.usecase.transfer.StartUploadUseCase
import mega.privacy.android.domain.usecase.workers.ScheduleCameraUploadUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.time.Instant
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

/**
 * Worker to run Camera Uploads
 */
@HiltWorker
class CameraUploadsWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val permissionsGateway: PermissionGateway,
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
    private val getVideoCompressionSizeLimitUseCase: GetVideoCompressionSizeLimitUseCase,
    private val isChargingRequired: IsChargingRequired,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val processMediaForUploadUseCase: ProcessMediaForUploadUseCase,
    private val getUploadFolderHandleUseCase: GetUploadFolderHandleUseCase,
    private val setPrimarySyncHandle: SetPrimarySyncHandle,
    private val setSecondarySyncHandle: SetSecondarySyncHandle,
    private val getDefaultNodeHandleUseCase: GetDefaultNodeHandleUseCase,
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorPausedTransfersUseCase: MonitorPausedTransfersUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorBatteryInfo: MonitorBatteryInfo,
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase,
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase,
    private val monitorChargingStoppedState: MonitorChargingStoppedState,
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
    private val disableCameraUploadsUseCase: DisableCameraUploadsUseCase,
    private val compressVideos: CompressVideos,
    private val resetMediaUploadTimeStamps: ResetMediaUploadTimeStamps,
    private val disableMediaUploadSettings: DisableMediaUploadSettings,
    private val createCameraUploadTemporaryRootDirectoryUseCase: CreateCameraUploadTemporaryRootDirectoryUseCase,
    private val deleteCameraUploadsTemporaryRootDirectoryUseCase: DeleteCameraUploadsTemporaryRootDirectoryUseCase,
    private val broadcastCameraUploadProgress: BroadcastCameraUploadProgress,
    private val scheduleCameraUploadUseCase: ScheduleCameraUploadUseCase,
    private val createTempFileAndRemoveCoordinatesUseCase: CreateTempFileAndRemoveCoordinatesUseCase,
    private val updateCameraUploadsBackupStatesUseCase: UpdateCameraUploadsBackupStatesUseCase,
    private val sendBackupHeartBeatSyncUseCase: SendBackupHeartBeatSyncUseCase,
    private val updateCameraUploadsBackupHeartbeatStatusUseCase: UpdateCameraUploadsBackupHeartbeatStatusUseCase,
    private val addCompletedTransferUseCase: AddCompletedTransferUseCase,
    private val completedTransferMapper: LegacyCompletedTransferMapper,
    private val setCoordinatesUseCase: SetCoordinatesUseCase,
    private val isChargingUseCase: IsChargingUseCase,
    private val stringWrapper: StringWrapper,
    private val monitorStorageOverQuotaUseCase: MonitorStorageOverQuotaUseCase,
    private val broadcastStorageOverQuotaUseCase: BroadcastStorageOverQuotaUseCase,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID =
            Constants.NOTIFICATION_CHANNEL_CAMERA_UPLOADS_ID
        private const val NOTIFICATION_CHANNEL_NAME =
            Constants.NOTIFICATION_CHANNEL_CAMERA_UPLOADS_NAME
        private const val NOTIFICATION_ID = Constants.NOTIFICATION_CAMERA_UPLOADS
        private const val PRIMARY_FOLDER_UNAVAILABLE_NOTIFICATION_ID = 1908
        private const val SECONDARY_FOLDER_UNAVAILABLE_NOTIFICATION_ID = 1909
        private const val COMPRESSION_ERROR_NOTIFICATION_ID = 1910
        private const val NOT_ENOUGH_STORAGE_NOTIFICATION_ID =
            Constants.NOTIFICATION_NOT_ENOUGH_STORAGE
        private const val OVER_STORAGE_QUOTA_NOTIFICATION_ID =
            Constants.NOTIFICATION_STORAGE_OVERQUOTA

        private const val LOW_BATTERY_LEVEL = 20
        private const val ON_TRANSFER_UPDATE_REFRESH_MILLIS = 1000
        private const val CONCURRENT_UPLOADS_LIMIT = 16
    }

    /**
     * Notification manager used to display notifications
     */
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * True if the battery level of the device is above
     * the required battery level to run the CU
     */
    private var deviceAboveMinimumBatteryLevel: Boolean = true

    /**
     * Default notification pending intent
     * that will redirect to the manager activity with a [Constants.ACTION_CANCEL_CAM_SYNC] action
     */
    private val defaultPendingIntent: PendingIntent by lazy {
        Intent(context, ManagerActivity::class.java).apply {
            action = Constants.ACTION_CANCEL_CAM_SYNC
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.PENDING_TAB)
        }.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
    }

    /**
     * Temp root path used for generating temporary files in the CU process
     * This folder is created at the beginning of the CU process
     * and deleted at the end of the process
     */
    private lateinit var tempRoot: String

    /**
     * Camera Upload State holder
     */
    private val cameraUploadState = CameraUploadsState()

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
     * True if all transfers paused
     */
    private var areUploadsPaused: Boolean = false

    /**
     * In order to not overload the memory of the app,
     * limit the number of concurrent uploads to [CONCURRENT_UPLOADS_LIMIT]
     */
    private val semaphore = Semaphore(CONCURRENT_UPLOADS_LIMIT)

    override suspend fun doWork() = coroutineScope {
        try {
            Timber.d("Start CU Worker")
            scope = this

            // Signal to not kill the worker if the app is killed
            setForegroundAsync(getForegroundInfo())

            withContext(ioDispatcher) {
                initService()
                if (hasMediaPermission() && isLoginSuccessful() && canRunCameraUploads()) {
                    Timber.d("Calling startWorker() successful. Starting Camera Uploads")
                    cancelNotifications()
                    checkUploadNodes()
                    startUploadAndCompression()
                    onQueueComplete()
                    endService()
                    Result.success()
                } else {
                    Timber.w("Calling startWorker() failed. Proceed to handle error")
                    endService(aborted = true)
                    Result.failure()
                }
            }
        } catch (throwable: Throwable) {
            Timber.e(throwable, "Worker cancelled")
            endService(aborted = true)
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo() = createCameraUploadsForegroundInfo()

    private fun monitorUploadPauseStatus() {
        monitorUploadPauseStatusJob = scope?.launch(ioDispatcher) {
            monitorPausedTransfersUseCase().collect {
                areUploadsPaused = it
                updateBackupState(if (it) BackupState.PAUSE_UPLOADS else BackupState.ACTIVE)
                displayUploadProgressNotification()
            }
        }
    }

    private fun monitorConnectivityStatus() {
        monitorConnectivityStatusJob = scope?.launch(ioDispatcher) {
            monitorConnectivityUseCase().collect {
                if (!it || isWifiNotSatisfiedUseCase()) {
                    endService(
                        cancelMessage = "Camera Upload by Wifi only but Mobile Network - Cancel Camera Upload",
                        aborted = true
                    )
                }
            }
        }
    }

    private fun monitorBatteryLevelStatus() {
        monitorBatteryLevelStatusJob = scope?.launch(ioDispatcher) {
            monitorBatteryInfo().collect {
                deviceAboveMinimumBatteryLevel = (it.level > LOW_BATTERY_LEVEL || it.isCharging)
                if (!deviceAboveMinimumBatteryLevel) {
                    endService(
                        cancelMessage = "Low Battery - Cancel Camera Upload",
                        aborted = true
                    )
                }
            }
        }
    }

    private fun monitorChargingStoppedStatus() {
        monitorChargingStoppedStatusJob = scope?.launch(ioDispatcher) {
            monitorChargingStoppedState().collect {
                if (isChargingRequired(totalVideoSize)) {
                    Timber.d("Detected device stops charging.")
                    videoCompressionJob?.cancel()
                }
            }
        }
    }

    private fun monitorStorageOverQuotaStatus() {
        monitorStorageOverQuotaStatusJob = scope?.launch(ioDispatcher) {
            monitorStorageOverQuotaUseCase().collect {
                if (it) {
                    showStorageOverQuotaNotification()
                    endService(
                        cancelMessage = "Storage Quota Filled - Cancel Camera Upload",
                        aborted = true
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

    /**
     * Check if the device has the required permission
     *
     * @return true if the device has the required permission
     */
    private fun hasMediaPermission(): Boolean {
        val hasMediaPermissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permissionsGateway.hasPermissions(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                ) || permissionsGateway.hasPermissions(
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsGateway.hasPermissions(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                )
            } else {
                permissionsGateway.hasPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                )
            }
        return hasMediaPermissions.also {
            Timber.d("Device has required permissions $it")
        }
    }

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
        establishCameraUploadsSyncHandlesUseCase()
        return true
    }

    private suspend fun areFoldersCheckedAndEstablished(): Boolean {
        return if (areFoldersEstablished()) {
            true
        } else {
            runCatching { establishFolders() }.isSuccess.also {
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
            if (!it) Timber.w("Camera Upload sync disabled")
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
        if (primarySyncHandle == MegaApiJava.INVALID_HANDLE) {
            return false
        }
        val isPrimaryFolderInRubbish = isNodeInRubbishOrDeletedUseCase(primarySyncHandle)
        val result =
            !isPrimaryFolderInRubbish || (getPrimaryFolderHandle() != MegaApiJava.INVALID_HANDLE)
        Timber.d("Primary Folder Established $result")
        return result
    }

    /**
     * Checks whether the Secondary Folder is established
     *
     * @return true if the Secondary Folder handle is a valid handle, and false if otherwise
     */
    private suspend fun isSecondaryFolderEstablished(): Boolean {
        val secondarySyncHandle = getSecondaryFolderHandle()
        if (secondarySyncHandle == MegaApiJava.INVALID_HANDLE) {
            return false
        }
        val isSecondaryFolderInRubbish = isNodeInRubbishOrDeletedUseCase(getSecondaryFolderHandle())
        Timber.d("Secondary Folder Established ${!isSecondaryFolderInRubbish}")
        return !isSecondaryFolderInRubbish
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

            val primaryHandle = getPrimaryFolderHandle()

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

            val secondaryHandle = getSecondaryFolderHandle()

            if (secondaryHandle == MegaApiJava.INVALID_HANDLE) {
                Timber.d("Proceed to create the Primary Folder")
                createAndSetupSecondaryUploadFolder()
            } else {
                Timber.d("Secondary Handle retrieved from getSecondaryFolderHandle(): $secondaryHandle")
                setSecondarySyncHandle(secondaryHandle)
            }
        }
    }

    private suspend fun checkUploadNodes() {
        Timber.d("Get Pending Files from Media Store Database")
        showCheckUploadsNotification()

        val primaryUploadNode =
            getNodeByIdUseCase(NodeId(getUploadFolderHandleUseCase(CameraUploadFolderType.Primary)))
        if (primaryUploadNode == null) {
            Timber.d("ERROR: Primary Parent Folder is NULL")
            endService(aborted = true)
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

    private suspend fun startUploadAndCompression() {
        val finalList = getPendingSyncRecords().also {
            Timber.d("Total File to upload ${it.size}")
        }
        startHeartbeat()
        if (finalList.isNotEmpty()) {
            startParallelUpload(finalList, isCompressedVideo = false)
        }
        if (compressedVideoPending()) {
            startVideoCompression()
            val compressedList = getVideoSyncRecordsByStatus(SyncStatus.STATUS_PENDING)
            if (compressedList.isNotEmpty()) {
                Timber.d("Start to upload ${compressedList.size} compressed videos.")
                startParallelUpload(compressedList, isCompressedVideo = true)
            }
        }
    }

    private suspend fun startParallelUpload(
        finalList: List<SyncRecord>,
        isCompressedVideo: Boolean,
    ) = coroutineScope {
        areUploadsPaused = areTransfersPausedUseCase()

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
                updateToUploadCount(record)
                uploadFileAsyncList.add(launch {
                    semaphore.acquire()
                    Timber.d("Copy from node, file timestamp is: ${record.timestamp}")
                    updateToUploadCount(record)
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
                        updateToUploadCount(record)
                        val lastModified = getLastModifiedTime(record)
                        startUploadUseCase(
                            localPath = it.path,
                            parentNodeId = parentNodeId,
                            fileName = record.fileName,
                            modificationTime = lastModified / 1000,
                            appData = Constants.APP_DATA_CU,
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
                        record.localPath?.let {
                            deleteSyncRecord(it, record.isSecondary)
                        }
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
                ?: record.localPath?.let { File(it).takeIf { localFile -> localFile.exists() } }
        } else {
            record.localPath?.let { File(it).takeIf { localFile -> localFile.exists() } }
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
                        tempRoot,
                        record
                    )
                }.retryWhen { cause, attempt ->
                    if (cause is NotEnoughStorageException) {
                        if (attempt >= 60) {
                            if (cameraUploadState.totalPendingCount == 0) {
                                showNotEnoughStorageNotification()
                                Timber.w("Stop service due to out of space issue")
                                endService(aborted = true)
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
                        record.localPath?.let { newPath ->
                            deleteSyncRecord(newPath, isSecondary = record.isSecondary)
                        }
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
        updateBackupState(if (areUploadsPaused) BackupState.PAUSE_UPLOADS else BackupState.ACTIVE)

        sendBackupHeartbeatJob =
            scope?.launch(ioDispatcher) {
                sendBackupHeartBeatSyncUseCase(cameraUploadState)
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
            is TransferEvent.TransferUpdateEvent -> onTransferUpdated(globalTransfer, record)
            is TransferEvent.TransferTemporaryErrorEvent -> onTransferTemporaryError(globalTransfer)
            // No further action necessary for these Scenarios
            is TransferEvent.TransferStartEvent,
            is TransferEvent.TransferDataEvent,
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
            updateUploadedCountAfterTransfer(record, transfer)
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
        record: SyncRecord,
    ) {
        val transfer = globalTransfer.transfer
        runCatching {
            updateUploadedCountAfterTransfer(record, transfer)
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
                        newNodeName = record.fileName.orEmpty(),
                    )
                    (getNodeByIdUseCase(nodeId) as? TypedFileNode)?.let { retrievedNode ->
                        val fingerprint = retrievedNode.fingerprint
                        val isSecondary = retrievedNode.parentId == NodeId(
                            getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)
                        )
                        // Delete the Camera Upload sync record by fingerprint
                        fingerprint?.let {
                            deleteSyncRecordByFingerprint(
                                originalPrint = fingerprint,
                                newPrint = fingerprint,
                                isSecondary = isSecondary,
                            )
                        }
                        updateUploadedCountAfterCopy(record)

                        Timber.d("Copy node successful")
                    }
                }
            }
        }.onFailure { error ->
            Timber.e("Copy node error: $error")
        }

    }

    private fun getLastModifiedTime(file: SyncRecord): Long {
        val source = file.localPath?.let { File(it) }
        return source?.lastModified() ?: 0
    }

    private suspend fun onQueueComplete() {
        Timber.d("onQueueComplete")
        resetTotalUploadsUseCase()
        cameraUploadState.resetUploadsCounts()
    }

    /**
     * Executes certain behavior when the Primary Folder is disabled
     */
    private suspend fun handlePrimaryFolderDisabled() {
        showFolderUnavailableNotification(CameraUploadFolderType.Primary)
        disableCameraUploadsUseCase()
        setPrimaryFolderLocalPathUseCase(Constants.INVALID_NON_NULL_VALUE)
        setSecondaryFolderLocalPathUseCase(Constants.INVALID_NON_NULL_VALUE)
        // Refresh SettingsCameraUploadsFragment
        context.sendBroadcast(Intent(BroadcastConstants.ACTION_REFRESH_CAMERA_UPLOADS_SETTING))
    }

    /**
     * Executes certain behavior when the Secondary Folder is disabled
     */
    private suspend fun handleSecondaryFolderDisabled() {
        showFolderUnavailableNotification(CameraUploadFolderType.Secondary)
        // Disable Media Uploads only
        resetMediaUploadTimeStamps()
        disableMediaUploadSettings()
        setSecondaryFolderLocalPathUseCase(SettingsConstants.INVALID_PATH)
        context.sendBroadcast(Intent(BroadcastConstants.ACTION_DISABLE_MEDIA_UPLOADS_SETTING))
    }

    /**
     * When the user is not logged in, perform a Complete Fast Login procedure
     * @return [Boolean] true if the login process successful otherwise false
     */
    private suspend fun isLoginSuccessful(): Boolean {
        Timber.d("Waiting for the user to complete the Fast Login procedure")

        // arbitrary retry value
        var retry = 3
        while (MegaApplication.isLoggingIn && retry > 0) {
            Timber.d("Wait for the isLoggingIn lock to be available")
            delay(1000)
            retry--
        }

        if (!MegaApplication.isLoggingIn) {
            // Legacy support: isLoggingIn needs to be set in order to inform other parts of the
            // app that a Login Procedure is occurring
            MegaApplication.isLoggingIn = true
            val result = runCatching { backgroundFastLoginUseCase() }.onFailure {
                Timber.e(it, "performCompleteFastLogin exception")
            }
            MegaApplication.isLoggingIn = false
            if (result.isSuccess) {
                Timber.d("Complete Fast Login procedure successful. Get cookies settings after login")
                MegaApplication.getInstance().checkEnabledCookies()
            }
            return result.isSuccess
        } else {
            Timber.e("isLoggingIn lock not available, cannot perform backgroundFastLogin. Stop process")
            return false
        }
    }

    /**
     * Gets the Primary Folder handle
     *
     * @return the Primary Folder handle
     */
    private suspend fun getPrimaryFolderHandle(): Long =
        getDefaultNodeHandleUseCase(context.getString(R.string.section_photo_sync))

    /**
     * Gets the Secondary Folder handle
     *
     * @return the Secondary Folder handle
     */
    private suspend fun getSecondaryFolderHandle(): Long {
        // get Secondary folder handle of user
        val secondarySyncHandle = getUploadFolderHandleUseCase(CameraUploadFolderType.Secondary)
        if (secondarySyncHandle == MegaApiJava.INVALID_HANDLE || getNodeByIdUseCase(
                NodeId(secondarySyncHandle)
            ) == null
        ) {
            // if it's invalid or deleted then return the default value
            return getDefaultNodeHandleUseCase(context.getString(R.string.section_secondary_media_uploads))
        }
        return secondarySyncHandle
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
     * Proceed with the end of the service
     * Clean up the resources
     *
     * @param cancelMessage message associated to cancel
     * @param aborted true if the service is ended prematurely
     */
    private suspend fun endService(
        cancelMessage: String = "Process completed",
        aborted: Boolean = false,
    ) = withContext(NonCancellable) {
        Timber.d("Finish Camera upload process: $cancelMessage")

        deleteCameraUploadsTemporaryRootDirectoryUseCase()
        sendStatusToBackupCenter(aborted = aborted)
        cancelAllPendingTransfers()
        broadcastProgress(100, 0)
        cancelNotification()

        // isStopped signals means that the worker has been cancelled from the WorkManager
        // If the process has been stopped for another reason, we can re-schedule the worker
        // to perform at a later time
        if (!isStopped) {
            Timber.d("Schedule Camera Upload")
            scheduleCameraUploadUseCase()
        }

        if (aborted) {
            Timber.d("Camera Upload stopped prematurely, cancel all running coroutines")
            scope?.coroutineContext?.cancelChildren(CancellationException(cancelMessage))
        } else {
            Timber.d("Camera Upload finished normally, cancel all flow monitoring")
            monitorUploadPauseStatusJob?.cancel()
            monitorConnectivityStatusJob?.cancel()
            monitorBatteryLevelStatusJob?.cancel()
            monitorChargingStoppedStatusJob?.cancel()
            sendBackupHeartbeatJob?.cancel()
            monitorStorageOverQuotaStatusJob?.cancel()
        }
    }

    /**
     * Send the status of Camera Uploads to back up center
     *
     * @param aborted true if the Camera Uploads has been stopped prematurely
     */
    private suspend fun sendStatusToBackupCenter(aborted: Boolean) {
        if (monitorConnectivityUseCase().value) {
            if (aborted)
                sendTransfersInterruptedInfoToBackupCenter()
            else
                sendTransfersUpToDateInfoToBackupCenter()
        }
    }

    /**
     * Sends the appropriate Backup States and Heartbeat Statuses on both Primary and
     * Secondary folders when the active Camera Uploads is interrupted by other means (e.g.
     * no user credentials, Wi-Fi not turned on)
     */
    private suspend fun sendTransfersInterruptedInfoToBackupCenter() {
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
        // Update both Primary and Secondary Heartbeat Statuses to UP_TO_DATE
        updateBackupHeartbeatStatus(HeartbeatStatus.UP_TO_DATE)
    }

    private suspend fun transferFinished(
        transfer: Transfer,
        error: MegaException?,
        record: SyncRecord,
    ) {
        val path = transfer.localPath
        if (transfer.state == TransferState.STATE_COMPLETED) {
            val androidCompletedTransfer = AndroidCompletedTransfer(transfer, error, context)
            addCompletedTransferUseCase(completedTransferMapper(androidCompletedTransfer))
        }
        error?.let {
            Timber.d("Image Sync Finished, Error Code: ${it.errorCode}")
            if (error is QuotaExceededMegaException) {
                Timber.w("Over quota error: ${error.errorCode}")
                showStorageOverQuotaNotification()
                endService(aborted = true)
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
            }
            val src = record.localPath?.let { File(it) }
            if (src != null && src.exists()) {
                Timber.d("Creating preview")
                val previewDir = PreviewUtils.getPreviewFolder(context)
                val preview = File(
                    previewDir,
                    MegaApiAndroid.handleToBase64(transfer.nodeHandle) + FileUtil.JPG_EXTENSION
                )
                val thumbDir = ThumbnailUtils.getThumbFolder(context)
                val thumb = File(
                    thumbDir,
                    MegaApiAndroid.handleToBase64(transfer.nodeHandle) + FileUtil.JPG_EXTENSION
                )
                if (FileUtil.isVideoFile(path)) {
                    val img = record.localPath?.let { File(it) }
                    if (!preview.exists()) {
                        ImageProcessor.createVideoPreview(
                            context,
                            img,
                            preview
                        )
                    }
                    ImageProcessor.createThumbnail(img, thumb)
                } else if (MimeTypeList.typeForName(path).isImage) {
                    if (!preview.exists()) {
                        ImageProcessor.createImagePreview(src, preview)
                    }
                    ImageProcessor.createThumbnail(src, thumb)
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
     *  @param record the [SyncRecord] associated to the file to transfer
     */
    private suspend fun updateToUploadCount(
        record: SyncRecord,
    ) {
        with(cameraUploadState) {
            val bytes = record.localPath?.let { File(it).length() } ?: 0L
            if (record.isSecondary) {
                secondaryCameraUploadsState.toUploadCount++
                secondaryCameraUploadsState.bytesToUploadCount += bytes
            } else {
                primaryCameraUploadsState.toUploadCount++
                primaryCameraUploadsState.bytesToUploadCount += bytes
            }
        }
        displayUploadProgressNotification()
    }

    /**
     *  Update total uploaded count after copying a node
     *
     *  @param record the [SyncRecord] associated to the file to transfer
     */
    private suspend fun updateUploadedCountAfterCopy(
        record: SyncRecord,
    ) {
        val cameraUploadFolderType =
            if (record.isSecondary) CameraUploadFolderType.Secondary
            else CameraUploadFolderType.Primary
        val isFinished = true
        val nodeHandle = record.nodeHandle
        val recordId = record.id
        val bytesTransferred = record.localPath?.let { File(it).length() } ?: 0L
        updateUploadedCount(
            cameraUploadFolderType,
            isFinished,
            nodeHandle,
            recordId,
            bytesTransferred
        )
    }

    /**
     *  Update total uploaded count after a transfer update or finish
     *
     *  @param record the [SyncRecord] associated to the file to transfer
     *  @param transfer the [Transfer] associated to the file to transfer.
     *                  The transfer can be ongoing or finished.
     */
    private suspend fun updateUploadedCountAfterTransfer(
        record: SyncRecord,
        transfer: Transfer,
    ) {
        val cameraUploadFolderType =
            if (record.isSecondary) CameraUploadFolderType.Secondary
            else CameraUploadFolderType.Primary
        val isFinished = transfer.isFinished
        val nodeHandle = transfer.nodeHandle
        val recordId = record.id
        val bytesTransferred = transfer.transferredBytes
        updateUploadedCount(
            cameraUploadFolderType,
            isFinished,
            nodeHandle,
            recordId,
            bytesTransferred
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
     *  @param bytesTransferred the bytes transferred for this file
     */
    private suspend fun updateUploadedCount(
        cameraUploadFolderType: CameraUploadFolderType,
        isFinished: Boolean,
        nodeHandle: Long?,
        recordId: Int?,
        bytesTransferred: Long,
    ) {
        with(cameraUploadState) {
            when (cameraUploadFolderType) {
                CameraUploadFolderType.Primary -> {
                    with(primaryCameraUploadsState) {
                        if (isFinished) {
                            uploadedCount++
                            lastTimestamp = Instant.now().epochSecond
                            nodeHandle?.let { lastHandle = it }
                            recordId?.let { bytesInProgressUploadedTable.remove(it) }
                            bytesFinishedUploadedCount += bytesTransferred
                        } else {
                            recordId?.let {
                                bytesInProgressUploadedTable[it] = bytesTransferred
                            }
                        }
                    }
                }

                else -> {
                    with(secondaryCameraUploadsState) {
                        if (isFinished) {
                            uploadedCount++
                            lastTimestamp = Instant.now().epochSecond
                            nodeHandle?.let { lastHandle = it }
                            recordId?.let { bytesInProgressUploadedTable.remove(it) }
                            bytesFinishedUploadedCount += bytesTransferred
                        } else {
                            recordId?.let {
                                bytesInProgressUploadedTable[it] = bytesTransferred
                            }
                        }
                    }
                }
            }
        }

        displayUploadProgressNotification()
    }

    /**
     * Update the timestamp of the last actions
     * This timestamp is updated when :
     * - the backup state is updated
     * - the heartbeat status is updated due to interrupted or finished process
     * - a transfer finished
     *
     * @param cameraUploadFolderType Whether primary or secondary folder.
     *                               If null, both folder timestamps are updated
     */
    private fun updateLastTimestamp(cameraUploadFolderType: CameraUploadFolderType? = null) {
        with(cameraUploadState) {
            when (cameraUploadFolderType) {
                CameraUploadFolderType.Primary ->
                    primaryCameraUploadsState.lastTimestamp = Instant.now().epochSecond

                CameraUploadFolderType.Secondary ->
                    secondaryCameraUploadsState.lastTimestamp = Instant.now().epochSecond

                else -> {
                    primaryCameraUploadsState.lastTimestamp = Instant.now().epochSecond
                    secondaryCameraUploadsState.lastTimestamp = Instant.now().epochSecond
                }
            }
        }
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
            cameraUploadState.resetUploadsCounts()
            totalVideoSize = getTotalVideoSizeInMB(fullList)
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
                                    onCompressFailed(fullList.first { record -> record.id == it.id })
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
                                    onCompressSuccessful(fullList.first { record -> record.id == it.id })
                                }
                            }
                        }
                }
                videoCompressionJob?.join()
                videoCompressionJob = null
            } else {
                Timber.d("Compression queue bigger than setting, show notification to user.")
                showVideoCompressionErrorNotification()
            }
        }
    }

    private fun getTotalVideoSizeInMB(records: List<SyncRecord>) =
        records.sumOf { it.localPath?.let { path -> File(path).length() } ?: 0 } / (1024 * 1024)

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
        showVideoCompressionOutOfSpaceNotification()
        endService(aborted = true)
    }

    /**
     * Update compression progress
     */
    private fun onCompressUpdateProgress(
        progress: Int,
        currentFileIndex: Int,
        totalCount: Int,
    ) {
        showVideoCompressionProgressNotification(progress, currentFileIndex, totalCount)
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
        val srcFile = localPath?.let { File(it) }
        if (srcFile != null && srcFile.exists()) {
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
            localPath?.let {
                deleteSyncRecordByLocalPath(localPath, isSecondary)
            }
        }
    }

    private suspend fun displayUploadProgressNotification() {
        // refresh UI every 1 seconds to avoid too much workload on main thread
        val now = System.currentTimeMillis()
        lastUpdated = if (now - lastUpdated > ON_TRANSFER_UPDATE_REFRESH_MILLIS) {
            now
        } else {
            return
        }

        with(cameraUploadState) {
            Timber.d(
                "Total to upload: $totalToUploadCount " +
                        "Total uploaded: $totalUploadedCount " +
                        "Pending uploads: $totalPendingCount " +
                        "bytes to upload: $totalBytesToUploadCount " +
                        "bytes uploaded: $totalBytesUploadedCount " +
                        "progress: $totalProgress"
            )

            val totalUploadBytes = totalBytesToUploadCount
            val totalUploadedBytes = totalBytesUploadedCount
            val totalUploaded = totalUploadedCount
            val totalToUpload = totalToUploadCount
            val pendingToUpload = totalPendingCount
            val progressPercent = totalProgress

            broadcastProgress(progressPercent, pendingToUpload)
            showUploadProgressNotification(
                totalUploaded,
                totalToUpload,
                totalUploadedBytes,
                totalUploadBytes,
                progressPercent,
                areUploadsPaused,
            )
        }
    }

    /**
     *  Display a notification for upload progress
     */
    private fun showUploadProgressNotification(
        totalUploaded: Int,
        totalToUpload: Int,
        totalUploadedBytes: Long,
        totalUploadBytes: Long,
        progress: Int,
        areUploadsPaused: Boolean,
    ) {
        val content = stringWrapper.getProgressSize(totalUploadedBytes, totalUploadBytes)
        val notification = createNotification(
            title = context.getString(
                if (areUploadsPaused)
                    R.string.upload_service_notification_paused
                else
                    R.string.upload_service_notification,
                totalUploaded,
                totalToUpload
            ),
            content = content,
            subText = content,
            intent = defaultPendingIntent,
            isAutoCancel = false,
            isOngoing = true,
            progress = progress,
        )
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     *  Display a notification for video compression progress
     */
    private fun showVideoCompressionProgressNotification(
        progress: Int,
        currentFileIndex: Int,
        totalCount: Int
    ) {
        val content = context.getString(
            R.string.title_compress_video,
            currentFileIndex,
            totalCount
        )
        val notification = createNotification(
            title = context.getString(R.string.message_compress_video, "$progress%"),
            content = content,
            subText = content,
            intent = defaultPendingIntent,
            isAutoCancel = false,
            isOngoing = true,
            progress = progress,
        )
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     *  Display a notification for checking files to upload
     */
    private fun showCheckUploadsNotification() {
        val notification = createNotification(
            title = context.getString(R.string.section_photo_sync),
            content = context.getString(R.string.settings_camera_notif_checking_title),
            intent = defaultPendingIntent,
            isAutoCancel = false,
            isOngoing = true,
        )
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     *  Display a notification in case the cloud storage does not have enough space
     */
    private fun showStorageOverQuotaNotification() {
        val notification = createNotification(
            title = context.getString(R.string.overquota_alert_title),
            content = context.getString(R.string.download_show_info),
            intent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, ManagerActivity::class.java).apply {
                    action = Constants.ACTION_OVERQUOTA_STORAGE
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            ),
        )
        notificationManager.notify(OVER_STORAGE_QUOTA_NOTIFICATION_ID, notification)
    }

    /**
     *  Display a notification in case the device does not have enough local storage
     *  for video compression
     */
    private fun showVideoCompressionOutOfSpaceNotification() {
        val notification = createNotification(
            title = context.getString(R.string.title_out_of_space),
            content = context.getString(R.string.message_out_of_space),
            intent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, ManagerActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            ),
        )
        notificationManager.notify(NOT_ENOUGH_STORAGE_NOTIFICATION_ID, notification)
    }

    /**
     *  Display a notification in case the device does not have enough local storage
     *  for creating temporary files
     */
    private fun showNotEnoughStorageNotification() {
        val notification = createNotification(
            title = context.getString(R.string.title_out_of_space),
            content = context.getString(R.string.error_not_enough_free_space),
            intent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, ManagerActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            ),
        )
        notificationManager.notify(NOT_ENOUGH_STORAGE_NOTIFICATION_ID, notification)
    }

    /**
     *  Display a notification in case an error happened during video compression
     */
    private suspend fun showVideoCompressionErrorNotification() {
        val notification = createNotification(
            title = context.getString(R.string.title_compression_size_over_limit),
            content = context.getString(
                R.string.message_compression_size_over_limit,
                context.getString(
                    R.string.label_file_size_mega_byte,
                    getVideoCompressionSizeLimitUseCase().toString()
                )
            ),
            intent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, ManagerActivity::class.java).apply {
                    action = Constants.ACTION_SHOW_SETTINGS
                },
                PendingIntent.FLAG_IMMUTABLE
            ),
        )
        notificationManager.notify(COMPRESSION_ERROR_NOTIFICATION_ID, notification)
    }

    /**
     * When Camera Uploads cannot launch due to the Folder being unavailable, display a Notification
     * to inform the User
     *
     * @param cameraUploadsFolderType
     */
    private fun showFolderUnavailableNotification(cameraUploadsFolderType: CameraUploadFolderType) {
        val (resId, notificationId) = when (cameraUploadsFolderType) {
            CameraUploadFolderType.Primary ->
                Pair(
                    R.string.camera_notif_primary_local_unavailable,
                    PRIMARY_FOLDER_UNAVAILABLE_NOTIFICATION_ID
                )

            CameraUploadFolderType.Secondary ->
                Pair(
                    R.string.camera_notif_secondary_local_unavailable,
                    SECONDARY_FOLDER_UNAVAILABLE_NOTIFICATION_ID
                )
        }

        val isShown = notificationManager.activeNotifications.any { it.id == notificationId }
        if (!isShown) {
            val notification = createNotification(
                title = context.getString(R.string.section_photo_sync),
                content = context.getString(resId),
                intent = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, ManagerActivity::class.java).apply {
                        action = Constants.ACTION_SHOW_SETTINGS
                    },
                    PendingIntent.FLAG_IMMUTABLE
                ),
            )
            notificationManager.notify(notificationId, notification)
        }
    }

    /**
     *  Create the [ForegroundInfo] used for the worker
     */
    private fun createCameraUploadsForegroundInfo(): ForegroundInfo {
        val notification = createNotification(
            title = context.getString(R.string.section_photo_sync),
            content = context.getString(R.string.settings_camera_notif_initializing_title),
            intent = null,
            isAutoCancel = false,
            isOngoing = true,
        )
        return createForegroundInfo(notification)
    }

    /**
     *  Create a [Notification]
     */
    private fun createNotification(
        title: String,
        content: String,
        subText: String? = null,
        intent: PendingIntent? = null,
        isOngoing: Boolean = false,
        progress: Int? = null,
        isAutoCancel: Boolean = true,
    ): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setShowBadge(false)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_stat_camera_sync)
            setOngoing(isOngoing)
            setContentTitle(title)
            setStyle(NotificationCompat.BigTextStyle().bigText(content))
            setContentText(content)
            setOnlyAlertOnce(true)
            setAutoCancel(isAutoCancel)
            intent?.let { setContentIntent(intent) }
            progress?.let { setProgress(100, progress, false) }
            subText?.let { setSubText(subText) }
        }
        return builder.build()
    }

    /**
     * Create a [ForegroundInfo] based on [Notification]
     */
    private fun createForegroundInfo(notification: Notification) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification
            )
        }

    /**
     * Dismiss error notifications
     */
    private fun cancelNotifications() {
        with(notificationManager) {
            cancel(PRIMARY_FOLDER_UNAVAILABLE_NOTIFICATION_ID)
            cancel(SECONDARY_FOLDER_UNAVAILABLE_NOTIFICATION_ID)
            cancel(COMPRESSION_ERROR_NOTIFICATION_ID)
            cancel(NOT_ENOUGH_STORAGE_NOTIFICATION_ID)
            cancel(OVER_STORAGE_QUOTA_NOTIFICATION_ID)
        }
    }

    /**
     * Dismiss progress notification
     */
    private fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
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
        updateCameraUploadsBackupHeartbeatStatusUseCase(
            heartbeatStatus = heartbeatStatus,
            cameraUploadsState = cameraUploadState,
        )
    }
}
