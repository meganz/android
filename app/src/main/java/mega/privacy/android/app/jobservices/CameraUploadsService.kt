package mega.privacy.android.app.jobservices

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleService
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.app.AndroidCompletedTransfer
import mega.privacy.android.app.LegacyDatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.VideoCompressor
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.constants.EventConstants.EVENT_TRANSFER_UPDATE
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.domain.usecase.AreAllUploadTransfersPaused
import mega.privacy.android.app.domain.usecase.CancelAllUploadTransfers
import mega.privacy.android.app.domain.usecase.CancelTransfer
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
import mega.privacy.android.app.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.app.domain.usecase.SetSecondarySyncHandle
import mega.privacy.android.app.globalmanagement.TransfersManagement.Companion.addCompletedTransfer
import mega.privacy.android.app.listeners.CreateFolderListener
import mega.privacy.android.app.listeners.CreateFolderListener.ExtraAction
import mega.privacy.android.app.listeners.GetCameraUploadAttributeListener
import mega.privacy.android.app.listeners.SetAttrUserListener
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.receivers.CameraServiceIpChangeHandler
import mega.privacy.android.app.receivers.NetworkTypeChangeReceiver
import mega.privacy.android.app.receivers.NetworkTypeChangeReceiver.OnNetworkTypeChangeCallback
import mega.privacy.android.app.sync.BackupState
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
import mega.privacy.android.app.utils.CameraUploadUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.ImageProcessor
import mega.privacy.android.app.utils.JobUtil
import mega.privacy.android.app.utils.PreviewUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.conversion.VideoCompressionCallback
import mega.privacy.android.data.mapper.SyncRecordTypeIntMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.CompleteFastLogin
import mega.privacy.android.domain.usecase.CompressedVideoPending
import mega.privacy.android.domain.usecase.DeleteSyncRecord
import mega.privacy.android.domain.usecase.DeleteSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.GetChargingOnSizeString
import mega.privacy.android.domain.usecase.GetPendingSyncRecords
import mega.privacy.android.domain.usecase.GetRemoveGps
import mega.privacy.android.domain.usecase.GetSession
import mega.privacy.android.domain.usecase.GetSyncRecordByPath
import mega.privacy.android.domain.usecase.GetVideoQuality
import mega.privacy.android.domain.usecase.GetVideoSyncRecordsByStatus
import mega.privacy.android.domain.usecase.HasPendingUploads
import mega.privacy.android.domain.usecase.HasPreferences
import mega.privacy.android.domain.usecase.IsCameraUploadByWifi
import mega.privacy.android.domain.usecase.IsCameraUploadSyncEnabled
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.MonitorBatteryInfo
import mega.privacy.android.domain.usecase.MonitorCameraUploadPauseState
import mega.privacy.android.domain.usecase.MonitorChargingStoppedState
import mega.privacy.android.domain.usecase.SetSecondaryFolderPath
import mega.privacy.android.domain.usecase.SetSyncLocalPath
import mega.privacy.android.domain.usecase.SetSyncRecordPendingByPath
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferListenerInterface
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Service to handle upload of photos and videos
 */
