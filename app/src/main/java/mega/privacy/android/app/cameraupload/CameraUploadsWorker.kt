package mega.privacy.android.app.cameraupload

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.StatFs
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
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.AndroidCompletedTransfer
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.domain.usecase.CancelAllUploadTransfers
import mega.privacy.android.app.domain.usecase.CancelTransfer
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.IsLocalSecondaryFolderSet
import mega.privacy.android.app.domain.usecase.ProcessMediaForUpload
import mega.privacy.android.app.domain.usecase.SetOriginalFingerprint
import mega.privacy.android.app.domain.usecase.StartUpload
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.transfers.model.mapper.CompletedTransferMapper
import mega.privacy.android.app.receivers.CameraServiceIpChangeHandler
import mega.privacy.android.app.receivers.CameraServiceWakeLockHandler
import mega.privacy.android.app.receivers.CameraServiceWifiLockHandler
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.isActive
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.onUploadSuccess
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.reportUploadFinish
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.reportUploadInterrupted
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.sendPrimaryFolderHeartbeat
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.sendSecondaryFolderHeartbeat
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.startActiveHeartbeat
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.stopActiveHeartbeat
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.updatePrimaryFolderBackupState
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.updateSecondaryFolderBackupState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.ImageProcessor
import mega.privacy.android.app.utils.PreviewUtils
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.gateway.PermissionGateway
import mega.privacy.android.data.mapper.camerauploads.SyncRecordTypeIntMapper
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.camerauploads.HeartbeatStatus
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.BroadcastCameraUploadProgress
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.CompressVideos
import mega.privacy.android.domain.usecase.CompressedVideoPending
import mega.privacy.android.domain.usecase.CreateCameraUploadFolder
import mega.privacy.android.domain.usecase.CreateCameraUploadTemporaryRootDirectory
import mega.privacy.android.domain.usecase.CreateTempFileAndRemoveCoordinatesUseCase
import mega.privacy.android.domain.usecase.DeleteSyncRecord
import mega.privacy.android.domain.usecase.DeleteSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.DisableCameraUploadsInDatabase
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.GetPendingSyncRecords
import mega.privacy.android.domain.usecase.GetSyncRecordByPath
import mega.privacy.android.domain.usecase.GetVideoSyncRecordsByStatus
import mega.privacy.android.domain.usecase.IsCameraUploadSyncEnabled
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.domain.usecase.IsNodeInRubbishOrDeleted
import mega.privacy.android.domain.usecase.IsNotEnoughQuota
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.IsWifiNotSatisfiedUseCase
import mega.privacy.android.domain.usecase.MonitorBatteryInfo
import mega.privacy.android.domain.usecase.MonitorCameraUploadPauseState
import mega.privacy.android.domain.usecase.MonitorChargingStoppedState
import mega.privacy.android.domain.usecase.ResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.ResetTotalUploads
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
import mega.privacy.android.domain.usecase.SetSyncRecordPendingByPath
import mega.privacy.android.domain.usecase.SetupPrimaryFolder
import mega.privacy.android.domain.usecase.SetupSecondaryFolder
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.DeleteCameraUploadsTemporaryRootDirectoryUseCase
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetDefaultNodeHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.HasPreferencesUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsPrimaryFolderPathValidUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCoordinatesUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetSecondaryFolderLocalPathUseCase
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.GetTypedChildrenNodeUseCase
import mega.privacy.android.domain.usecase.transfer.AddCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfer.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfer.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.workers.ScheduleCameraUploadUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import java.io.File
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt


/**
 * Worker to run Camera Uploads
 */
