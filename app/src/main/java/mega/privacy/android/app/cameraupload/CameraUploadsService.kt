package mega.privacy.android.app.cameraupload

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.AndroidCompletedTransfer
import mega.privacy.android.app.LegacyDatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.domain.usecase.AreAllUploadTransfersPaused
import mega.privacy.android.app.domain.usecase.CancelAllUploadTransfers
import mega.privacy.android.app.domain.usecase.CancelTransfer
import mega.privacy.android.app.domain.usecase.CopyNode
import mega.privacy.android.app.domain.usecase.GetCameraUploadLocalPath
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetDefaultNodeHandle
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetPrimarySyncHandle
import mega.privacy.android.app.domain.usecase.GetSecondarySyncHandle
import mega.privacy.android.app.domain.usecase.IsLocalPrimaryFolderSet
import mega.privacy.android.app.domain.usecase.IsLocalSecondaryFolderSet
import mega.privacy.android.app.domain.usecase.IsWifiNotSatisfied
import mega.privacy.android.app.domain.usecase.ProcessMediaForUpload
import mega.privacy.android.app.domain.usecase.SetOriginalFingerprint
import mega.privacy.android.app.domain.usecase.StartUpload
import mega.privacy.android.app.globalmanagement.TransfersManagement.Companion.addCompletedTransfer
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.receivers.CameraServiceIpChangeHandler
import mega.privacy.android.app.receivers.CameraServiceWakeLockHandler
import mega.privacy.android.app.receivers.CameraServiceWifiLockHandler
import mega.privacy.android.app.sync.HeartbeatStatus
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
import mega.privacy.android.app.utils.JobUtil
import mega.privacy.android.app.utils.PreviewUtils
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.mapper.camerauploads.SyncRecordTypeIntMapper
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.worker.ACTION_STOP
import mega.privacy.android.data.worker.EXTRA_ABORTED
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.domain.usecase.BroadcastCameraUploadProgress
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.CompressVideos
import mega.privacy.android.domain.usecase.CompressedVideoPending
import mega.privacy.android.domain.usecase.CreateCameraUploadFolder
import mega.privacy.android.domain.usecase.CreateCameraUploadTemporaryRootDirectory
import mega.privacy.android.domain.usecase.DeleteCameraUploadTemporaryRootDirectory
import mega.privacy.android.domain.usecase.DeleteSyncRecord
import mega.privacy.android.domain.usecase.DeleteSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.DisableCameraUploadsInDatabase
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.GetPendingSyncRecords
import mega.privacy.android.domain.usecase.GetSession
import mega.privacy.android.domain.usecase.GetSyncRecordByPath
import mega.privacy.android.domain.usecase.GetVideoSyncRecordsByStatus
import mega.privacy.android.domain.usecase.HasPreferences
import mega.privacy.android.domain.usecase.IsCameraUploadByWifi
import mega.privacy.android.domain.usecase.IsCameraUploadSyncEnabled
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.domain.usecase.IsNodeInRubbishOrDeleted
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.MonitorBatteryInfo
import mega.privacy.android.domain.usecase.MonitorCameraUploadPauseState
import mega.privacy.android.domain.usecase.MonitorChargingStoppedState
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.ResetMediaUploadTimeStamps
import mega.privacy.android.domain.usecase.ResetTotalUploads
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.domain.usecase.SetSecondaryFolderPath
import mega.privacy.android.domain.usecase.SetSecondarySyncHandle
import mega.privacy.android.domain.usecase.SetSyncLocalPath
import mega.privacy.android.domain.usecase.SetSyncRecordPendingByPath
import mega.privacy.android.domain.usecase.SetupPrimaryFolder
import mega.privacy.android.domain.usecase.SetupSecondaryFolder
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.camerauploads.AreLocationTagsEnabled
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandles
import mega.privacy.android.domain.usecase.camerauploads.GetVideoCompressionSizeLimit
import mega.privacy.android.domain.usecase.login.BackgroundFastLogin
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Service to handle upload of photos and videos
 */
@AndroidEntryPoint
class CameraUploadsService : LifecycleService() {

    companion object {

        private const val LOCAL_FOLDER_REMINDER_PRIMARY = 1908
        private const val LOCAL_FOLDER_REMINDER_SECONDARY = 1909
        private const val OVER_QUOTA_NOTIFICATION_CHANNEL_ID = "OVER_QUOTA_NOTIFICATION"
        private const val ERROR_NOT_ENOUGH_SPACE = "ERROR_NOT_ENOUGH_SPACE"
        private const val ERROR_CREATE_FILE_IO_ERROR = "ERROR_CREATE_FILE_IO_ERROR"
        private const val ERROR_SOURCE_FILE_NOT_EXIST = "SOURCE_FILE_NOT_EXIST"
        private const val LOW_BATTERY_LEVEL = 20
        private const val notificationId = Constants.NOTIFICATION_CAMERA_UPLOADS
        private const val notificationChannelId = Constants.NOTIFICATION_CHANNEL_CAMERA_UPLOADS_ID
        private const val notificationChannelName =
            Constants.NOTIFICATION_CHANNEL_CAMERA_UPLOADS_NAME

        /**
         * Get a new intent to the Camera Upload Service
         */
        fun newIntent(context: Context) = Intent(context, CameraUploadsService::class.java)
    }