@AndroidEntryPoint
class CameraUploadsService : LifecycleService(), OnNetworkTypeChangeCallback,
    MegaRequestListenerInterface, MegaTransferListenerInterface, VideoCompressionCallback {

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
         * Camera Uploads
         */
        const val CAMERA_UPLOADS_ENGLISH = "Camera Uploads"

        /**
         * Secondary Uploads
         */
        const val SECONDARY_UPLOADS_ENGLISH = "Media Uploads"

        /**
         * Stop Camera Sync
         */
        const val ACTION_STOP = "STOP_SYNC"

        /**
         * Cancel all actions
         */
        const val ACTION_CANCEL_ALL = "CANCEL_ALL"

        /**
         * Ignore extra attributes
         */
        const val EXTRA_IGNORE_ATTR_CHECK = "EXTRA_IGNORE_ATTR_CHECK"

        /**
         * Primary sync handle successfully set
         */
        const val EXTRA_PRIMARY_SYNC_SUCCESS = "EXTRA_PRIMARY_SYNC_SUCCESS"

        /**
         * Camera Uploads Cache Folder
         */
        const val CU_CACHE_FOLDER = "cu"

        private var ignoreAttr = false
        private var running = false

        /**
         * Creating primary folder now
         */
        @JvmField
        var isCreatingPrimary = false

        /**
         * Creating secondary folder now
         */
        @JvmField
        var isCreatingSecondary = false

        /**
         * Is Camera Upload running now
         */
        @Volatile
        var isServiceRunning = false
            private set
    }

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
     * GetRemoveGps
     */
    @Inject
    lateinit var getRemoveGps: GetRemoveGps

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
     * GetVideoQuality
     */
    @Inject
    lateinit var getVideoQuality: GetVideoQuality

    /**
     * GetChargingOnSizeString
     */
    @Inject
    lateinit var getChargingOnSizeString: GetChargingOnSizeString

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
     * DatabaseHandler
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
     * Coroutine dispatcher for camera upload work
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
     * Monitor battery level
     */
    @Inject
    lateinit var monitorBatteryInfo: MonitorBatteryInfo

    /**
     * Complete Fast Login
     */
    @Inject
    lateinit var completeFastLogin: CompleteFastLogin

    /**
     * Get Session
     */
    @Inject
    lateinit var getSession: GetSession

    /**
     * Is Node In Rubbish
     */
    @Inject
    lateinit var isNodeInRubbish: IsNodeInRubbish

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
     * Has Pending Uploads
     */
    @Inject
    lateinit var hasPendingUploads: HasPendingUploads

    /**
     * Coroutine Scope for camera upload work
     */
    private var coroutineScope: CoroutineScope? = null

    private var app: MegaApplication? = null
    private var megaApi: MegaApiAndroid? = null
    private var megaApiFolder: MegaApiAndroid? = null
    private var receiver: NetworkTypeChangeReceiver? = null
    private var wifiLock: WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var videoCompressor: VideoCompressor? = null
    private var notification: Notification? = null
    private var notificationManager: NotificationManager? = null
    private var builder: NotificationCompat.Builder? = null
    private var intent: Intent? = null
    private var deviceAboveMinimumBatteryLevel: Boolean = true
    private var pendingIntent: PendingIntent? = null
    private var tempRoot: String? = null
    private var isOverQuota = false
    private var canceled = false
    private var stopByNetworkStateChange = false
    private var isPrimaryHandleSynced = false
    private var totalUploaded = 0
    private var totalToUpload = 0
    private var lastUpdated: Long = 0
    private var getAttrUserListener: GetCameraUploadAttributeListener? = null
    private var setAttrUserListener: SetAttrUserListener? = null
    private var createFolderListener: CreateFolderListener? = null
    private val cuTransfers: MutableList<MegaTransfer> = mutableListOf()

    private fun monitorUploadPauseStatus() {
        coroutineScope?.launch {
            monitorCameraUploadPauseState().collect {
                updateProgressNotification()
            }
        }
    }

    private fun monitorBatteryLevelStatus() {
        coroutineScope?.launch {
            monitorBatteryInfo().collect {
                deviceAboveMinimumBatteryLevel = (it.level > LOW_BATTERY_LEVEL || it.isCharging)
                if (!deviceAboveMinimumBatteryLevel) {
                    coroutineScope?.cancel("Low Battery - Cancel Camera Upload")
                    for (transfer in cuTransfers) {
                        handleCancelTransfer(transfer)
                    }
                    sendTransfersInterruptedInfoToBackupCenter()
                    endService()
                }
            }
        }
    }

    private fun monitorChargingStoppedStatus() {
        coroutineScope?.launch {
            monitorChargingStoppedState().collect {
                if (isChargingRequired((videoCompressor?.totalInputSize ?: 0) / (1024 * 1024))) {
                    Timber.d("Detected device stops charging.")
                    videoCompressor?.stop()
                }
            }
        }
    }

    /**
     * Service starts
     */
    override fun onCreate() {
        super.onCreate()
        coroutineScope = CoroutineScope(ioDispatcher)
        startForegroundNotification()
        monitorChargingStoppedStatus()
        monitorBatteryLevelStatus()
        monitorUploadPauseStatus()
        getAttrUserListener = GetCameraUploadAttributeListener(this)
        setAttrUserListener = SetAttrUserListener(this)
        createFolderListener = CreateFolderListener(this, ExtraAction.INIT_CAMERA_UPLOAD)
    }

    /**
     * Service ends
     */
    override fun onDestroy() {
        Timber.d("Service destroys.")
        super.onDestroy()
        isServiceRunning = false
        receiver?.let { unregisterReceiver(it) }
        getAttrUserListener = null
        setAttrUserListener = null
        createFolderListener = null
        megaApi?.let {
            it.removeRequestListener(this)
            it.removeTransferListener(this)
        }
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
     * Network type change
     */
    override fun onTypeChanges(type: Int) {
        Timber.d("Network type change to: %s", type)
        coroutineScope?.launch {
            stopByNetworkStateChange =
                type == NetworkTypeChangeReceiver.MOBILE && isCameraUploadByWifi()
            if (stopByNetworkStateChange) {
                for (transfer in cuTransfers) {
                    handleCancelTransfer(transfer = transfer, shouldResetTotalUploads = true)
                }
                coroutineScope?.cancel("Camera Upload by Wifi only but Mobile Network - Cancel Camera Upload")
                sendTransfersInterruptedInfoToBackupCenter()
                endService()
            }
        }
    }

    /**
     * Handle the transfer cancellation here. This includes a listener for callback events
     *
     * @param transfer the [MegaTransfer] to be cancelled
     * @param shouldResetTotalUploads if true, will reset the total upload count
     */
    private suspend fun handleCancelTransfer(
        transfer: MegaTransfer,
        shouldResetTotalUploads: Boolean = false,
    ) {
        runCatching { cancelTransfer(transfer) }
            .onSuccess {
                Timber.d("Transfer cancellation successful")
                if (shouldResetTotalUploads) {
                    // Reset the pending uploads overall count
                    handleResetTotalUploads()
                }
            }
            .onFailure { error ->
                Timber.e("Transfer cancellation error: $error")
            }
    }

    /**
     * Start service work
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.d("Starting CameraUpload service (flags: %d, startId: %d)", flags, startId)
        isServiceRunning = true
        startForegroundNotification()
        initService()

        if (megaApi == null) {
            Timber.d("MegaApi is null, return.")
            endService()
            return START_NOT_STICKY
        }

        if (intent != null && intent.action != null) {
            Timber.d("onStartCommand intent action is %s", intent.action)

            when (intent.action) {
                ACTION_STOP -> {
                    Timber.d("Stop all Camera Uploads Transfers")
                    coroutineScope?.launch {
                        for (transfer in cuTransfers) {
                            handleCancelTransfer(
                                transfer = transfer,
                                shouldResetTotalUploads = true,
                            )
                        }
                        sendTransfersInterruptedInfoToBackupCenter()
                    }
                }
                ACTION_CANCEL_ALL -> {
                    Timber.d("Cancel all Camera Uploads Transfers")
                    handleCancelAllUploadTransfers()
                    sendTransfersCancelledInfoToBackupCenter()
                }
                else -> Unit
            }
            coroutineScope?.cancel("Camera Upload Stop/Cancel Intent Action - Stop Camera Upload")
            endService()
            return START_NOT_STICKY
        }

        if (intent != null) {
            ignoreAttr = intent.getBooleanExtra(EXTRA_IGNORE_ATTR_CHECK, false)
            isPrimaryHandleSynced = intent.getBooleanExtra(EXTRA_PRIMARY_SYNC_SUCCESS, false)
        }

        Timber.d("Start Service - Create Coroutine")
        coroutineScope?.launch { startWorker() }

        return START_NOT_STICKY
    }

    /**
     * Handle the cancellation of all upload transfers here
     */
    private fun handleCancelAllUploadTransfers() {
        coroutineScope?.launch {
            runCatching { cancelAllUploadTransfers() }
                .onSuccess {
                    Timber.d("Cancel all transfers successful")
                    handleResetTotalUploads()
                }
                .onFailure { error ->
                    Timber.e("Cancel all transfers error: $error")
                }
        }
    }

    /**
     * Resets the overall upload count if there are no pending upload transfers
     *
     * The function is marked with the DEPRECATION annotation as resetTotalUploads() is deprecated
     */
    @Suppress("DEPRECATION")
    private suspend fun handleResetTotalUploads() {
        if (!hasPendingUploads()) megaApi?.resetTotalUploads()
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
     * Secondary folders when the user has cancelled all Transfers.
     *
     * In the UI, this is done in the Transfers page where the selects "Cancel all" and
     * confirms the Dialog
     */
    private fun sendTransfersCancelledInfoToBackupCenter() {
        // Update both Primary and Secondary Backup States to ACTIVE
        updatePrimaryFolderBackupState(BackupState.ACTIVE)
        updateSecondaryFolderBackupState(BackupState.ACTIVE)

        // Update both Primary and Secondary Heartbeat Statuses to UP_TO_DATE
        sendPrimaryFolderHeartbeat(HeartbeatStatus.UP_TO_DATE)
        sendSecondaryFolderHeartbeat(HeartbeatStatus.UP_TO_DATE)
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

    private fun registerNetworkTypeChangeReceiver() {
        if (receiver != null) {
            unregisterReceiver(receiver)
        }
        receiver = NetworkTypeChangeReceiver()
        receiver?.setCallback(this)
        registerReceiver(receiver, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))
    }

    /**
     * Function that starts the Camera Uploads functionality
     */
    private suspend fun startWorker() {
        runCatching {
            val state = canRunCameraUploads()
            if (state == StartCameraUploadsState.CAN_RUN_CAMERA_UPLOADS) {
                hideLocalFolderPathNotifications()
                startCameraUploads()
            } else {
                handleFailedStartCameraUploadsState(state)
            }
        }.onFailure { exception ->
            Timber.e("Calling startWorker() failed with exception $exception")
            endService()
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
     * 3. The User is online - [isUserOnline],
     * 4. The Device battery level is above the minimum threshold - [deviceAboveMinimumBatteryLevel],
     * 5. The Camera Uploads local path exists - [hasCameraUploadsLocalPath],
     * 6. The Wi-Fi Constraint is satisfied - [isWifiConstraintSatisfied],
     * 7. The local Primary Folder exists - [hasLocalPrimaryFolder],
     * 8. The local Secondary Folder exists - [hasLocalSecondaryFolder],
     * 9. The user is logged in - [isUserLoggedIn],
     * 10. The user Camera Uploads attribute exists - [hasCameraUploadsUserAttribute],
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
            !isUserOnline() -> StartCameraUploadsState.OFFLINE_USER
            !deviceAboveMinimumBatteryLevel -> StartCameraUploadsState.BELOW_DEVICE_BATTERY_LEVEL
            !hasCameraUploadsLocalPath() -> StartCameraUploadsState.MISSING_LOCAL_PATH
            !isWifiConstraintSatisfied() -> StartCameraUploadsState.UNSATISFIED_WIFI_CONSTRAINT
            !hasLocalPrimaryFolder() -> StartCameraUploadsState.MISSING_LOCAL_PRIMARY_FOLDER
            !hasLocalSecondaryFolder() -> StartCameraUploadsState.MISSING_LOCAL_SECONDARY_FOLDER
            !isUserLoggedIn() -> StartCameraUploadsState.LOGGED_OUT_USER
            !hasCameraUploadsUserAttribute() -> StartCameraUploadsState.MISSING_USER_ATTRIBUTE
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
        when (state) {
            StartCameraUploadsState.MISSING_PREFERENCES,
            StartCameraUploadsState.DISABLED_SYNC,
            StartCameraUploadsState.OFFLINE_USER,
            StartCameraUploadsState.BELOW_DEVICE_BATTERY_LEVEL,
            StartCameraUploadsState.MISSING_LOCAL_PATH,
            StartCameraUploadsState.UNSATISFIED_WIFI_CONSTRAINT,
            -> {
                endService()
            }
            StartCameraUploadsState.MISSING_LOCAL_PRIMARY_FOLDER -> {
                handleLocalPrimaryFolderDisabled()
                endService()
            }
            StartCameraUploadsState.MISSING_LOCAL_SECONDARY_FOLDER -> {
                handleLocalSecondaryFolderDisabled()
                endService()
            }
            StartCameraUploadsState.LOGGED_OUT_USER -> {
                performCompleteFastLogin()
            }
            StartCameraUploadsState.MISSING_USER_ATTRIBUTE -> {
                handleMissingCameraUploadsUserAttribute()
            }
            StartCameraUploadsState.UNESTABLISHED_FOLDERS -> {
                establishFolders()
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
     * Checks if the User is online through [Util.isOnline]
     *
     * @return true if the User is online, and false if otherwise
     */
    private fun isUserOnline(): Boolean =
        Util.isOnline(applicationContext).also {
            if (!it) Timber.w("User is not online")
        }

    /**
     * Checks if the Camera Uploads local path from [localPath] exists
     *
     * @return true if the Camera Uploads local path exists, and false if otherwise
     */
    private suspend fun hasCameraUploadsLocalPath(): Boolean =
        !localPath().isNullOrBlank().also {
            if (!it) Timber.w("Camera Uploads local path is empty")
        }

    /**
     * Checks if the Wi-Fi constraint from the negated [isWifiNotSatisfied] is satisfied
     *
     * @return true if the Wi-Fi constraint is satisfied, and false if otherwise
     */
    private suspend fun isWifiConstraintSatisfied(): Boolean =
        !isWifiNotSatisfied().also {
            if (!it) Timber.w("Cannot start, Wi-Fi required")
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
     * Checks if the user's Camera Uploads attribute exists
     *
     * @return true if the attribute exists, and false if otherwise
     */
    private fun hasCameraUploadsUserAttribute(): Boolean {
        // Prevent checking while the app is alive, because it has been handled by a global event
        Timber.d("ignoreAttr value is $ignoreAttr")
        (ignoreAttr || isPrimaryHandleSynced).also {
            if (!it) Timber.w("The Camera Uploads user attribute is missing. Wait for the user attribute")
            return it
        }
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
        val isPrimaryFolderInRubbish = isNodeInRubbish(getPrimarySyncHandle())
        return !isPrimaryFolderInRubbish || (isPrimaryFolderInRubbish && (getPrimaryFolderHandle() != MegaApiJava.INVALID_HANDLE))
    }

    /**
     * Checks whether the Secondary Folder is established
     *
     * @return true if the Secondary Folder handle is a valid handle, and false if otherwise
     */
    private suspend fun isSecondaryFolderEstablished(): Boolean {
        val isSecondaryFolderInRubbish = isNodeInRubbish(getSecondaryFolderHandle())
        return !isSecondaryFolderInRubbish || (isSecondaryFolderInRubbish && (getSecondaryFolderHandle() != MegaApiJava.INVALID_HANDLE))
    }

    /**
     * When the Primary Folder and Secondary Folder (if enabled) does not exist, this function will establish
     * the folders
     */
    private suspend fun establishFolders() {
        // Check if either Primary or Secondary Folder should be set up
        val shouldSetupFolders =
            !isPrimaryFolderEstablished() || (isSecondaryFolderEnabled() && !isSecondaryFolderEstablished())

        // Setup the Primary Folder if it is missing
        if (!isPrimaryFolderEstablished()) {
            Timber.w("The local primary folder is missing")
            setupPrimaryFolder()
            if (!isSecondaryFolderEnabled()) {
                Timber.d("Waiting for user to login or to check the Camera Uploads attribute")
                return
            }
        }

        // If Secondary Media Uploads is enabled, setup the Secondary Folder if it is missing
        if (isSecondaryFolderEnabled() && !isSecondaryFolderEstablished()) {
            Timber.w("The local secondary folder is missing")
            setupSecondaryFolder()
            Timber.d("Waiting for user to login or to check the Camera Uploads attribute")
            return
        }

        // If either Primary or Secondary Folder needs to be set up, then call
        // the API to setup both Folders for Camera Uploads
        if (shouldSetupFolders) {
            val primaryFolderHandle = getPrimaryFolderHandle()
            val secondaryFolderHandle = getSecondaryFolderHandle()

            Timber.d("Setting Primary Folder handle: $primaryFolderHandle")
            Timber.d("Setting Secondary Folder handle: $secondaryFolderHandle")

            megaApi?.setCameraUploadsFolders(
                primaryFolderHandle,
                secondaryFolderHandle,
                setAttrUserListener,
            )
            Timber.d("Waiting to setup Camera Uploads folders")
        }
    }

    private suspend fun startCameraUploads() {
        showNotification(
            getString(R.string.section_photo_sync),
            getString(R.string.settings_camera_notif_checking_title),
            pendingIntent,
            false
        )
        checkUploadNodes()
    }

    private suspend fun checkUploadNodes() {
        Timber.d("Get Pending Files from Media Store Database")
        val primaryUploadNode = getNodeByHandle(getPrimarySyncHandle())
        if (primaryUploadNode == null) {
            Timber.d("ERROR: Primary Parent Folder is NULL")
            endService()
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
                // Make sure to send inactive heartbeat.
                JobUtil.fireSingleHeartbeat(this)
                // Make sure to re schedule the job
                JobUtil.scheduleCameraUploadJob(this)
                endService()
                FileUtil.purgeDirectory(tempRoot?.let { File(it) })
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
            if (!running) break
            val isSecondary = file.isSecondary
            val parent = (if (isSecondary) secondaryUploadNode else primaryUploadNode) ?: continue

            if (file.type == syncRecordTypeIntMapper(SyncRecordType.TYPE_PHOTO) && !file.isCopyOnly) {
                if (getRemoveGps()) {
                    var newPath = createTempFile(file)
                    // IOException occurs.
                    if (ERROR_CREATE_FILE_IO_ERROR == newPath) continue

                    // Only retry for 60 seconds
                    var counter = 60
                    while (ERROR_NOT_ENOUGH_SPACE == newPath && running && counter != 0) {
                        counter--
                        try {
                            Timber.d("Waiting for disk space to process")
                            delay(1000)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }

                        //show no space notification
                        @Suppress("DEPRECATION")
                        if (megaApi?.numPendingUploads == 0) {
                            Timber.w("Stop service due to out of space issue")
                            endService()
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
                totalToUpload++
                file.nodeHandle?.let {
                    megaApi?.copyNode(
                        getNodeByHandle(it),
                        parent,
                        file.fileName,
                        this
                    )
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
                        megaApi?.startUpload(
                            path, parent, file.fileName, lastModified / 1000,
                            Constants.APP_DATA_CU, false, false, null, this
                        )
                    }
                } else {
                    Timber.d("Local file is unavailable, delete record from database.")
                    path?.let {
                        deleteSyncRecord(it, isSecondary)
                    }
                }
            }
        }

        if (totalToUpload == totalUploaded) {
            if (compressedVideoPending() && !canceled && isCompressorAvailable()) {
                Timber.d("Got pending videos, will start compress.")
                startVideoCompression()
            } else {
                Timber.d("No pending videos, finish.")
                onQueueComplete()
            }
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

    private fun onQueueComplete() {
        Timber.d("Stopping foreground!")
        coroutineScope?.launch { handleResetTotalUploads() }
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
        CameraUploadUtil.disableCameraUploadSettingProcess()
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
        CameraUploadUtil.disableMediaUploadProcess()
        setSecondaryFolderPath(SettingsConstants.INVALID_PATH)
        sendBroadcast(Intent(BroadcastConstants.ACTION_DISABLE_MEDIA_UPLOADS_SETTING))
    }

    /**
     * When the user is not logged in, perform a Complete Fast Login procedure
     */
    private suspend fun performCompleteFastLogin() {
        running = true

        Timber.d("Waiting for the user to complete the Fast Login procedure")

        // Legacy support: isLoggingIn needs to be set in order to inform other parts of the
        // app that a Login Procedure is occurring
        MegaApplication.isLoggingIn = true
        val result = runCatching { completeFastLogin() }
        MegaApplication.isLoggingIn = false

        if (result.isSuccess) {
            Timber.d("Complete Fast Login procedure successful. Get cookies settings after login")
            MegaApplication.getInstance().checkEnabledCookies()
            Timber.d("Start CameraUploadsService")
            startWorker()
        } else {
            Timber.e("Complete Fast Login procedure unsuccessful with error ${result.exceptionOrNull()}")
            endService()
        }
    }

    /**
     * Executes certain behavior if the User's Camera Uploads attribute is missing
     */
    private fun handleMissingCameraUploadsUserAttribute() {
        Timber.d("Try to get Camera Uploads primary target folder from attribute")
        megaApi?.getUserAttribute(
            MegaApiJava.USER_ATTR_CAMERA_UPLOADS_FOLDER,
            getAttrUserListener
        )
        Timber.d("Waiting to check the user attribute")
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
            notification = createNotification(
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
    private suspend fun getSecondaryFolderHandle(): Long =
        getDefaultNodeHandle(getString(R.string.section_secondary_media_uploads))

    /**
     * Sets up the Primary Folder if it is missing
     */
    private suspend fun setupPrimaryFolder() {
        val primaryHandle = getPrimaryFolderHandle().also {
            setPrimarySyncHandle(it)
        }
        // isCreatingPrimary is a flag that prevents creating a duplicate Primary Folder
        if (primaryHandle == MegaApiJava.INVALID_HANDLE && !isCreatingPrimary) {
            Timber.d("isCreatingPrimary is false. Proceed to create the Primary Folder")
            isCreatingPrimary = true
            // Create a folder with name "Camera Uploads" in the root node
            megaApi?.createFolder(
                getString(R.string.section_photo_sync),
                megaApi?.rootNode,
                createFolderListener
            )
        }
    }

    /**
     * Sets up the Secondary Folder if it is missing
     */
    private suspend fun setupSecondaryFolder() {
        val secondaryHandle = getSecondaryFolderHandle().also {
            setSecondarySyncHandle(it)
        }
        // isCreatingSecondary is a flag that prevents creating a duplicate Primary Folder
        if (secondaryHandle == MegaApiJava.INVALID_HANDLE && !isCreatingSecondary) {
            Timber.d("isCreatingSecondary is false. Proceed to create the Secondary Folder")
            isCreatingSecondary = true
            // Create a folder with name "Media Uploads" in the root node
            megaApi?.createFolder(
                getString(R.string.section_secondary_media_uploads),
                megaApi?.rootNode,
                createFolderListener
            )
        }
    }

    private fun initService() {
        registerNetworkTypeChangeReceiver()
        try {
            app = application as MegaApplication
        } catch (ex: Exception) {
            endService()
        }

        val wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(wifiLockMode, "MegaDownloadServiceWifiLock")
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock =
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MegaDownloadServicePowerLock:")
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire()
        }
        if (wifiLock?.isHeld == false) {
            wifiLock?.acquire()
        }

        stopByNetworkStateChange = false
        lastUpdated = 0
        totalUploaded = 0
        totalToUpload = 0
        canceled = false
        isOverQuota = false
        running = true

        megaApi = app?.megaApi
        megaApiFolder = app?.megaApiFolder

        if (megaApi == null) {
            endService()
            return
        }

        cameraServiceIpChangeHandler.start()
        // end new logic
        intent = Intent(this, ManagerActivity::class.java)
        intent?.action = Constants.ACTION_CANCEL_CAM_SYNC
        intent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent?.putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.PENDING_TAB)
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        coroutineScope?.launch {
            tempRoot = "${File(cacheDir, CU_CACHE_FOLDER).absolutePath}${File.separator}"
            val root = tempRoot?.let { File(it) }
            if (root?.exists() == false) {
                root.mkdirs()
            }
            clearSyncRecords()
        }
    }

    private fun endService() {
        Timber.d("Finish Camera upload process.")
        releaseLocks()
        if (isOverQuota) {
            showStorageOverQuotaNotification()
            JobUtil.fireStopCameraUploadJob(this)
        }

        videoCompressor?.stop()
        cuTransfers.clear()
        canceled = true
        running = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        cancelNotification()
        stopSelf()
    }

    private fun cancelNotification() {
        notificationManager?.let {
            Timber.d("Cancelling notification ID is %s", notificationId)
            it.cancel(notificationId)
            return
        }
        Timber.w("No notification to cancel")
    }

    /**
     * Start request
     */
    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {}

    /**
     * Update request
     */
    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {}

    /**
     * Finish request
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish: %s", request.requestString)
        try {
            coroutineScope?.launch {
                requestFinished(request, e)
            }
        } catch (th: Throwable) {
            Timber.e(th)
            th.printStackTrace()
        }
    }

    private suspend fun requestFinished(request: MegaRequest, e: MegaError) {
        when (request.type) {
            MegaRequest.TYPE_PAUSE_TRANSFERS -> {
                Timber.d("PauseTransfer false received")
                if (e.errorCode == MegaError.API_OK) {
                    endService()
                }
            }
            MegaRequest.TYPE_COPY -> {
                if (e.errorCode == MegaError.API_OK) {
                    val node = getNodeByHandle(request.nodeHandle)
                    val fingerPrint = node?.fingerprint
                    val isSecondary = node?.parentHandle == getSecondarySyncHandle()
                    fingerPrint?.let { deleteSyncRecordByFingerprint(it, fingerPrint, isSecondary) }
                    node?.let { onUploadSuccess(it, isSecondary) }
                }
                updateUpload()
            }
        }
    }

    /**
     * Callback when getting Primary folder handle from Primary attributes completes.
     *
     * @param handle      Primary folder handle stored in Primary attributes.
     * @param shouldStart If should start worker for camera upload.
     */
    fun onGetPrimaryFolderAttribute(handle: Long, shouldStart: Boolean) {
        isPrimaryHandleSynced = true
        coroutineScope?.launch {
            if (getPrimarySyncHandle() != handle) {
                setPrimarySyncHandle(handle)
            }
            if (shouldStart) {
                Timber.d("On Get Primary - Start Coroutine")
                startWorker()
            }
        }
    }

    /**
     * Callback when getting MU folder handle from CU attributes completes.
     *
     * @param handle    MU folder handle stored in CU attributes.
     */
    fun onGetSecondaryFolderAttribute(handle: Long) {
        coroutineScope?.launch {
            if (getSecondarySyncHandle() != handle) {
                setSecondarySyncHandle(handle)
            }
            // Start upload now - unlike in onGetPrimaryFolderAttribute where it needs to wait for getting Media Uploads folder handle to complete
            Timber.d("On Get Secondary - Start Coroutine")
            startWorker()
        }
    }

    /**
     * Set attributes for folder
     */
    fun onSetFolderAttribute() {
        Timber.d("On Set Camera Upload Folder - Start Coroutine")
        coroutineScope?.launch { startWorker() }
    }

    /**
     * Create folder
     */
    fun onCreateFolder(isSuccessful: Boolean) {
        if (!isSuccessful) {
            endService()
        }
    }

    /**
     * Temporary error on request
     */
    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.w("onRequestTemporaryError: %s", request.requestString)
    }

    /**
     * Start transfer
     */
    override fun onTransferStart(api: MegaApiJava, transfer: MegaTransfer) {
        cuTransfers.add(transfer)
        LiveEventBus.get(EVENT_TRANSFER_UPDATE, Int::class.java).post(MegaTransfer.TYPE_UPLOAD)
    }

    /**
     * Update transfer
     */
    override fun onTransferUpdate(api: MegaApiJava, transfer: MegaTransfer) {
        transferUpdated(transfer)
    }

    @Synchronized
    private fun transferUpdated(transfer: MegaTransfer) {
        if (canceled) {
            Timber.d("Transfer cancel: %s", transfer.nodeHandle)
            coroutineScope?.launch { handleCancelTransfer(transfer) }
            endService()
            return
        }
        if (isOverQuota) {
            return
        }
        updateProgressNotification()
    }

    /**
     * Temporary error on transfer
     */
    override fun onTransferTemporaryError(
        api: MegaApiJava,
        transfer: MegaTransfer,
        e: MegaError,
    ) {
        Timber.w("onTransferTemporaryError: %s", transfer.nodeHandle)
        if (e.errorCode == MegaError.API_EOVERQUOTA) {
            if (e.value != 0L) Timber.w("TRANSFER OVER QUOTA ERROR: %s", e.errorCode)
            else Timber.w("STORAGE OVER QUOTA ERROR: %s", e.errorCode)
            isOverQuota = true
            endService()
        }
    }

    /**
     * Finish transfer
     */
    override fun onTransferFinish(api: MegaApiJava, transfer: MegaTransfer, e: MegaError) {
        Timber.d(
            "Image sync finished, error code: %d, handle: %d, size: %d",
            e.errorCode,
            transfer.nodeHandle,
            transfer.transferredBytes
        )
        try {
            LiveEventBus.get(EVENT_TRANSFER_UPDATE, Int::class.java)
                .post(MegaTransfer.TYPE_UPLOAD)
            coroutineScope?.launch {
                transferFinished(transfer, e)
            }
        } catch (th: Throwable) {
            Timber.e(th)
            th.printStackTrace()
        }
    }

    private suspend fun transferFinished(transfer: MegaTransfer, e: MegaError) {
        val path = transfer.path
        if (isOverQuota) {
            return
        }

        if (transfer.state == MegaTransfer.STATE_COMPLETED) {
            // TODO Remove in MegaApi refactoring (remove MegaTransferListenerInterface)
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
                node?.let { onUploadSuccess(it, record.isSecondary) }
                val originalFingerprint = record.originFingerprint
                megaApi?.setOriginalFingerprint(node, originalFingerprint, this)
                record.latitude?.let { latitude ->
                    record.longitude?.let { longitude ->
                        megaApi?.setNodeCoordinates(
                            node,
                            latitude.toDouble(),
                            longitude.toDouble(),
                            null
                        )
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
                if (tempRoot?.let { path.startsWith(it) } == true) {
                    val temp = File(path)
                    if (temp.exists()) {
                        temp.delete()
                    }
                }
            }
        } else if (e.errorCode == MegaError.API_EOVERQUOTA) {
            Timber.w("Over quota error: %s", e.errorCode)
            isOverQuota = true
            endService()
        } else {
            Timber.w("Image Sync FAIL: %d___%s", transfer.nodeHandle, e.errorString)
        }
        if (canceled) {
            Timber.w("Image sync cancelled: %s", transfer.nodeHandle)
            endService()
        }
        updateUpload()
    }

    private suspend fun updateUpload() {
        if (!canceled) {
            updateProgressNotification()
        }
        totalUploaded++
        @Suppress("DEPRECATION")
        Timber.d(
            "Total to upload: %d Total uploaded: %d Pending uploads: %d",
            totalToUpload,
            totalUploaded,
            megaApi?.numPendingUploads
        )
        if (totalToUpload == totalUploaded) {
            Timber.d("Photo upload finished, now checking videos")
            if (compressedVideoPending() && !canceled && isCompressorAvailable()) {
                Timber.d("Got pending videos, will start compress")
                startVideoCompression()
            } else {
                Timber.d("No pending videos, finish")
                onQueueComplete()
                sendBroadcast(
                    Intent(BroadcastConstants.ACTION_UPDATE_CU)
                        .putExtra(BroadcastConstants.PROGRESS, 100)
                        .putExtra(BroadcastConstants.PENDING_TRANSFERS, 0)
                )
            }
        }
    }

    /**
     * Transfer data
     */
    override fun onTransferData(api: MegaApiJava, transfer: MegaTransfer, buffer: ByteArray) =
        true

    private fun isCompressorAvailable() = !(videoCompressor?.isRunning ?: false)

    private suspend fun startVideoCompression() {
        val fullList = getVideoSyncRecordsByStatus(SyncStatus.STATUS_TO_COMPRESS)
        @Suppress("DEPRECATION")
        handleResetTotalUploads()
        totalUploaded = 0
        totalToUpload = 0

        videoCompressor = VideoCompressor(this, this, getVideoQuality())
        videoCompressor?.setPendingList(fullList)
        videoCompressor?.setOutputRoot(tempRoot)
        val totalPendingSizeInMB = (videoCompressor?.totalInputSize ?: 0) / (1024 * 1024)
        Timber.d(
            "Total videos count are %d, %d mb to Conversion",
            fullList.size,
            totalPendingSizeInMB
        )

        if (shouldStartVideoCompression(totalPendingSizeInMB)) {
            coroutineScope?.launch {
                Timber.d("Starting compressor")
                videoCompressor?.start()
            }
        } else {
            Timber.d("Compression queue bigger than setting, show notification to user.")
            endService()
            val intent = Intent(this, ManagerActivity::class.java)
            intent.action = Constants.ACTION_SHOW_SETTINGS
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            val title = getString(R.string.title_compression_size_over_limit)
            val size = getChargingOnSizeString()
            val message = getString(
                R.string.message_compression_size_over_limit,
                getString(R.string.label_file_size_mega_byte, size)
            )
            showNotification(title, message, pendingIntent, true)
        }
    }

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
    override fun onInsufficientSpace() {
        Timber.w("Insufficient space for video compression.")
        endService()
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
    @Synchronized
    override fun onCompressUpdateProgress(progress: Int) {
        if (!canceled) {
            val message = getString(R.string.message_compress_video, "$progress%")
            val subText = getString(
                R.string.title_compress_video,
                videoCompressor?.currentFileIndex,
                videoCompressor?.totalCount
            )
            showProgressNotification(progress, pendingIntent, message, subText, "")
        }
    }

    /**
     * Compression successful
     */
    @Synchronized
    override fun onCompressSuccessful(record: SyncRecord) {
        coroutineScope?.launch {
            Timber.d("Compression successfully for file with timestamp: %s", record.timestamp)
            setSyncRecordPendingByPath(record.localPath, record.isSecondary)
        }
    }

    /**
     * Compression not supported
     */
    @Synchronized
    override fun onCompressNotSupported(record: SyncRecord) {
        Timber.d("Compression failed, no support for file with timestamp: %s", record.timestamp)
    }

    /**
     * Compression failed
     */
    @Synchronized
    override fun onCompressFailed(record: SyncRecord) {
        val localPath = record.localPath
        val isSecondary = record.isSecondary
        Timber.w("Compression failed for file with timestamp:  %s", record.timestamp)

        val srcFile = localPath?.let { File(it) }
        if (srcFile != null && srcFile.exists()) {
            try {
                val stat = StatFs(tempRoot)
                val availableFreeSpace = stat.availableBytes.toDouble()
                if (availableFreeSpace > srcFile.length()) {
                    Timber.d("Can not compress but got enough disk space, so should be un-supported format issue")
                    val newPath = record.newPath
                    val temp = newPath?.let { File(it) }
                    coroutineScope?.launch {
                        setSyncRecordPendingByPath(localPath, isSecondary)
                    }
                    if (newPath != null && tempRoot?.let { newPath.startsWith(it) } == true && temp != null && temp.exists()) {
                        temp.delete()
                    }
                } else {
                    // record will remain in DB and will be re-compressed next launch
                }
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        } else {
            coroutineScope?.launch {
                Timber.w("Compressed video not exists, remove from DB")
                localPath?.let {
                    deleteSyncRecordByLocalPath(localPath, isSecondary)
                }
            }
        }
    }

    /**
     * Compression finished
     */
    override fun onCompressFinished(currentIndexString: String) {
        coroutineScope?.launch {
            if (!canceled) {
                Timber.d("Preparing to upload compressed video.")
                val compressedList = getVideoSyncRecordsByStatus(SyncStatus.STATUS_PENDING)
                if (compressedList.isNotEmpty()) {
                    Timber.d("Start to upload %d compressed videos.", compressedList.size)
                    startParallelUpload(compressedList, true)
                } else {
                    onQueueComplete()
                }
            } else {
                Timber.d("Compress finished, but process is canceled.")
            }
        }
    }

    @Synchronized
    private fun updateProgressNotification() {
        // refresh UI every 1 seconds to avoid too much workload on main thread
        val now = System.currentTimeMillis()
        lastUpdated = if (now - lastUpdated > Util.ONTRANSFERUPDATE_REFRESH_MILLIS) {
            now
        } else {
            return
        }

        @Suppress("DEPRECATION") val pendingTransfers = megaApi?.numPendingUploads
        @Suppress("DEPRECATION") val totalTransfers = megaApi?.totalUploads
        @Suppress("DEPRECATION") val totalSizePendingTransfer = megaApi?.totalUploadBytes
        @Suppress("DEPRECATION") val totalSizeTransferred = megaApi?.totalUploadedBytes

        if (pendingTransfers != null && totalTransfers != null && totalSizeTransferred != null && totalSizePendingTransfer != null) {
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
                    totalTransfers - pendingTransfers
                } else {
                    totalTransfers - pendingTransfers + 1
                }

                sendBroadcast(
                    Intent(BroadcastConstants.ACTION_UPDATE_CU)
                        .putExtra(BroadcastConstants.PROGRESS, progressPercent)
                        .putExtra(BroadcastConstants.PENDING_TRANSFERS, pendingTransfers)
                )

                message = if (megaApi?.areTransfersPaused(MegaTransfer.TYPE_UPLOAD) == true) {
                    StringResourcesUtils.getString(
                        R.string.upload_service_notification_paused,
                        inProgress,
                        totalTransfers
                    )
                } else {
                    StringResourcesUtils.getString(
                        R.string.upload_service_notification,
                        inProgress,
                        totalTransfers
                    )
                }
            }

            val info =
                Util.getProgressSize(this, totalSizeTransferred, totalSizePendingTransfer)
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            showProgressNotification(
                progressPercent,
                pendingIntent,
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
    ): Notification? {
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

        builder = NotificationCompat.Builder(this, notificationChannelId)
        builder?.setSmallIcon(R.drawable.ic_stat_camera_sync)
            ?.setOngoing(false)
            ?.setContentTitle(title)
            ?.setStyle(NotificationCompat.BigTextStyle().bigText(content))
            ?.setContentText(content)
            ?.setOnlyAlertOnce(true)
            ?.setAutoCancel(isAutoCancel)
        if (intent != null) {
            builder?.setContentIntent(intent)
        }
        return builder?.build()
    }

    private fun showNotification(
        title: String,
        content: String,
        intent: PendingIntent?,
        isAutoCancel: Boolean,
    ) {
        notification = createNotification(title, content, intent, isAutoCancel)
        notificationManager?.notify(notificationId, notification)
    }

    private fun showProgressNotification(
        progressPercent: Int,
        pendingIntent: PendingIntent?,
        message: String,
        subText: String,
        contentText: String,
    ) {
        notification = null
        builder = NotificationCompat.Builder(this, notificationChannelId)
        builder?.setSmallIcon(R.drawable.ic_stat_camera_sync)
            ?.setProgress(100, progressPercent, false)
            ?.setContentIntent(pendingIntent)
            ?.setOngoing(true)
            ?.setStyle(NotificationCompat.BigTextStyle().bigText(subText))
            ?.setContentTitle(message)
            ?.setContentText(contentText)
            ?.setOnlyAlertOnce(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                notificationChannelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setShowBadge(true)
            channel.setSound(null, null)
            notificationManager?.createNotificationChannel(channel)
            builder?.setSubText(subText)
        } else {
            builder?.setSubText(subText)
        }
        notification = builder?.build()
        notificationManager?.notify(notificationId, notification)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                OVER_QUOTA_NOTIFICATION_CHANNEL_ID,
                notificationChannelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setShowBadge(true)
            channel.setSound(null, null)
            notificationManager?.createNotificationChannel(channel)
            builder.setContentText(contentText)
        } else {
            builder.setContentText(contentText)
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

    private fun releaseLocks() {
        if (wifiLock?.isHeld == true) {
            try {
                wifiLock?.release()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