@HiltWorker
class CameraUploadsWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val permissionsGateway: PermissionGateway,
    private val isNotEnoughQuota: IsNotEnoughQuota,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val FOLDER_REMINDER_PRIMARY = 1908
        private const val LOCAL_FOLDER_REMINDER_SECONDARY = 1909
        private const val OVER_QUOTA_NOTIFICATION_CHANNEL_ID = "OVER_QUOTA_NOTIFICATION"
        private const val LOW_BATTERY_LEVEL = 20
        private const val notificationId = Constants.NOTIFICATION_CAMERA_UPLOADS
        private const val notificationChannelId = Constants.NOTIFICATION_CHANNEL_CAMERA_UPLOADS_ID
        private const val notificationChannelName =
            Constants.NOTIFICATION_CHANNEL_CAMERA_UPLOADS_NAME
    }

    /**
     * The [MegaApiAndroid] for SDK calls, which will be removed once all direct calls have
     * been converted to Use Cases
     */
    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    /**
     * [GetPrimaryFolderPathUseCase]
     */
    @Inject
    lateinit var getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase

    /**
     * [IsPrimaryFolderPathValidUseCase]
     */
    @Inject
    lateinit var isPrimaryFolderPathValidUseCase: IsPrimaryFolderPathValidUseCase

    /**
     * IsLocalSecondaryFolderSet
     */
    @Inject
    lateinit var isLocalSecondaryFolderSet: IsLocalSecondaryFolderSet

    /**
     * IsSecondaryFolderEnabled
     */
    @Inject
    lateinit var isSecondaryFolderEnabled: IsSecondaryFolderEnabled

    /**
     * HasPreferencesUseCase
     */
    @Inject
    lateinit var hasPreferencesUseCase: HasPreferencesUseCase

    /**
     * IsCameraUploadSyncEnabled
     */
    @Inject
    lateinit var isCameraUploadSyncEnabled: IsCameraUploadSyncEnabled

    /**
     * IsWifiNotSatisfied Use Case
     */
    @Inject
    lateinit var isWifiNotSatisfiedUseCase: IsWifiNotSatisfiedUseCase

    /**
     * DeleteSyncRecord
     */
    @Inject
    lateinit var deleteSyncRecord: DeleteSyncRecord

    /**
     * DeleteSyncRecordByLocalPath
     */
    @Inject
    lateinit var deleteSyncRecordByLocalPath: DeleteSyncRecordByLocalPath

    /**
     * DeleteSyncRecordByFingerprint
     */
    @Inject
    lateinit var deleteSyncRecordByFingerprint: DeleteSyncRecordByFingerprint

    /**
     * SetPrimaryFolderLocalPathUseCase
     */
    @Inject
    lateinit var setPrimaryFolderLocalPathUseCase: SetPrimaryFolderLocalPathUseCase

    /**
     * ShouldCompressVideo
     */
    @Inject
    lateinit var shouldCompressVideo: ShouldCompressVideo

    /**
     * SetSecondaryFolderLocalPathUseCase
     */
    @Inject
    lateinit var setSecondaryFolderLocalPathUseCase: SetSecondaryFolderLocalPathUseCase

    /**
     * ClearSyncRecords
     */
    @Inject
    lateinit var clearSyncRecords: ClearSyncRecords

    /**
     * Are Location Tags Enabled
     */
    @Inject
    lateinit var areLocationTagsEnabledUseCase: AreLocationTagsEnabledUseCase

    /**
     * GetSyncRecordByPath
     */
    @Inject
    lateinit var getSyncRecordByPath: GetSyncRecordByPath

    /**
     * GetPendingSyncRecords
     */
    @Inject
    lateinit var getPendingSyncRecords: GetPendingSyncRecords

    /**
     * CompressedVideoPending
     */
    @Inject
    lateinit var compressedVideoPending: CompressedVideoPending

    /**
     * GetVideoSyncRecordsByStatus
     */
    @Inject
    lateinit var getVideoSyncRecordsByStatus: GetVideoSyncRecordsByStatus

    /**
     * SetSyncRecordPendingByPath
     */
    @Inject
    lateinit var setSyncRecordPendingByPath: SetSyncRecordPendingByPath

    /**
     * Get Video Compression Size Limit
     */
    @Inject
    lateinit var getVideoCompressionSizeLimitUseCase: GetVideoCompressionSizeLimitUseCase

    /**
     * IsChargingRequired
     */
    @Inject
    lateinit var isChargingRequired: IsChargingRequired

    /**
     * GetNodeByHandle
     */
    @Inject
    lateinit var getNodeByHandle: GetNodeByHandle

    /**
     * GetChildrenNode
     */
    @Inject
    lateinit var getTypedChildrenNodeUseCase: GetTypedChildrenNodeUseCase

    /**
     * ProcessMediaForUpload
     */
    @Inject
    lateinit var processMediaForUpload: ProcessMediaForUpload

    /**
     * GetPrimarySyncHandle
     */
    @Inject
    lateinit var getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase

    /**
     * GetSecondarySyncHandle
     */
    @Inject
    lateinit var getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase

    /**
     * SetPrimarySyncHandle
     */
    @Inject
    lateinit var setPrimarySyncHandle: SetPrimarySyncHandle

    /**
     * SetSecondarySyncHandle
     */
    @Inject
    lateinit var setSecondarySyncHandle: SetSecondarySyncHandle

    /**
     * GetDefaultNodeHandle
     */
    @Inject
    lateinit var getDefaultNodeHandleUseCase: GetDefaultNodeHandleUseCase

    /**
     * AreTransfersPausedUseCase
     */
    @Inject
    lateinit var areTransfersPausedUseCase: AreTransfersPausedUseCase

    /**
     * Sync Record Type Mapper
     */
    @Inject
    lateinit var syncRecordTypeIntMapper: SyncRecordTypeIntMapper

    /**
     * IO dispatcher for camera upload work
     */
    @IoDispatcher
    @Inject
    lateinit var ioDispatcher: CoroutineDispatcher

    /**
     * Monitor camera upload pause state
     */
    @Inject
    lateinit var monitorCameraUploadPauseState: MonitorCameraUploadPauseState

    /**
     * Monitor connectivity
     */
    @Inject
    lateinit var monitorConnectivityUseCase: MonitorConnectivityUseCase

    /**
     * Monitor battery level
     */
    @Inject
    lateinit var monitorBatteryInfo: MonitorBatteryInfo

    /**
     * Background Fast Login
     */
    @Inject
    lateinit var backgroundFastLoginUseCase: BackgroundFastLoginUseCase

    /**
     * Is Node In Rubbish or deleted
     */
    @Inject
    lateinit var isNodeInRubbishOrDeleted: IsNodeInRubbishOrDeleted

    /**
     * Monitor charging stop status
     */
    @Inject
    lateinit var monitorChargingStoppedState: MonitorChargingStoppedState

    /**
     * initiate mega api connection based on IP
     */
    @Inject
    lateinit var cameraServiceIpChangeHandler: CameraServiceIpChangeHandler

    /**
     * Cancel Transfer
     */
    @Inject
    lateinit var cancelTransferByTagUseCase: CancelTransferByTagUseCase

    /**
     * Cancel All Upload Transfers
     */
    @Inject
    lateinit var cancelAllUploadTransfers: CancelAllUploadTransfers

    /**
     * Copy Node
     */
    @Inject
    lateinit var copyNodeUseCase: CopyNodeUseCase

    /**
     * Set Original Fingerprint
     */
    @Inject
    lateinit var setOriginalFingerprint: SetOriginalFingerprint

    /**
     * Start Upload
     */
    @Inject
    lateinit var startUpload: StartUpload

    /**
     * Create Camera Upload Folder
     */
    @Inject
    lateinit var createCameraUploadFolder: CreateCameraUploadFolder

    /**
     * Setup Primary Folder
     */
    @Inject
    lateinit var setupPrimaryFolder: SetupPrimaryFolder

    /**
     * Setup Secondary Folder
     */
    @Inject
    lateinit var setupSecondaryFolder: SetupSecondaryFolder

    /**
     * Establish Camera Uploads Sync Handles
     */
    @Inject
    lateinit var establishCameraUploadsSyncHandlesUseCase: EstablishCameraUploadsSyncHandlesUseCase

    /**
     * Reset Total Uploads
     */
    @Inject
    lateinit var resetTotalUploads: ResetTotalUploads

    /**
     * Camera Service Wake Lock Handler
     */
    @Inject
    lateinit var cameraServiceWakeLockHandler: CameraServiceWakeLockHandler

    /**
     * Camera Service Wifi Lock Handler
     */
    @Inject
    lateinit var cameraServiceWifiLockHandler: CameraServiceWifiLockHandler

    /**
     * Disable Camera Uploads in Database
     */
    @Inject
    lateinit var disableCameraUploadsInDatabase: DisableCameraUploadsInDatabase

    /**
     * Compress Videos
     */
    @Inject
    lateinit var compressVideos: CompressVideos

    /**
     * Reset Media Upload Time Stamps
     */
    @Inject
    lateinit var resetMediaUploadTimeStamps: ResetMediaUploadTimeStamps

    /**
     * Disable Media Upload Settings
     */
    @Inject
    lateinit var disableMediaUploadSettings: DisableMediaUploadSettings

    /**
     * Create camera upload temporary root directory
     */
    @Inject
    lateinit var createCameraUploadTemporaryRootDirectory: CreateCameraUploadTemporaryRootDirectory

    /**
     * Delete Camera Uploads Temporary Root Directory
     */
    @Inject
    lateinit var deleteCameraUploadsTemporaryRootDirectoryUseCase: DeleteCameraUploadsTemporaryRootDirectoryUseCase

    /**
     * Broadcast camera upload progress
     */
    @Inject
    lateinit var broadcastCameraUploadProgress: BroadcastCameraUploadProgress

    /**
     * Schedule Camera Upload
     */
    @Inject
    lateinit var scheduleCameraUploadUseCase: ScheduleCameraUploadUseCase

    /**
     * Create temporary file and remove coordinates
     */
    @Inject
    lateinit var createTempFileAndRemoveCoordinatesUseCase: CreateTempFileAndRemoveCoordinatesUseCase

    /**
     * Add completed transfer to local storage
     */
    @Inject
    lateinit var addCompletedTransferUseCase: AddCompletedTransferUseCase

    /**
     * Temporary mapper that convert a CompletedTransfer to legacy model [AndroidCompletedTransfer]
     * Should be removed once [AndroidCompletedTransfer] removed from codebase
     */
    @Inject
    lateinit var completedTransferMapper: CompletedTransferMapper

    /**
     * Set coordinates for image files
     */
    @Inject
    lateinit var setCoordinatesUseCase: SetCoordinatesUseCase

    /**
     * True if the camera uploads attributes have already been requested
     * from the server
     */
    private var missingAttributesChecked = false

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
     * Default pending intent used to redirect user when clicking on a notification
     */
    private var defaultPendingIntent: PendingIntent? = null

    /**
     * Temp root path used for generating temporary files in the CU process
     * This folder is created at the beginning of the CU process
     * and deleted at the end of the process
     */
    private lateinit var tempRoot: String

    /**
     * Count of total files uploaded
     */
    private var totalUploaded = 0

    /**
     * Count of total files to upload
     */
    private var totalToUpload = 0

    /**
     * Time in milliseconds to flag the last update of the notification
     * Used to prevent updating the notification too often
     */
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

    override suspend fun doWork() = coroutineScope {
        Timber.d("Start CU Worker")
        try {
            scope = this

            createDefaultNotificationPendingIntent()
            setForegroundAsync(getForegroundInfo())

            val isNotEnoughQuota = isNotEnoughQuota()
            val hasMediaPermissions =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsGateway.hasPermissions(
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO,
                    )
                } else {
                    permissionsGateway.hasPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                    )
                }
            Timber.d("isNotEnoughQuota: $isNotEnoughQuota, hasMediaPermissions: $hasMediaPermissions")
            if (!isNotEnoughQuota && hasMediaPermissions) {
                Timber.d("No active process, start service")
                withContext(ioDispatcher) {
                    initService()
                    performCompleteFastLogin()
                }
                Result.success()
            } else {
                Timber.d("Finished CU with Failure")
                Result.failure()
            }
        } catch (throwable: Throwable) {
            Timber.e(throwable)
            endService(aborted = true)
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = createNotification(
            title = context.getString(R.string.section_photo_sync),
            content = context.getString(R.string.settings_camera_notif_initializing_title),
            intent = null,
            isAutoCancel = false
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                notificationId,
                notification
            )
        }
    }

    private fun monitorUploadPauseStatus() {
        monitorUploadPauseStatusJob = scope?.launch(ioDispatcher) {
            monitorCameraUploadPauseState().collect {
                updateProgressNotification()
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


    /**
     * Cancels a pending [MegaTransfer] through [CancelTransfer],
     * and call [ResetTotalUploads] after every cancellation to reset the total uploads if
     * there are no more pending uploads
     *
     * @param transfer the [MegaTransfer] to be cancelled
     */
    private suspend fun cancelPendingTransfer(transfer: MegaTransfer) {
        runCatching { cancelTransferByTagUseCase(transfer.tag) }
            .onSuccess {
                Timber.d("Transfer cancellation successful")
                resetTotalUploads()
            }
            .onFailure { error -> Timber.e("Transfer cancellation error: $error") }
    }

    /**
     * Cancels all pending [MegaTransfer] items through [CancelAllUploadTransfers],
     * and call [ResetTotalUploads] afterwards
     */
    private suspend fun cancelAllPendingTransfers() {
        runCatching { cancelAllUploadTransfers() }
            .onSuccess {
                Timber.d("Cancel all transfers successful")
                resetTotalUploads()
            }
            .onFailure { error -> Timber.e("Cancel all transfers error: $error") }

    }

    /**
     * Function that starts the Camera Uploads functionality
     */
    private suspend fun startWorker() {
        runCatching {
            val state = canRunCameraUploads()
            if (state == StartCameraUploadsState.CAN_RUN_CAMERA_UPLOADS) {
                Timber.d("Calling startWorker() successful. Starting Camera Uploads")
                hideFolderPathNotifications()
                startCameraUploads()
            } else {
                Timber.w("Calling startWorker() failed. Proceed to handle error")
                handleFailedStartCameraUploadsState(state)
            }
        }.onFailure { exception ->
            Timber.e("Calling startWorker() failed with exception $exception")
            endService(aborted = true)
        }
    }

    /**
     * Instructs [notificationManager] to hide the Primary and/or Secondary Folder
     * notifications if they exist
     */
    private suspend fun hideFolderPathNotifications() {
        if (isPrimaryFolderValid()) notificationManager.cancel(FOLDER_REMINDER_PRIMARY)
        if (hasLocalSecondaryFolder()) notificationManager.cancel(LOCAL_FOLDER_REMINDER_SECONDARY)
    }

    /**
     * Checks if Camera Uploads can run by evaluating the following conditions in order:
     *
     * 1. The Preferences exist - [preferencesExist],
     * 2. The Camera Uploads sync is enabled - [cameraUploadsSyncEnabled],
     * 3. The Device battery level is above the minimum threshold - [deviceAboveMinimumBatteryLevel],
     * 4. The Wi-Fi Constraint is satisfied - [isWifiConstraintSatisfied],
     * 5. The Primary Folder exists and is valid - [isPrimaryFolderValid],
     * 6. The local Secondary Folder exists - [hasLocalSecondaryFolder],
     * 7. The user Camera Uploads attribute exists - [missingAttributesChecked],
     * 8. The Primary Folder exists - [areFoldersEstablished],
     * 9. The Secondary Folder exists if Enable Secondary Media Uploads is enabled - [areFoldersEstablished]
     *
     * If all conditions are met, [StartCameraUploadsState.CAN_RUN_CAMERA_UPLOADS] is returned.
     * Otherwise, a specific [StartCameraUploadsState] is returned depending on what condition has failed
     *
     * @return A specific [StartCameraUploadsState]
     */
    private suspend fun canRunCameraUploads(): StartCameraUploadsState =
        when {
            !preferencesExist() -> StartCameraUploadsState.MISSING_PREFERENCES
            !cameraUploadsSyncEnabled() -> StartCameraUploadsState.DISABLED_SYNC
            !isWifiConstraintSatisfied() -> StartCameraUploadsState.UNSATISFIED_WIFI_CONSTRAINT
            !deviceAboveMinimumBatteryLevel -> StartCameraUploadsState.BELOW_DEVICE_BATTERY_LEVEL
            !isPrimaryFolderValid() -> StartCameraUploadsState.INVALID_PRIMARY_FOLDER
            !hasLocalSecondaryFolder() -> StartCameraUploadsState.MISSING_LOCAL_SECONDARY_FOLDER
            !missingAttributesChecked -> StartCameraUploadsState.MISSING_USER_ATTRIBUTE
            !areFoldersEstablished() -> StartCameraUploadsState.UNESTABLISHED_FOLDERS
            else -> StartCameraUploadsState.CAN_RUN_CAMERA_UPLOADS
        }

    /**
     * When Camera Uploads cannot be enabled, the function executes specific actions depending
     * on the [StartCameraUploadsState] that was passed
     *
     * @param state The failing [StartCameraUploadsState]
     */
    private suspend fun handleFailedStartCameraUploadsState(state: StartCameraUploadsState) {
        Timber.w("Start Camera Uploads Error state: $state")
        when (state) {
            StartCameraUploadsState.MISSING_PREFERENCES,
            StartCameraUploadsState.DISABLED_SYNC,
            StartCameraUploadsState.BELOW_DEVICE_BATTERY_LEVEL,
            StartCameraUploadsState.UNSATISFIED_WIFI_CONSTRAINT,
            -> {
                Timber.e("Stop Camera Uploads due to $state")
                endService(aborted = true)
            }

            StartCameraUploadsState.INVALID_PRIMARY_FOLDER -> {
                Timber.e("Primary Folder is invalid. Stop Camera Uploads")
                handlePrimaryFolderDisabled()
                endService(aborted = true)
            }

            StartCameraUploadsState.MISSING_LOCAL_SECONDARY_FOLDER -> {
                Timber.e("Local Secondary Folder is disabled. Stop Camera Uploads")
                handleLocalSecondaryFolderDisabled()
                endService(aborted = true)
            }

            StartCameraUploadsState.MISSING_USER_ATTRIBUTE -> {
                Timber.w("Handle the missing Camera Uploads user attribute")
                runCatching {
                    establishCameraUploadsSyncHandlesUseCase()
                    missingAttributesChecked = true
                }
                    .onSuccess { startWorker() }
                    .onFailure { endService() }
            }

            StartCameraUploadsState.UNESTABLISHED_FOLDERS -> {
                Timber.w("Primary and/or Secondary Folders do not exist. Establish the folders")
                runCatching { establishFolders() }
                    .onSuccess { startWorker() }
                    .onFailure { endService(aborted = true) }
            }

            else -> Unit
        }
    }

    /**
     * Checks if the Preferences from [hasPreferencesUseCase] exist
     *
     * @return true if it exists, and false if otherwise
     */
    private suspend fun preferencesExist(): Boolean =
        hasPreferencesUseCase().also {
            if (!it) Timber.w("Preferences not defined, so not enabled")
        }

    /**
     * Checks if the Camera Uploads sync from [isCameraUploadSyncEnabled] is enabled
     *
     * @return true if enabled, and false if otherwise
     */
    private suspend fun cameraUploadsSyncEnabled(): Boolean =
        isCameraUploadSyncEnabled().also {
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
            if (!it) Timber.w("The Primary Folder does not exist or is invalid")
        }

    /**
     * Checks if the local Secondary Folder from [isLocalSecondaryFolderSet] exists
     *
     * @return true if it exists, and false if otherwise
     */
    private suspend fun hasLocalSecondaryFolder(): Boolean =
        isLocalSecondaryFolderSet().also {
            if (!it) Timber.w("Local Secondary Folder is not set")
        }

    /**
     * Checks whether both Primary and Secondary Folders are established
     *
     * @return true if the Primary Folder exists and the Secondary Folder option disabled, or
     * if the Primary Folder exists and the Secondary Folder option enabled with the folder also existing.
     *
     * false if both conditions are not met
     */
    private suspend fun areFoldersEstablished(): Boolean =
        (isPrimaryFolderEstablished() && !isSecondaryFolderEnabled()) ||
                (isPrimaryFolderEstablished() && (isSecondaryFolderEnabled() && isSecondaryFolderEstablished()))

    /**
     * Checks whether the Primary Folder is established
     *
     * @return true if the Primary Folder handle is a valid handle, and false if otherwise
     */
    private suspend fun isPrimaryFolderEstablished(): Boolean {
        val primarySyncHandle = getPrimarySyncHandleUseCase()
        if (primarySyncHandle == MegaApiJava.INVALID_HANDLE) {
            return false
        }
        val isPrimaryFolderInRubbish = isNodeInRubbishOrDeleted(primarySyncHandle)
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
        val isSecondaryFolderInRubbish = isNodeInRubbishOrDeleted(getSecondaryFolderHandle())
        val result =
            !isSecondaryFolderInRubbish || (getSecondaryFolderHandle() != MegaApiJava.INVALID_HANDLE)
        Timber.d("Secondary Folder Established $result")
        return result
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

    private suspend fun startCameraUploads() {
        showNotification(
            context.getString(R.string.section_photo_sync),
            context.getString(R.string.settings_camera_notif_checking_title),
            defaultPendingIntent,
            false
        )
        checkUploadNodes()
    }

    private suspend fun checkUploadNodes() {
        Timber.d("Get Pending Files from Media Store Database")
        val primaryUploadNode = getNodeByHandle(getPrimarySyncHandleUseCase())
        if (primaryUploadNode == null) {
            Timber.d("ERROR: Primary Parent Folder is NULL")
            endService(aborted = true)
            return
        }
        val secondaryUploadNode = if (isSecondaryFolderEnabled()) {
            Timber.d("Secondary Upload is ENABLED")
            getNodeByHandle(getSecondarySyncHandleUseCase())
        } else {
            null
        }
        totalUploaded = 0
        processMediaForUpload(primaryUploadNode, secondaryUploadNode, tempRoot)
        gatherSyncRecordsForUpload()
    }

    private suspend fun gatherSyncRecordsForUpload() {
        val finalList = getPendingSyncRecords()
        if (finalList.isEmpty()) {
            if (compressedVideoPending()) {
                Timber.d("Pending upload list is empty, now check view compression status.")
                startVideoCompression()
            } else {
                Timber.d("Nothing to upload.")
                endService()
                deleteCameraUploadsTemporaryRootDirectoryUseCase()
            }
        } else {
            Timber.d("Start to upload %d files.", finalList.size)
            startParallelUpload(finalList, false)
        }
    }

    private fun showNotEnoughStorageNotification() {
        val title = context.getString(R.string.title_out_of_space)
        val message = context.getString(R.string.error_not_enough_free_space)
        val intent = Intent(context, ManagerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        showNotification(title, message, pendingIntent, true)
    }

    private suspend fun startParallelUpload(
        finalList: List<SyncRecord>,
        isCompressedVideo: Boolean,
    ) = coroutineScope {
        val copyFileAsyncList = mutableListOf<Job>()
        val uploadFileAsyncList = mutableListOf<Job>()
        // If the Service detects that all upload transfers are paused when turning on
        // Camera Uploads, update the Primary and Secondary Folder Backup States to
        // BackupState.PAUSE_UPLOADS
        if (areTransfersPausedUseCase()) {
            Timber.d("All Pending Uploads Paused. Send Backup State = ${BackupState.PAUSE_UPLOADS}")
            updatePrimaryFolderBackupState(BackupState.PAUSE_UPLOADS)
            updateSecondaryFolderBackupState(BackupState.PAUSE_UPLOADS)
        }
        val primaryUploadNode = getNodeByHandle(getPrimarySyncHandleUseCase())
        val secondaryUploadNode = getNodeByHandle(getSecondarySyncHandleUseCase())

        startActiveHeartbeat(finalList)
        for (file in finalList) {
            val isSecondary = file.isSecondary
            val parent = (if (isSecondary) secondaryUploadNode else primaryUploadNode) ?: continue
            if (file.type == SyncRecordType.TYPE_PHOTO && !file.isCopyOnly) {
                if (!areLocationTagsEnabledUseCase()) {
                    var shouldContinue = false
                    flowOf(
                        createTempFileAndRemoveCoordinatesUseCase(
                            tempRoot,
                            file
                        )
                    ).retryWhen { cause, attempt ->
                        if (cause is NotEnoughStorageException) {
                            if (attempt >= 60) {
                                @Suppress("DEPRECATION")
                                if (megaApi.numPendingUploads == 0) {
                                    showNotEnoughStorageNotification()
                                    Timber.w("Stop service due to out of space issue")
                                    endService(aborted = true)
                                } else {
                                    // we will not be retying again and skip the current record
                                    Timber.d("Stop retrying for $file")
                                    shouldContinue = true
                                    return@retryWhen false
                                }
                            }
                            Timber.d("Waiting for disk space to process for $file")
                            // total delay (1 second times 60 attempts) = 60 seconds
                            delay(TimeUnit.SECONDS.toMillis(1))
                            return@retryWhen true
                        } else {
                            // not storage exception, no need to retry
                            return@retryWhen false
                        }
                    }.catch {
                        Timber.e("Temporary File creation exception$it")
                        shouldContinue = true
                    }.collect()
                    if (shouldContinue) continue
                } else {
                    // Set as don't remove GPS
                    file.newPath = file.localPath
                }
            }

            var path: String?
            if (isCompressedVideo || file.type == SyncRecordType.TYPE_PHOTO
                || file.type == SyncRecordType.TYPE_VIDEO && shouldCompressVideo()
            ) {
                path = file.newPath
                val temp = path?.let { File(it) }
                if ((temp != null) && !temp.exists()) {
                    path = file.localPath
                }
            } else {
                path = file.localPath
            }

            if (file.isCopyOnly) {
                Timber.d("Copy from node, file timestamp is: %s", file.timestamp)
                file.nodeHandle?.let { nodeHandle ->
                    getNodeByHandle(nodeHandle)?.let { nodeToCopy ->
                        totalToUpload++
                        copyFileAsyncList.add(async {
                            handleCopyNode(
                                nodeToCopy = nodeToCopy,
                                newNodeParent = parent,
                                newNodeName = file.fileName.orEmpty(),
                            )
                        })
                    }
                }
            } else {
                val toUpload = path?.let { File(it) }
                if ((toUpload != null) && toUpload.exists()) {
                    // compare size
                    val node = checkExistBySize(parent, toUpload.length())
                    if (node != null && node.fingerprint == null) {
                        Timber.d(
                            "Node with handle: %d already exists, delete record from database.",
                            node.id.longValue
                        )
                        path?.let {
                            deleteSyncRecord(it, isSecondary)
                        }
                    } else {
                        totalToUpload++
                        val lastModified = getLastModifiedTime(file)

                        // If the local file path exists, call the Use Case to upload the file
                        path?.let { nonNullFilePath ->
                            uploadFileAsyncList.add(async {
                                startUpload(
                                    localPath = nonNullFilePath,
                                    parentNode = parent,
                                    fileName = file.fileName,
                                    modificationTime = lastModified / 1000,
                                    appData = Constants.APP_DATA_CU,
                                    isSourceTemporary = false,
                                    shouldStartFirst = false,
                                    cancelToken = null,
                                ).collect { globalTransfer ->
                                    // Handle the GlobalTransfer emitted by the Use Case
                                    onGlobalTransferUpdated(globalTransfer)
                                }
                            })
                        }
                    }
                } else {
                    Timber.d("Local file is unavailable, delete record from database.")
                    path?.let {
                        deleteSyncRecord(it, isSecondary)
                    }
                }
            }
        }
        if (copyFileAsyncList.isNotEmpty()) {
            updateUpload()
            copyFileAsyncList.joinAll()
        }
        uploadFileAsyncList.joinAll()

        if (compressedVideoPending() && isCompressorAvailable().not()) {
            Timber.d("Got pending videos, will start compress.")
            startVideoCompression()
        } else {
            Timber.d("No pending videos, finish.")
            onQueueComplete()
        }
    }

    /**
     * Handles the [GlobalTransfer] emitted by [StartUpload]
     *
     * @param globalTransfer The [GlobalTransfer] emitted from the Use Case
     */
    private suspend fun onGlobalTransferUpdated(globalTransfer: GlobalTransfer) {
        when (globalTransfer) {
            is GlobalTransfer.OnTransferFinish -> onTransferFinished(globalTransfer)
            is GlobalTransfer.OnTransferUpdate -> onTransferUpdated(globalTransfer)
            is GlobalTransfer.OnTransferTemporaryError -> onTransferTemporaryError(globalTransfer)
            // No further action necessary for these Scenarios
            is GlobalTransfer.OnTransferStart,
            is GlobalTransfer.OnTransferData,
            -> Unit
        }
    }

    /**
     * Handle logic for when an upload has finished
     *
     * @param globalTransfer [GlobalTransfer.OnTransferFinish]
     */
    private suspend fun onTransferFinished(globalTransfer: GlobalTransfer.OnTransferFinish) {
        val transfer = globalTransfer.transfer
        val error = globalTransfer.error

        Timber.d(
            "Image Sync Finished, Error Code: ${error.errorCode}, " +
                    "Image Handle: ${transfer.nodeHandle}, " +
                    "Image Size: ${transfer.transferredBytes}"
        )
        try {
            transferFinished(transfer, error)
        } catch (th: Throwable) {
            Timber.e(th)
            th.printStackTrace()
        }
    }

    /**
     * Handle logic for when an upload has been updated
     *
     * @param globalTransfer [GlobalTransfer.OnTransferFinish]
     */
    private suspend fun onTransferUpdated(globalTransfer: GlobalTransfer.OnTransferUpdate) {
        val transfer = globalTransfer.transfer
        runCatching {
            updateProgressNotification()
        }.onFailure {
            Timber.d("Cancelled Transfer Node: ${transfer.nodeHandle}")
            cancelPendingTransfer(transfer)
        }
    }

    /**
     * Handle logic for when a temporary error has occurred during uploading
     *
     * @param globalTransfer [GlobalTransfer.OnTransferTemporaryError]
     */
    private suspend fun onTransferTemporaryError(globalTransfer: GlobalTransfer.OnTransferTemporaryError) {
        val error = globalTransfer.error

        Timber.w("onTransferTemporaryError: ${globalTransfer.transfer.nodeHandle}")
        if (error.errorCode == MegaError.API_EOVERQUOTA) {
            if (error.value != 0L) Timber.w("Transfer Over Quota Error: ${error.errorCode}")
            else Timber.w("Storage Over Quota Error: ${error.errorCode}")
            showStorageOverQuotaNotification()
            endService(aborted = true)
        }
    }

    /**
     * Perform a copy operation through [CopyNodeUseCase]
     *
     * @param nodeToCopy The [MegaNode] to be copied
     * @param newNodeParent the [MegaNode] that [nodeToCopy] will be moved to
     * @param newNodeName the new name for [nodeToCopy] once it is moved to [newNodeParent]
     */
    private suspend fun handleCopyNode(
        nodeToCopy: MegaNode,
        newNodeParent: MegaNode,
        newNodeName: String,
    ) {
        runCatching {
            copyNodeUseCase(
                nodeToCopy = NodeId(nodeToCopy.handle),
                newNodeParent = NodeId(newNodeParent.handle),
                newNodeName = newNodeName,
            )
        }.onSuccess { nodeId ->
            Timber.d("Copy node successful")
            getNodeByHandle(nodeId.longValue)?.let { retrievedNode ->
                val fingerprint = retrievedNode.fingerprint
                val isSecondary = retrievedNode.parentHandle == getSecondarySyncHandleUseCase()
                // Delete the Camera Upload sync record by fingerprint
                deleteSyncRecordByFingerprint(
                    originalPrint = fingerprint,
                    newPrint = fingerprint,
                    isSecondary = isSecondary,
                )
                // Update information when the file is copied to the target node
                onUploadSuccess(
                    node = retrievedNode,
                    isSecondary = isSecondary,
                )
            }
            updateUpload()
        }.onFailure { error ->
            Timber.e("Copy node error: $error")
            updateUpload()
        }

    }

    private suspend fun checkExistBySize(parent: MegaNode, size: Long): FileNode? {
        val nodeList =
            getTypedChildrenNodeUseCase(NodeId(parent.handle), SortOrder.ORDER_ALPHABETICAL_ASC)
        for (node in nodeList) {
            if (node is FileNode && node.size == size) {
                return node
            }
        }
        return null
    }

    private fun getLastModifiedTime(file: SyncRecord): Long {
        val source = file.localPath?.let { File(it) }
        return source?.lastModified() ?: 0
    }

    private suspend fun onQueueComplete() {
        Timber.d("Stopping foreground!")
        resetTotalUploads()
        totalUploaded = 0
        totalToUpload = 0
        reportUploadFinish()
        stopActiveHeartbeat()
        endService()
    }

    /**
     * Executes certain behavior when the Primary Folder is disabled
     */
    private suspend fun handlePrimaryFolderDisabled() {
        displayFolderUnavailableNotification(
            R.string.camera_notif_primary_local_unavailable,
            FOLDER_REMINDER_PRIMARY
        )
        disableCameraUploadsInDatabase()
        setPrimaryFolderLocalPathUseCase(Constants.INVALID_NON_NULL_VALUE)
        setSecondaryFolderLocalPathUseCase(Constants.INVALID_NON_NULL_VALUE)
        // Refresh SettingsCameraUploadsFragment
        context.sendBroadcast(Intent(BroadcastConstants.ACTION_REFRESH_CAMERA_UPLOADS_SETTING))
    }

    /**
     * Executes certain behavior when the Local Secondary Folder is disabled
     */
    private suspend fun handleLocalSecondaryFolderDisabled() {
        displayFolderUnavailableNotification(
            R.string.camera_notif_secondary_local_unavailable,
            LOCAL_FOLDER_REMINDER_SECONDARY
        )
        // Disable Media Uploads only
        resetMediaUploadTimeStamps()
        disableMediaUploadSettings()
        setSecondaryFolderLocalPathUseCase(SettingsConstants.INVALID_PATH)
        context.sendBroadcast(Intent(BroadcastConstants.ACTION_DISABLE_MEDIA_UPLOADS_SETTING))
    }

    /**
     * When the user is not logged in, perform a Complete Fast Login procedure
     */
    private suspend fun performCompleteFastLogin() {
        Timber.d("Waiting for the user to complete the Fast Login procedure")

        // arbitrary retry value
        var retry = 3
        while (MegaApplication.isLoggingIn && retry > 0) {
            Timber.d("Wait for the isLoggingIn lock to be available")
            delay(1000)
            retry--
        }

        // Legacy support: isLoggingIn needs to be set in order to inform other parts of the
        // app that a Login Procedure is occurring
        MegaApplication.isLoggingIn = true
        val result = runCatching { backgroundFastLoginUseCase() }
        MegaApplication.isLoggingIn = false

        if (result.isSuccess) {
            Timber.d("Complete Fast Login procedure successful. Get cookies settings after login")
            MegaApplication.getInstance().checkEnabledCookies()
            Timber.d("Start process")
            startWorker()
        } else {
            Timber.e("Complete Fast Login procedure unsuccessful with error ${result.exceptionOrNull()}. Stop process")
            endService(aborted = true)
        }
    }

    /**
     * When Camera Uploads cannot launch due to the Folder being unavailable, display a Notification
     * to inform the User
     *
     * @param resId  The content text of the notification. Here is the string's res id.
     * @param notificationId Notification id, can cancel the notification by the same id when need.
     */
    private fun displayFolderUnavailableNotification(resId: Int, notificationId: Int) {
        var isShowing = false
        for (notification in notificationManager.activeNotifications) {
            if (notification.id == notificationId) {
                isShowing = true
            }
        }
        if (!isShowing) {
            val notification = createNotification(
                context.getString(R.string.section_photo_sync),
                context.getString(resId),
                null,
                false
            )
            notificationManager.notify(notificationId, notification)
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
        val secondarySyncHandle = getSecondarySyncHandleUseCase()
        if (secondarySyncHandle == MegaApiJava.INVALID_HANDLE || getNodeByHandle(secondarySyncHandle) == null) {
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
            setupPrimaryFolder(it)
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
            setupSecondaryFolder(it)
        } ?: throw Exception("Failed to create secondary upload folder")
    }

    private suspend fun initService() {
        // Start monitoring external events
        startWakeAndWifiLocks()
        monitorConnectivityStatus()
        monitorChargingStoppedStatus()
        monitorBatteryLevelStatus()
        monitorUploadPauseStatus()
        cameraServiceIpChangeHandler.start()

        // Reset properties
        lastUpdated = 0
        totalUploaded = 0
        totalToUpload = 0
        missingAttributesChecked = false

        // Create temp root folder
        runCatching { tempRoot = createCameraUploadTemporaryRootDirectory() }
            .onFailure {
                Timber.w("Root path doesn't exist")
                endService(aborted = true)
            }

        // Clear sync records if needed
        clearSyncRecords()
    }

    /**
     * Create a default notification pending intent
     * that will redirect to the manager activity with a [Constants.ACTION_CANCEL_CAM_SYNC] action
     */
    private fun createDefaultNotificationPendingIntent() {
        val intent = Intent(context, ManagerActivity::class.java).apply {
            action = Constants.ACTION_CANCEL_CAM_SYNC
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.PENDING_TAB)
        }
        defaultPendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }


    /**
     * When [CameraUploadsWorker] is initialized, start the Wake and Wifi Locks
     */
    private fun startWakeAndWifiLocks() {
        cameraServiceWakeLockHandler.startWakeLock()
        cameraServiceWifiLockHandler.startWifiLock()
    }

    /**
     * When [CameraUploadsWorker] ends, Stop both Wake and Wifi Locks
     */
    private fun stopWakeAndWifiLocks() {
        cameraServiceWakeLockHandler.stopWakeLock()
        cameraServiceWifiLockHandler.stopWifiLock()
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

        sendStatusToBackupCenter(aborted = aborted)
        cancelAllPendingTransfers()
        broadcastProgress(100, 0)
        stopActiveHeartbeat()
        stopWakeAndWifiLocks()
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
        }
    }

    /**
     * Send the status of Camera Uploads to back up center
     *
     * @param aborted true if the Camera Uploads has been stopped prematurely
     */
    private fun sendStatusToBackupCenter(aborted: Boolean) {
        if (aborted)
            sendTransfersInterruptedInfoToBackupCenter()
        else
            sendTransfersUpToDateInfoToBackupCenter()
    }

    /**
     * Sends the appropriate Backup States and Heartbeat Statuses on both Primary and
     * Secondary folders when the active Camera Uploads is interrupted by other means (e.g.
     * no user credentials, Wi-Fi not turned on)
     */
    private fun sendTransfersInterruptedInfoToBackupCenter() {
        if (isActive()) {
            // Update both Primary and Secondary Folder Backup States to TEMPORARILY_DISABLED
            updatePrimaryFolderBackupState(BackupState.TEMPORARILY_DISABLED)
            updateSecondaryFolderBackupState(BackupState.TEMPORARILY_DISABLED)

            // Send an INACTIVE Heartbeat Status for both Primary and Secondary Folders
            reportUploadInterrupted()
        }
    }

    /**
     * Sends the appropriate Backup States and Heartbeat Statuses on both Primary and
     * Secondary folders when the user complete the CU in a normal manner
     *
     * One particular case where these states are sent is when the user "Cancel all" uploads
     */
    private fun sendTransfersUpToDateInfoToBackupCenter() {
        // Update both Primary and Secondary Backup States to ACTIVE
        updatePrimaryFolderBackupState(BackupState.ACTIVE)
        updateSecondaryFolderBackupState(BackupState.ACTIVE)

        // Update both Primary and Secondary Heartbeat Statuses to UP_TO_DATE
        sendPrimaryFolderHeartbeat(HeartbeatStatus.UP_TO_DATE)
        sendSecondaryFolderHeartbeat(HeartbeatStatus.UP_TO_DATE)
    }

    private fun cancelNotification() {
        Timber.d("Cancelling notification ID is %s", notificationId)
        notificationManager.cancel(notificationId)
    }

    private suspend fun transferFinished(transfer: MegaTransfer, e: MegaError) {
        val path = transfer.path
        if (transfer.state == MegaTransfer.STATE_COMPLETED) {
            val androidCompletedTransfer = AndroidCompletedTransfer(transfer, e, context)
            addCompletedTransferUseCase(completedTransferMapper(androidCompletedTransfer))
        }

        if (e.errorCode == MegaError.API_OK) {
            Timber.d("Image Sync API_OK")
            val node = getNodeByHandle(transfer.nodeHandle)
            val isSecondary = node?.parentHandle == getSecondarySyncHandleUseCase()
            val record = getSyncRecordByPath(path, isSecondary)
            if (record != null) {
                node?.let { nonNullNode ->
                    onUploadSuccess(
                        node = nonNullNode,
                        isSecondary = record.isSecondary,
                    )
                    handleSetOriginalFingerprint(
                        node = nonNullNode,
                        originalFingerprint = record.originFingerprint.orEmpty(),
                    )
                    record.latitude?.let { latitude ->
                        record.longitude?.let { longitude ->
                            setCoordinatesUseCase(
                                nodeId = NodeId(nonNullNode.handle),
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
                    if (FileUtil.isVideoFile(transfer.path)) {
                        val img = record.localPath?.let { File(it) }
                        if (!preview.exists()) {
                            ImageProcessor.createVideoPreview(
                                context,
                                img,
                                preview
                            )
                        }
                        ImageProcessor.createThumbnail(img, thumb)
                    } else if (MimeTypeList.typeForName(transfer.path).isImage) {
                        if (!preview.exists()) {
                            ImageProcessor.createImagePreview(src, preview)
                        }
                        ImageProcessor.createThumbnail(src, thumb)
                    }
                }
                // delete database record
                deleteSyncRecord(path, isSecondary)
                // delete temp files
                if (path.startsWith(tempRoot)) {
                    val temp = File(path)
                    if (temp.exists()) {
                        temp.delete()
                    }
                }
            }
        } else if (e.errorCode == MegaError.API_EOVERQUOTA) {
            Timber.w("Over quota error: %s", e.errorCode)
            showStorageOverQuotaNotification()
            endService(aborted = true)
        } else {
            Timber.w("Image Sync FAIL: %d___%s", transfer.nodeHandle, e.errorString)
        }
        updateUpload()
    }

    /**
     * Sets the original fingerprint by calling [setOriginalFingerprint] and logs the result
     *
     * @param node the [MegaNode] to attach the [originalFingerprint] to
     * @param originalFingerprint the fingerprint of the file before modification
     */
    private suspend fun handleSetOriginalFingerprint(node: MegaNode, originalFingerprint: String) {
        runCatching {
            setOriginalFingerprint(
                node = node,
                originalFingerprint = originalFingerprint,
            )
        }.onSuccess {
            Timber.d("Set original fingerprint successful")
        }.onFailure { error -> Timber.e("Set original fingerprint error: $error") }
    }

    private suspend fun updateUpload() {
        updateProgressNotification()

        totalUploaded++
        @Suppress("DEPRECATION")
        Timber.d(
            "Total to upload: %d Total uploaded: %d Pending uploads: %d",
            totalToUpload,
            totalUploaded,
            megaApi.numPendingUploads
        )
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

    private fun isCompressorAvailable() = (videoCompressionJob?.isActive ?: false)

    private suspend fun startVideoCompression() = coroutineScope {
        val fullList = getVideoSyncRecordsByStatus(SyncStatus.STATUS_TO_COMPRESS)
        if (fullList.isNotEmpty()) {
            @Suppress("DEPRECATION")
            resetTotalUploads()
            totalUploaded = 0
            totalToUpload = 0
            totalVideoSize = getTotalVideoSizeInMB(fullList)
            Timber.d(
                "Total videos count are %d, %d mb to Conversion",
                fullList.size,
                totalVideoSize
            )
            if (shouldStartVideoCompression(totalVideoSize)) {
                videoCompressionJob = launch {
                    Timber.d("Starting compressor")
                    runCatching {
                        compressVideos(tempRoot, fullList).collect {
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
                                    onInsufficientSpace()
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
                    }.onFailure {
                        Timber.d("Video Compression Callback Exception $it")
                        endService(aborted = true)
                    }
                }
                videoCompressionJob?.join()
                videoCompressionJob = null
                onCompressFinished()
            } else {
                Timber.d("Compression queue bigger than setting, show notification to user.")
                showVideoCompressionErrorNotification()
                endService(aborted = true)
            }
        }
    }

    @SuppressLint("StringFormatInvalid")
    private suspend fun showVideoCompressionErrorNotification() {
        val intent = Intent(context, ManagerActivity::class.java)
        intent.action = Constants.ACTION_SHOW_SETTINGS
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val title = context.getString(R.string.title_compression_size_over_limit)
        val size = getVideoCompressionSizeLimitUseCase()
        val message = context.getString(
            R.string.message_compression_size_over_limit,
            context.getString(R.string.label_file_size_mega_byte, size.toString())
        )
        showNotification(title, message, pendingIntent, true)
    }

    private fun getTotalVideoSizeInMB(records: List<SyncRecord>) =
        records.sumOf { it.localPath?.let { path -> File(path).length() } ?: 0 } / (1024 * 1024)

    private suspend fun shouldStartVideoCompression(queueSize: Long): Boolean {
        if (isChargingRequired(queueSize) && !Util.isCharging(context)) {
            Timber.d("Should not start video compression.")
            return false
        }
        return true
    }

    /**
     * Not enough space available
     */
    private suspend fun onInsufficientSpace() {
        Timber.w("Insufficient space for video compression.")
        endService(aborted = true)
        showOutOfSpaceNotification()
    }

    /**
     * Update compression progress
     */
    private fun onCompressUpdateProgress(
        progress: Int,
        currentFileIndex: Int,
        totalCount: Int,
    ) {
        val message = context.getString(R.string.message_compress_video, "$progress%")
        val subText = context.getString(
            R.string.title_compress_video,
            currentFileIndex,
            totalCount
        )
        showProgressNotification(progress, defaultPendingIntent, message, subText, "")
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
        val localPath = record.localPath
        val isSecondary = record.isSecondary
        Timber.w("Compression failed for file with timestamp:  %s", record.timestamp)
        val srcFile = localPath?.let { File(it) }
        if (srcFile != null && srcFile.exists()) {
            try {
                val stat = StatFs(tempRoot)
                val availableFreeSpace = stat.availableBytes.toDouble()
                if (availableFreeSpace > srcFile.length()) {
                    setSyncRecordPendingByPath(localPath, isSecondary)
                    Timber.d("Can not compress but got enough disk space, so should be un-supported format issue")
                    record.newPath?.let { newPath ->
                        if (newPath.startsWith(tempRoot)) {
                            File(newPath).takeIf { it.exists() }?.delete()
                        }
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

    /**
     * Compression finished
     */
    private suspend fun onCompressFinished() {
        Timber.d("Video Compression Finished")
        Timber.d("Preparing to upload compressed video.")
        val compressedList = getVideoSyncRecordsByStatus(SyncStatus.STATUS_PENDING)
        if (compressedList.isNotEmpty()) {
            Timber.d("Start to upload ${compressedList.size} compressed videos.")
            startParallelUpload(compressedList, true)
        } else {
            onQueueComplete()
        }
    }

    private suspend fun updateProgressNotification() = withContext(ioDispatcher) {
        // refresh UI every 1 seconds to avoid too much workload on main thread
        val now = System.currentTimeMillis()
        lastUpdated = if (now - lastUpdated > Util.ONTRANSFERUPDATE_REFRESH_MILLIS) {
            now
        } else {
            return@withContext
        }

        @Suppress("DEPRECATION") val pendingTransfers = megaApi.numPendingUploads
        @Suppress("DEPRECATION") val totalTransfers = megaApi.totalUploads
        @Suppress("DEPRECATION") val totalSizePendingTransfer = megaApi.totalUploadBytes
        @Suppress("DEPRECATION") val totalSizeTransferred = megaApi.totalUploadedBytes

        val progressPercent = if (totalSizePendingTransfer == 0L) {
            0
        } else {
            (totalSizeTransferred.toDouble() / totalSizePendingTransfer * 100).roundToInt()
        }
        val message: String
        if (totalTransfers == 0) {
            message = context.getString(R.string.download_preparing_files)
        } else {
            val inProgress = if (pendingTransfers == 0) {
                totalTransfers
            } else {
                totalTransfers - pendingTransfers + 1
            }

            broadcastProgress(progressPercent, pendingTransfers)

            message = if (areTransfersPausedUseCase()) {
                context.getString(
                    R.string.upload_service_notification_paused,
                    inProgress,
                    totalTransfers
                )
            } else {
                context.getString(
                    R.string.upload_service_notification,
                    inProgress,
                    totalTransfers
                )
            }
        }

        val info =
            Util.getProgressSize(context, totalSizeTransferred, totalSizePendingTransfer)

        showProgressNotification(
            progressPercent,
            defaultPendingIntent,
            message,
            info,
            context.getString(R.string.settings_camera_notif_title)
        )
    }

    private fun createNotification(
        title: String,
        content: String,
        intent: PendingIntent?,
        isAutoCancel: Boolean,
    ): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                notificationChannelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setShowBadge(false)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, notificationChannelId)
        builder.setSmallIcon(R.drawable.ic_stat_camera_sync)
            .setOngoing(false)
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentText(content)
            .setOnlyAlertOnce(true)
            .setAutoCancel(isAutoCancel)
        if (intent != null) {
            builder.setContentIntent(intent)
        }
        return builder.build()
    }

    private fun showNotification(
        title: String,
        content: String,
        intent: PendingIntent?,
        isAutoCancel: Boolean,
    ) {
        val notification = createNotification(title, content, intent, isAutoCancel)
        notificationManager.notify(notificationId, notification)
    }

    private fun showProgressNotification(
        progressPercent: Int,
        pendingIntent: PendingIntent?,
        message: String,
        subText: String,
        contentText: String,
    ) {
        val builder = NotificationCompat.Builder(context, notificationChannelId)
        builder.setSmallIcon(R.drawable.ic_stat_camera_sync)
            .setProgress(100, progressPercent, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subText))
            .setContentTitle(message)
            .setContentText(contentText)
            .setOnlyAlertOnce(true)
            .setSubText(subText)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                notificationChannelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setShowBadge(true)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(notificationId, builder.build())
    }

    private fun showStorageOverQuotaNotification() {
        Timber.d("Show storage over quota notification.")
        val contentText = context.getString(R.string.download_show_info)
        val message = context.getString(R.string.overquota_alert_title)
        val intent = Intent(context, ManagerActivity::class.java)
        intent.action = Constants.ACTION_OVERQUOTA_STORAGE

        val builder = NotificationCompat.Builder(context, OVER_QUOTA_NOTIFICATION_CHANNEL_ID)
        builder.setSmallIcon(R.drawable.ic_stat_camera_sync)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(true)
            .setTicker(contentText)
            .setContentTitle(message)
            .setOngoing(false)
            .setContentText(contentText)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                OVER_QUOTA_NOTIFICATION_CHANNEL_ID,
                notificationChannelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setShowBadge(true)
            channel.setSound(null, null)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(Constants.NOTIFICATION_STORAGE_OVERQUOTA, builder.build())
    }

    private fun showOutOfSpaceNotification() {
        val intent = Intent(context, ManagerActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val title = context.getString(R.string.title_out_of_space)
        val message = context.getString(R.string.message_out_of_space)
        showNotification(title, message, pendingIntent, true)
    }
}