    /**
     * The [MegaApiAndroid] for SDK calls, which will be removed once all direct calls have
     * been converted to Use Cases
     */
    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    /**
     * GetCameraUploadLocalPath
     */
    @Inject
    lateinit var localPath: GetCameraUploadLocalPath

    /**
     * IsLocalPrimaryFolderSet
     */
    @Inject
    lateinit var isLocalPrimaryFolderSet: IsLocalPrimaryFolderSet

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
     * HasPreferences
     */
    @Inject
    lateinit var hasPreferences: HasPreferences

    /**
     * IsCameraUploadSyncEnabled
     */
    @Inject
    lateinit var isCameraUploadSyncEnabled: IsCameraUploadSyncEnabled

    /**
     * IsCameraUploadByWifi
     */
    @Inject
    lateinit var isCameraUploadByWifi: IsCameraUploadByWifi

    /**
     * IsWifiNotSatisfied
     */
    @Inject
    lateinit var isWifiNotSatisfied: IsWifiNotSatisfied

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
     * SetSyncLocalPath
     */
    @Inject
    lateinit var setSyncLocalPath: SetSyncLocalPath

    /**
     * ShouldCompressVideo
     */
    @Inject
    lateinit var shouldCompressVideo: ShouldCompressVideo

    /**
     * SetSecondaryFolderPath
     */
    @Inject
    lateinit var setSecondaryFolderPath: SetSecondaryFolderPath

    /**
     * ClearSyncRecords
     */
    @Inject
    lateinit var clearSyncRecords: ClearSyncRecords

    /**
     * Are Location Tags Enabled
     */
    @Inject
    lateinit var areLocationTagsEnabled: AreLocationTagsEnabled

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
    lateinit var getVideoCompressionSizeLimit: GetVideoCompressionSizeLimit

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
    lateinit var getChildrenNode: GetChildrenNode

    /**
     * ProcessMediaForUpload
     */
    @Inject
    lateinit var processMediaForUpload: ProcessMediaForUpload

    /**
     * GetPrimarySyncHandle
     */
    @Inject
    lateinit var getPrimarySyncHandle: GetPrimarySyncHandle

    /**
     * GetSecondarySyncHandle
     */
    @Inject
    lateinit var getSecondarySyncHandle: GetSecondarySyncHandle

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
    lateinit var getDefaultNodeHandle: GetDefaultNodeHandle

    /**
     * LegacyDatabaseHandler
     */
    @Inject
    lateinit var tempDbHandler: LegacyDatabaseHandler

    /**
     * AreAllUploadTransfersPaused
     */
    @Inject
    lateinit var areAllUploadTransfersPaused: AreAllUploadTransfersPaused

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
     * Main dispatcher for camera upload work
     */
    @MainDispatcher
    @Inject
    lateinit var mainDispatcher: CoroutineDispatcher

    /**
     * Monitor camera upload pause state
     */
    @Inject
    lateinit var monitorCameraUploadPauseState: MonitorCameraUploadPauseState

    /**
     * Monitor connectivity
     */
    @Inject
    lateinit var monitorConnectivity: MonitorConnectivity

    /**
     * Monitor battery level
     */
    @Inject
    lateinit var monitorBatteryInfo: MonitorBatteryInfo

    /**
     * Background Fast Login
     */
    @Inject
    lateinit var backgroundFastLogin: BackgroundFastLogin

    /**
     * Get Session
     */
    @Inject
    lateinit var getSession: GetSession

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
    lateinit var cancelTransfer: CancelTransfer

    /**
     * Cancel All Upload Transfers
     */
    @Inject
    lateinit var cancelAllUploadTransfers: CancelAllUploadTransfers

    /**
     * Copy Node
     */
    @Inject
    lateinit var copyNode: CopyNode

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
    lateinit var establishCameraUploadsSyncHandles: EstablishCameraUploadsSyncHandles

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
     * Delete camera upload temporary root directory
     */
    @Inject
    lateinit var deleteCameraUploadTemporaryRootDirectory: DeleteCameraUploadTemporaryRootDirectory

    /**
     * Broadcast camera upload progress
     */
    @Inject
    lateinit var broadcastCameraUploadProgress: BroadcastCameraUploadProgress

    /**
     * Coroutine Scope for camera upload work
     */
    private var coroutineScope: CoroutineScope? = null

    /**
     * True if the camera uploads attributes have already been requested
     * from the server
     */
    private var missingAttributesChecked = false

    /**
     * Notification manager used to display notifications
     */
    private var notificationManager: NotificationManager? = null

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

    private fun monitorUploadPauseStatus() {
        coroutineScope?.launch {
            monitorCameraUploadPauseState().collect {
                updateProgressNotification()
            }
        }
    }

    private fun monitorConnectivityStatus() {
        coroutineScope?.launch {
            monitorConnectivity().collect {
                if (!it || isWifiNotSatisfied()) {
                    endService(
                        cancelMessage = "Camera Upload by Wifi only but Mobile Network - Cancel Camera Upload",
                        aborted = true
                    )
                }
            }
        }
    }

    private fun monitorBatteryLevelStatus() {
        coroutineScope?.launch {
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
        coroutineScope?.launch {
            monitorChargingStoppedState().collect {
                if (isChargingRequired(totalVideoSize)) {
                    Timber.d("Detected device stops charging.")
                    videoCompressionJob?.cancel()
                }
            }
        }
    }

    /**
     * Service starts
     */
    override fun onCreate() {
        super.onCreate()
        createDefaultNotificationPendingIntent()
        startForegroundNotification()
    }

    /**
     * Service ends
     */
    override fun onDestroy() {
        Timber.d("Service destroys.")
        super.onDestroy()
        stopActiveHeartbeat()
        coroutineScope?.cancel()
    }

    /**
     * Bind service
     */
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    /**
     * Start service work
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.d("Starting CameraUpload service (flags: %d, startId: %d)", flags, startId)

        when (intent?.action) {
            ACTION_STOP -> {
                Timber.d("Received Stop CU service command")
                if (coroutineScope?.isActive == true) {
                    Timber.d("active process, stop service")
                    coroutineScope?.launch {
                        val aborted = intent.getBooleanExtra(EXTRA_ABORTED, false)
                        endService(
                            cancelMessage = "Camera Upload Stop Intent Action - Stop Camera Upload",
                            aborted = aborted
                        )
                    }
                }
            }
            else -> {
                Timber.d("Received Start CU service command")
                if (coroutineScope?.isActive != true) {
                    Timber.d("No active process, start service")
                    coroutineScope = CoroutineScope(ioDispatcher)
                    coroutineScope?.launch {
                        initService()
                        startWorker()
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    /**
     * Cancels a pending [MegaTransfer] through [CancelTransfer],
     * and call [ResetTotalUploads] after every cancellation to reset the total uploads if
     * there are no more pending uploads
     *
     * @param transfer the [MegaTransfer] to be cancelled
     */
    private suspend fun cancelPendingTransfer(transfer: MegaTransfer) {
        runCatching { cancelTransfer(transfer) }
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
     * Show a foreground notification.
     * It's a requirement of Android system for foreground service.
     * Should call this both when "onCreate" and "onStartCommand".
     */
    private fun startForegroundNotification() {
        if (notificationManager == null) {
            notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        }
        val notification = createNotification(
            getString(R.string.section_photo_sync),
            getString(R.string.settings_camera_notif_initializing_title),
            null,
            false
        )
        startForeground(notificationId, notification)
    }

    /**
     * Function that starts the Camera Uploads functionality
     */
    private suspend fun startWorker() {
        runCatching {
            val state = canRunCameraUploads()
            if (state == StartCameraUploadsState.CAN_RUN_CAMERA_UPLOADS) {
                Timber.d("Calling startWorker() successful. Starting Camera Uploads")
                hideLocalFolderPathNotifications()
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
    private suspend fun hideLocalFolderPathNotifications() {
        if (hasLocalPrimaryFolder()) notificationManager?.cancel(LOCAL_FOLDER_REMINDER_PRIMARY)
        if (hasLocalSecondaryFolder()) notificationManager?.cancel(LOCAL_FOLDER_REMINDER_SECONDARY)
    }

    /**
     * Checks if Camera Uploads can run by evaluating the following conditions in order:
     *
     * 1. The Preferences exist - [preferencesExist],
     * 2. The Camera Uploads sync is enabled - [cameraUploadsSyncEnabled],
     * 4. The Device battery level is above the minimum threshold - [deviceAboveMinimumBatteryLevel],
     * 5. The Camera Uploads local path exists - [hasCameraUploadsLocalPath],
     * 6. The Wi-Fi Constraint is satisfied - [isWifiConstraintSatisfied],
     * 7. The local Primary Folder exists - [hasLocalPrimaryFolder],
     * 8. The local Secondary Folder exists - [hasLocalSecondaryFolder],
     * 9. The user is logged in - [isUserLoggedIn],
     * 10. The user Camera Uploads attribute exists - [missingAttributesChecked],
     * 11. The Primary Folder exists - [areFoldersEstablished],
     * 12. The Secondary Folder exists if Enable Secondary Media Uploads is enabled - [areFoldersEstablished]
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
            !hasCameraUploadsLocalPath() -> StartCameraUploadsState.MISSING_LOCAL_PATH
            !hasLocalPrimaryFolder() -> StartCameraUploadsState.MISSING_LOCAL_PRIMARY_FOLDER
            !hasLocalSecondaryFolder() -> StartCameraUploadsState.MISSING_LOCAL_SECONDARY_FOLDER
            !isUserLoggedIn() -> StartCameraUploadsState.LOGGED_OUT_USER
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
            StartCameraUploadsState.MISSING_LOCAL_PATH,
            StartCameraUploadsState.UNSATISFIED_WIFI_CONSTRAINT,
            -> {
                Timber.e("Stop Camera Uploads due to $state")
                endService(aborted = true)
            }

            StartCameraUploadsState.MISSING_LOCAL_PRIMARY_FOLDER -> {
                Timber.e("Local Primary Folder is disabled. Stop Camera Uploads")
                handleLocalPrimaryFolderDisabled()
                endService(aborted = true)
            }

            StartCameraUploadsState.MISSING_LOCAL_SECONDARY_FOLDER -> {
                Timber.e("Local Secondary Folder is disabled. Stop Camera Uploads")
                handleLocalSecondaryFolderDisabled()
                endService(aborted = true)
            }

            StartCameraUploadsState.LOGGED_OUT_USER -> {
                Timber.w("User is logged out. Perform a Complete Fast Login")
                performCompleteFastLogin()
            }

            StartCameraUploadsState.MISSING_USER_ATTRIBUTE -> {
                Timber.w("Handle the missing Camera Uploads user attribute")
                runCatching {
                    establishCameraUploadsSyncHandles()
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
     * Checks if the user is logged in by calling [getSession] and checking whether the
     * session exists or not
     *
     * @return true if the user session exists, and false if otherwise
     */
    private suspend fun isUserLoggedIn(): Boolean =
        getSession().orEmpty().isNotBlank().also {
            if (!it) Timber.w("No user session currently exists")
        }

    /**
     * Checks if the Preferences from [hasPreferences] exist
     *
     * @return true if it exists, and false if otherwise
     */
    private suspend fun preferencesExist(): Boolean =
        hasPreferences().also {
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
     * Checks if the Camera Uploads local path from [localPath] exists
     *
     * @return true if the Camera Uploads local path exists, and false if otherwise
     */
    private suspend fun hasCameraUploadsLocalPath(): Boolean =
        !localPath().isNullOrBlank().also {
            if (it) Timber.w("Camera Uploads local path is empty")
        }

    /**
     * Checks if the Wi-Fi constraint from the negated [isWifiNotSatisfied] is satisfied
     *
     * @return true if the Wi-Fi constraint is satisfied, and false if otherwise
     */
    private suspend fun isWifiConstraintSatisfied(): Boolean =
        !isWifiNotSatisfied().also {
            if (it) Timber.w("Cannot start, Wi-Fi required")
        }

    /**
     * Checks if the local Primary Folder from [isLocalPrimaryFolderSet] exists
     *
     * @return true if it exists, and false if otherwise
     */
    private suspend fun hasLocalPrimaryFolder(): Boolean =
        isLocalPrimaryFolderSet().also {
            if (!it) Timber.w("Local Primary Folder is not set")
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
        val primarySyncHandle = getPrimarySyncHandle()
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
            Timber.w("The local primary folder is missing")

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
            getString(R.string.section_photo_sync),
            getString(R.string.settings_camera_notif_checking_title),
            defaultPendingIntent,
            false
        )
        checkUploadNodes()
    }

    private suspend fun checkUploadNodes() {
        Timber.d("Get Pending Files from Media Store Database")
        val primaryUploadNode = getNodeByHandle(getPrimarySyncHandle())
        if (primaryUploadNode == null) {
            Timber.d("ERROR: Primary Parent Folder is NULL")
            endService(aborted = true)
            return
        }
        val secondaryUploadNode = if (isSecondaryFolderEnabled()) {
            Timber.d("Secondary Upload is ENABLED")
            getNodeByHandle(getSecondarySyncHandle())
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
                // Make sure to re schedule the job
                JobUtil.scheduleCameraUploadJob(this)
                endService()
                deleteCameraUploadTemporaryRootDirectory()
            }
        } else {
            Timber.d("Start to upload %d files.", finalList.size)
            startParallelUpload(finalList, false)
        }
    }

    private suspend fun startParallelUpload(
        finalList: List<SyncRecord>,
        isCompressedVideo: Boolean,
    ) {
        val copyFileAsyncList = mutableListOf<Deferred<Unit>>()
        // If the Service detects that all upload transfers are paused when turning on
        // Camera Uploads, update the Primary and Secondary Folder Backup States to
        // BackupState.PAUSE_UPLOADS
        if (areAllUploadTransfersPaused()) {
            Timber.d("All Pending Uploads Paused. Send Backup State = ${BackupState.PAUSE_UPLOADS}")
            updatePrimaryFolderBackupState(BackupState.PAUSE_UPLOADS)
            updateSecondaryFolderBackupState(BackupState.PAUSE_UPLOADS)
        }
        val primaryUploadNode = getNodeByHandle(getPrimarySyncHandle())
        val secondaryUploadNode = getNodeByHandle(getSecondarySyncHandle())

        startActiveHeartbeat(finalList)
        for (file in finalList) {
            val isSecondary = file.isSecondary
            val parent = (if (isSecondary) secondaryUploadNode else primaryUploadNode) ?: continue

            if (file.type == syncRecordTypeIntMapper(SyncRecordType.TYPE_PHOTO) && !file.isCopyOnly) {
                if (!areLocationTagsEnabled()) {
                    var newPath = createTempFile(file)
                    // IOException occurs.
                    if (ERROR_CREATE_FILE_IO_ERROR == newPath) continue

                    // Only retry for 60 seconds
                    var counter = 60
                    while (ERROR_NOT_ENOUGH_SPACE == newPath && counter != 0) {
                        counter--
                        try {
                            Timber.d("Waiting for disk space to process")
                            delay(1000)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }

                        //show no space notification
                        @Suppress("DEPRECATION")
                        if (megaApi.numPendingUploads == 0) {
                            Timber.w("Stop service due to out of space issue")
                            endService(aborted = true)
                            val title = getString(R.string.title_out_of_space)
                            val message = getString(R.string.error_not_enough_free_space)
                            val intent = Intent(this, ManagerActivity::class.java)
                            val pendingIntent = PendingIntent.getActivity(
                                this,
                                0,
                                intent,
                                PendingIntent.FLAG_IMMUTABLE
                            )
                            showNotification(title, message, pendingIntent, true)
                            return
                        }
                        newPath = createTempFile(file)
                    }
                    if (newPath != file.newPath) {
                        file.newPath = newPath
                    }
                } else {
                    // Set as don't remove GPS
                    file.newPath = file.localPath
                }
            }

            var path: String?
            if (isCompressedVideo || file.type == syncRecordTypeIntMapper(SyncRecordType.TYPE_PHOTO)
                || file.type == syncRecordTypeIntMapper(SyncRecordType.TYPE_VIDEO) && shouldCompressVideo()
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
                    coroutineScope?.let { scope ->
                        getNodeByHandle(nodeHandle)?.let { nodeToCopy ->
                            totalToUpload++
                            copyFileAsyncList.add(scope.async {
                                handleCopyNode(
                                    nodeToCopy = nodeToCopy,
                                    newNodeParent = parent,
                                    newNodeName = file.fileName.orEmpty(),
                                )
                            })
                        }

                    }

                }
            } else {
                val toUpload = path?.let { File(it) }
                if ((toUpload != null) && toUpload.exists()) {
                    // compare size
                    val node = checkExistBySize(parent, toUpload.length())
                    if (node != null && node.originalFingerprint == null) {
                        Timber.d(
                            "Node with handle: %d already exists, delete record from database.",
                            node.handle
                        )
                        path?.let {
                            deleteSyncRecord(it, isSecondary)
                        }
                    } else {
                        totalToUpload++
                        val lastModified = getLastModifiedTime(file)

                        // If the local file path exists, call the Use Case to upload the file
                        path?.let { nonNullFilePath ->
                            coroutineScope?.launch {
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
                                    ensureActive()
                                    onGlobalTransferUpdated(globalTransfer)
                                }
                            }
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
        if (totalToUpload == totalUploaded) {
            if (compressedVideoPending() && isCompressorAvailable()) {
                Timber.d("Got pending videos, will start compress.")
                startVideoCompression()
            } else {
                Timber.d("No pending videos, finish.")
                onQueueComplete()
            }
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
     * Perform a copy operation through [CopyNode]
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
            copyNode(
                nodeToCopy = nodeToCopy,
                newNodeParent = newNodeParent,
                newNodeName = newNodeName,
            )
        }.onSuccess { nodeId ->
            Timber.d("Copy node successful")
            getNodeByHandle(nodeId.longValue)?.let { retrievedNode ->
                val fingerprint = retrievedNode.fingerprint
                val isSecondary = retrievedNode.parentHandle == getSecondarySyncHandle()
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

    private suspend fun checkExistBySize(parent: MegaNode, size: Long): MegaNode? {
        val nodeList = getChildrenNode(parent, SortOrder.ORDER_ALPHABETICAL_ASC)
        for (node in nodeList) {
            if (node.size == size) {
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
     * Executes certain behavior when the Local Primary Folder is disabled
     */
    private suspend fun handleLocalPrimaryFolderDisabled() {
        localFolderUnavailableNotification(
            R.string.camera_notif_primary_local_unavailable,
            LOCAL_FOLDER_REMINDER_PRIMARY
        )
        disableCameraUploadsInDatabase()
        setSyncLocalPath(Constants.INVALID_NON_NULL_VALUE)
        setSecondaryFolderPath(Constants.INVALID_NON_NULL_VALUE)
        // Refresh SettingsCameraUploadsFragment
        sendBroadcast(Intent(BroadcastConstants.ACTION_REFRESH_CAMERA_UPLOADS_SETTING))
    }

    /**
     * Executes certain behavior when the Local Secondary Folder is disabled
     */
    private suspend fun handleLocalSecondaryFolderDisabled() {
        localFolderUnavailableNotification(
            R.string.camera_notif_secondary_local_unavailable,
            LOCAL_FOLDER_REMINDER_SECONDARY
        )
        // Disable Media Uploads only
        resetMediaUploadTimeStamps()
        disableMediaUploadSettings()
        setSecondaryFolderPath(SettingsConstants.INVALID_PATH)
        sendBroadcast(Intent(BroadcastConstants.ACTION_DISABLE_MEDIA_UPLOADS_SETTING))
    }

    /**
     * When the user is not logged in, perform a Complete Fast Login procedure
     */
    private suspend fun performCompleteFastLogin() {
        Timber.d("Waiting for the user to complete the Fast Login procedure")

        // Legacy support: isLoggingIn needs to be set in order to inform other parts of the
        // app that a Login Procedure is occurring
        MegaApplication.isLoggingIn = true
        val result = runCatching { backgroundFastLogin() }
        MegaApplication.isLoggingIn = false

        if (result.isSuccess) {
            Timber.d("Complete Fast Login procedure successful. Get cookies settings after login")
            MegaApplication.getInstance().checkEnabledCookies()
            Timber.d("Start CameraUploadsService")
            startWorker()
        } else {
            Timber.e("Complete Fast Login procedure unsuccessful with error ${result.exceptionOrNull()}. Stop CameraUploadsService")
            endService(aborted = true)
        }
    }

    /**
     * When local folder is unavailable, CU cannot launch, need to show a notification to let the user know.
     *
     * @param resId  The content text of the notification. Here is the string's res id.
     * @param notificationId Notification id, can cancel the notification by the same id when need.
     */
    private fun localFolderUnavailableNotification(resId: Int, notificationId: Int) {
        var isShowing = false
        notificationManager?.let {
            for (notification in it.activeNotifications) {
                if (notification.id == notificationId) {
                    isShowing = true
                }
            }
        }
        if (!isShowing) {
            val notification = createNotification(
                getString(R.string.section_photo_sync),
                getString(resId),
                null,
                false
            )
            notificationManager?.notify(notificationId, notification)
        }
    }

    /**
     * Gets the Primary Folder handle
     *
     * @return the Primary Folder handle
     */
    private suspend fun getPrimaryFolderHandle(): Long =
        getDefaultNodeHandle(getString(R.string.section_photo_sync))

    /**
     * Gets the Secondary Folder handle
     *
     * @return the Secondary Folder handle
     */
    private suspend fun getSecondaryFolderHandle(): Long {
        // get Secondary folder handle of user
        val secondarySyncHandle = getSecondarySyncHandle()
        if (secondarySyncHandle == MegaApiJava.INVALID_HANDLE || getNodeByHandle(secondarySyncHandle) == null) {
            // if it's invalid or deleted then return the default value
            return getDefaultNodeHandle(getString(R.string.section_secondary_media_uploads))
        }
        return secondarySyncHandle
    }

    /**
     * Create the primary upload folder on the cloud drive
     * If the creation succeed, set up the primary folder in local
     *
     * @throws Exception if the creation of the primary upload folder failed
     */
    private suspend fun createAndSetupPrimaryUploadFolder() {
        createCameraUploadFolder(getString(R.string.section_photo_sync))?.let {
            Timber.d("Primary Folder successfully created with handle $it. Setting up Primary Folder")
            setupPrimaryFolder(it)
        } ?: throw Exception("Failed to create primary upload folder")
    }

    /**
     * Create the secondary upload folder on the cloud drive
     * If the creation succeed, set up the primary folder in local
     *
     * @throws Exception if the creation of the secondary upload folder failed
     */
    private suspend fun createAndSetupSecondaryUploadFolder() {
        createCameraUploadFolder(getString(R.string.section_secondary_media_uploads))?.let {
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

        // Display notification
        startForegroundNotification()

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
        val intent = Intent(this, ManagerActivity::class.java).apply {
            action = Constants.ACTION_CANCEL_CAM_SYNC
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.PENDING_TAB)
        }
        defaultPendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }


    /**
     * When [CameraUploadsService] is initialized, start the Wake and Wifi Locks
     */
    private fun startWakeAndWifiLocks() {
        cameraServiceWakeLockHandler.startWakeLock()
        cameraServiceWifiLockHandler.startWifiLock()
    }

    /**
     * When [CameraUploadsService] ends, Stop both Wake and Wifi Locks
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
        cancelMessage: String = "Ending Service",
        aborted: Boolean = false,
    ) {
        Timber.d("Finish Camera upload process.")
        stopWakeAndWifiLocks()

        if (coroutineScope?.isActive == true) {
            sendStatusToBackupCenter(aborted = aborted)
            cancelAllPendingTransfers()
            broadcastProgress(100, 0)
            videoCompressionJob?.cancel()
            coroutineScope?.cancel(CancellationException(cancelMessage))
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        cancelNotification()
        stopSelf()
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
        notificationManager?.let {
            Timber.d("Cancelling notification ID is %s", notificationId)
            it.cancel(notificationId)
            return
        }
        Timber.w("No notification to cancel")
    }

    private suspend fun transferFinished(transfer: MegaTransfer, e: MegaError) {
        val path = transfer.path
        if (transfer.state == MegaTransfer.STATE_COMPLETED) {
            addCompletedTransfer(
                AndroidCompletedTransfer(transfer, e),
                tempDbHandler
            )
        }

        if (e.errorCode == MegaError.API_OK) {
            Timber.d("Image Sync API_OK")
            val node = getNodeByHandle(transfer.nodeHandle)
            val isSecondary = node?.parentHandle == getSecondarySyncHandle()
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
                            megaApi.setNodeCoordinates(
                                nonNullNode,
                                latitude.toDouble(),
                                longitude.toDouble(),
                                null
                            )
                        }
                    }
                }
                val src = record.localPath?.let { File(it) }
                if (src != null && src.exists()) {
                    Timber.d("Creating preview")
                    val previewDir = PreviewUtils.getPreviewFolder(this)
                    val preview = File(
                        previewDir,
                        MegaApiAndroid.handleToBase64(transfer.nodeHandle) + FileUtil.JPG_EXTENSION
                    )
                    val thumbDir = ThumbnailUtils.getThumbFolder(this)
                    val thumb = File(
                        thumbDir,
                        MegaApiAndroid.handleToBase64(transfer.nodeHandle) + FileUtil.JPG_EXTENSION
                    )
                    if (FileUtil.isVideoFile(transfer.path)) {
                        val img = record.localPath?.let { File(it) }
                        if (!preview.exists()) {
                            ImageProcessor.createVideoPreview(
                                this@CameraUploadsService,
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
        if (totalToUpload == totalUploaded) {
            Timber.d("Photo upload finished, now checking videos")
            if (compressedVideoPending() && isCompressorAvailable()) {
                Timber.d("Got pending videos, will start compress")
                startVideoCompression()
            } else {
                Timber.d("No pending videos, finish")
                onQueueComplete()
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

    private fun isCompressorAvailable() = !(videoCompressionJob?.isActive ?: false)

    private suspend fun startVideoCompression() {
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
                videoCompressionJob = coroutineScope?.launch {
                    Timber.d("Starting compressor")
                    runCatching {
                        compressVideos(tempRoot, fullList).collect {
                            ensureActive()
                            when (it) {
                                is VideoCompressionState.Failed -> {
                                    onCompressFailed(fullList.first { record -> record.id == it.id })
                                }

                                VideoCompressionState.Finished -> {
                                    onCompressFinished()
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
                        videoCompressionJob?.cancel()
                        videoCompressionJob = null
                    }.onFailure {
                        Timber.d("Video Compression Callback Exception $it")
                        endService(aborted = true)
                    }
                }
            } else {
                Timber.d("Compression queue bigger than setting, show notification to user.")
                showVideoCompressionErrorNotification()
                endService(aborted = true)
            }
        }
    }

    @SuppressLint("StringFormatInvalid")
    private suspend fun showVideoCompressionErrorNotification() {
        val intent = Intent(this, ManagerActivity::class.java)
        intent.action = Constants.ACTION_SHOW_SETTINGS
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val title = getString(R.string.title_compression_size_over_limit)
        val size = getVideoCompressionSizeLimit()
        val message = getString(
            R.string.message_compression_size_over_limit,
            getString(R.string.label_file_size_mega_byte, size.toString())
        )
        showNotification(title, message, pendingIntent, true)
    }

    private fun getTotalVideoSizeInMB(records: List<SyncRecord>) =
        records.sumOf { it.localPath?.let { path -> File(path).length() } ?: 0 } / (1024 * 1024)

    private suspend fun shouldStartVideoCompression(queueSize: Long): Boolean {
        if (isChargingRequired(queueSize) && !Util.isCharging(this)) {
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
        showOutOfSpaceNotification()
        endService(aborted = true)
    }

    private fun showOutOfSpaceNotification() {
        val intent = Intent(this, ManagerActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val title = resources.getString(R.string.title_out_of_space)
        val message = resources.getString(R.string.message_out_of_space)
        showNotification(title, message, pendingIntent, true)
    }

    /**
     * Update compression progress
     */
    private fun onCompressUpdateProgress(
        progress: Int,
        currentFileIndex: Int,
        totalCount: Int,
    ) {
        val message = getString(R.string.message_compress_video, "$progress%")
        val subText = getString(
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

    private suspend fun updateProgressNotification() {
        // refresh UI every 1 seconds to avoid too much workload on main thread
        val now = System.currentTimeMillis()
        lastUpdated = if (now - lastUpdated > Util.ONTRANSFERUPDATE_REFRESH_MILLIS) {
            now
        } else {
            return
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
            message = getString(R.string.download_preparing_files)
        } else {
            val inProgress = if (pendingTransfers == 0) {
                totalTransfers
            } else {
                totalTransfers - pendingTransfers + 1
            }

            broadcastProgress(progressPercent, pendingTransfers)

            message = if (megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)) {
                getString(
                    R.string.upload_service_notification_paused,
                    inProgress,
                    totalTransfers
                )
            } else {
                getString(
                    R.string.upload_service_notification,
                    inProgress,
                    totalTransfers
                )
            }
        }

        val info =
            Util.getProgressSize(this, totalSizeTransferred, totalSizePendingTransfer)

        withContext(mainDispatcher) {
            showProgressNotification(
                progressPercent,
                defaultPendingIntent,
                message,
                info,
                getString(R.string.settings_camera_notif_title)
            )
        }
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
            notificationManager?.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, notificationChannelId)
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
        notificationManager?.notify(notificationId, notification)
    }

    private fun showProgressNotification(
        progressPercent: Int,
        pendingIntent: PendingIntent?,
        message: String,
        subText: String,
        contentText: String,
    ) {
        val builder = NotificationCompat.Builder(this, notificationChannelId)
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
            notificationManager?.createNotificationChannel(channel)
        }
        notificationManager?.notify(notificationId, builder.build())
    }

    private fun showStorageOverQuotaNotification() {
        Timber.d("Show storage over quota notification.")
        val contentText = getString(R.string.download_show_info)
        val message = getString(R.string.overquota_alert_title)
        val intent = Intent(this, ManagerActivity::class.java)
        intent.action = Constants.ACTION_OVERQUOTA_STORAGE

        val builder = NotificationCompat.Builder(this, OVER_QUOTA_NOTIFICATION_CHANNEL_ID)
        builder.setSmallIcon(R.drawable.ic_stat_camera_sync)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
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
            notificationManager?.createNotificationChannel(channel)
        }
        notificationManager?.notify(Constants.NOTIFICATION_STORAGE_OVERQUOTA, builder.build())
    }

    private fun removeGPSCoordinates(filePath: String?) {
        try {
            filePath?.let {
                val exif = ExifInterface(it)
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "0/1,0/1,0/1000")
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "0")
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "0/1,0/1,0/1000")
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "0")
                exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "0/1,0/1,0/1000")
                exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, "0")
                exif.saveAttributes()
            }
        } catch (e: IOException) {
            Timber.e(e)
            e.printStackTrace()
        }
    }

    private fun createTempFile(file: SyncRecord): String? {
        val srcFile = file.localPath?.let { File(it) }
        if (srcFile != null && !srcFile.exists()) {
            Timber.d(ERROR_SOURCE_FILE_NOT_EXIST)
            return ERROR_SOURCE_FILE_NOT_EXIST
        }

        try {
            val stat = StatFs(tempRoot)
            val availableFreeSpace = stat.availableBytes.toDouble()
            if (srcFile != null && availableFreeSpace <= srcFile.length()) {
                Timber.d(ERROR_NOT_ENOUGH_SPACE)
                return ERROR_NOT_ENOUGH_SPACE
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            Timber.e(ex)
        }
        val destPath = file.newPath
        val destFile = destPath?.let { File(it) }
        try {
            FileUtil.copyFile(srcFile, destFile)
            removeGPSCoordinates(destPath)
        } catch (e: IOException) {
            e.printStackTrace()
            Timber.e(e)
            return ERROR_CREATE_FILE_IO_ERROR
        }
        return destPath
    }
}
