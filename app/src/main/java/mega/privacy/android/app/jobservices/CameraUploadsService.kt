package mega.privacy.android.app.jobservices

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import android.os.StatFs
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleService
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import mega.privacy.android.app.AndroidCompletedTransfer
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.VideoCompressor
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.constants.EventConstants.EVENT_TRANSFER_UPDATE
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.domain.usecase.AreAllUploadTransfersPaused
import mega.privacy.android.app.domain.usecase.GetCameraUploadLocalPath
import mega.privacy.android.app.domain.usecase.GetCameraUploadLocalPathSecondary
import mega.privacy.android.app.domain.usecase.GetCameraUploadSelectionQuery
import mega.privacy.android.app.domain.usecase.GetChildrenNode
import mega.privacy.android.app.domain.usecase.GetDefaultNodeHandle
import mega.privacy.android.app.domain.usecase.GetFingerprint
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetNodeFromCloud
import mega.privacy.android.app.domain.usecase.GetParentMegaNode
import mega.privacy.android.app.domain.usecase.GetPrimarySyncHandle
import mega.privacy.android.app.domain.usecase.GetSecondarySyncHandle
import mega.privacy.android.app.domain.usecase.GetSyncFileUploadUris
import mega.privacy.android.app.domain.usecase.IsLocalPrimaryFolderSet
import mega.privacy.android.app.domain.usecase.IsLocalSecondaryFolderSet
import mega.privacy.android.app.domain.usecase.IsWifiNotSatisfied
import mega.privacy.android.app.domain.usecase.SaveSyncRecordsToDB
import mega.privacy.android.app.domain.usecase.SetPrimarySyncHandle
import mega.privacy.android.app.domain.usecase.SetSecondarySyncHandle
import mega.privacy.android.app.globalmanagement.TransfersManagement.Companion.addCompletedTransfer
import mega.privacy.android.app.listeners.CreateFolderListener
import mega.privacy.android.app.listeners.CreateFolderListener.ExtraAction
import mega.privacy.android.app.listeners.GetCameraUploadAttributeListener
import mega.privacy.android.app.listeners.SetAttrUserListener
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
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
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.ImageProcessor
import mega.privacy.android.app.utils.JobUtil
import mega.privacy.android.app.utils.MegaNodeUtil.isNodeInRubbishOrDeleted
import mega.privacy.android.app.utils.PreviewUtils
import mega.privacy.android.app.utils.SDCardUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.conversion.VideoCompressionCallback
import mega.privacy.android.domain.entity.CameraUploadMedia
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.CompressedVideoPending
import mega.privacy.android.domain.usecase.DeleteSyncRecord
import mega.privacy.android.domain.usecase.DeleteSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.GetChargingOnSizeString
import mega.privacy.android.domain.usecase.GetPendingSyncRecords
import mega.privacy.android.domain.usecase.GetRemoveGps
import mega.privacy.android.domain.usecase.GetSyncRecordByPath
import mega.privacy.android.domain.usecase.GetVideoQuality
import mega.privacy.android.domain.usecase.GetVideoSyncRecordsByStatus
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.HasPreferences
import mega.privacy.android.domain.usecase.IsCameraUploadByWifi
import mega.privacy.android.domain.usecase.IsCameraUploadSyncEnabled
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.MediaLocalPathExists
import mega.privacy.android.domain.usecase.SetSecondaryFolderPath
import mega.privacy.android.domain.usecase.SetSyncLocalPath
import mega.privacy.android.domain.usecase.SetSyncRecordPendingByPath
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp
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
import java.util.LinkedList
import java.util.Queue
import javax.inject.Inject
import kotlin.math.max
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
        private const val LOGIN_IN = 12
        private const val SETTING_USER_ATTRIBUTE = 7
        private const val TARGET_FOLDER_NOT_EXIST = 8
        private const val CHECKING_USER_ATTRIBUTE = 9
        private const val SHOULD_RUN_STATE_FAILED = -1
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
     * UpdateCameraUploadTimeStamp
     */
    @Inject
    lateinit var updateTimeStamp: UpdateCameraUploadTimeStamp

    /**
     * GetCameraUploadLocalPath
     */
    @Inject
    lateinit var localPath: GetCameraUploadLocalPath

    /**
     * GetCameraUploadLocalPathSecondary
     */
    @Inject
    lateinit var localPathSecondary: GetCameraUploadLocalPathSecondary

    /**
     * GetCameraUploadSelectionQuery
     */
    @Inject
    lateinit var selectionQuery: GetCameraUploadSelectionQuery

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
     * HasCredentials
     */
    @Inject
    lateinit var hasCredentials: HasCredentials

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
     * GetSyncFileUploadUris
     */
    @Inject
    lateinit var getSyncFileUploadUris: GetSyncFileUploadUris

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
     * MediaLocalPathExists
     */
    @Inject
    lateinit var mediaLocalPathExists: MediaLocalPathExists

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
     * GetFingerprint
     */
    @Inject
    lateinit var getFingerprint: GetFingerprint

    /**
     * GetParentMegaNode
     */
    @Inject
    lateinit var getParentMegaNode: GetParentMegaNode

    /**
     * GetChildrenNode
     */
    @Inject
    lateinit var getChildrenNode: GetChildrenNode

    /**
     * GetNodeFromCloud
     */
    @Inject
    lateinit var getNodeFromCloud: GetNodeFromCloud

    /**
     * SaveSyncRecordsToDB
     */
    @Inject
    lateinit var saveSyncRecordsToDB: SaveSyncRecordsToDB

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
    lateinit var tempDbHandler: DatabaseHandler

    /**
     * AreAllUploadTransfersPaused
     */
    @Inject
    lateinit var areAllUploadTransfersPaused: AreAllUploadTransfersPaused

    /**
     * Coroutine dispatcher for camera upload work
     */
    @IoDispatcher
    @Inject
    lateinit var ioDispatcher: CoroutineDispatcher

    /**
     * Coroutine Scope for camera upload work
     */
    private var coroutineScope: CoroutineScope? = null

    private var app: MegaApplication? = null
    private var megaApi: MegaApiAndroid? = null
    private var megaApiFolder: MegaApiAndroid? = null
    private var receiver: NetworkTypeChangeReceiver? = null
    private var handler: Handler? = null
    private var wifiLock: WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var videoCompressor: VideoCompressor? = null
    private var notification: Notification? = null
    private var notificationManager: NotificationManager? = null
    private var builder: NotificationCompat.Builder? = null
    private var intent: Intent? = null
    private var batteryIntent: Intent? = null
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

    private val pauseReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            @Suppress("DEPRECATION")
            Handler().postDelayed({ updateProgressNotification() }, 1000)
        }
    }

    private val chargingStopReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            coroutineScope?.launch {
                if (isChargingRequired((videoCompressor?.totalInputSize ?: 0) / (1024 * 1024))) {
                    Timber.d("Detected device stops charging.")
                    videoCompressor?.stop()
                }
            }
        }
    }

    private val batteryInfoReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            batteryIntent = intent
            if (isDeviceLowOnBattery(batteryIntent)) {
                coroutineScope?.cancel("Low Battery - Cancel Camera Upload")
                for (transfer in cuTransfers) {
                    megaApi?.cancelTransfer(transfer)
                }
                sendTransfersInterruptedInfoToBackupCenter()
                finish()
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
        registerReceiver(chargingStopReceiver, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))
        registerReceiver(batteryInfoReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(
            pauseReceiver,
            IntentFilter(Constants.BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION)
        )
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
        unregisterReceiver(chargingStopReceiver)
        unregisterReceiver(batteryInfoReceiver)
        unregisterReceiver(pauseReceiver)
        getAttrUserListener = null
        setAttrUserListener = null
        createFolderListener = null
        stopActiveHeartbeat()
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
                    megaApi?.cancelTransfer(transfer, this@CameraUploadsService)
                }
                coroutineScope?.cancel("Camera Upload by Wifi only but Mobile Network - Cancel Camera Upload")
                sendTransfersInterruptedInfoToBackupCenter()
                finish()
            }
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
            finish()
            return START_NOT_STICKY
        }

        if (intent != null && intent.action != null) {
            Timber.d("onStartCommand intent action is %s", intent.action)

            when (intent.action) {
                ACTION_STOP -> {
                    Timber.d("Stop all Camera Uploads Transfers")
                    for (transfer in cuTransfers) {
                        megaApi?.cancelTransfer(transfer, this)
                    }
                    sendTransfersInterruptedInfoToBackupCenter()
                }
                ACTION_CANCEL_ALL -> {
                    Timber.d("Cancel all Camera Uploads Transfers")
                    megaApi?.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this)
                    sendTransfersCancelledInfoToBackupCenter()
                }
                else -> Unit
            }
            coroutineScope?.cancel("Camera Upload Stop/Cancel Intent Action - Stop Camera Upload")
            finish()
            return START_NOT_STICKY
        }

        if (intent != null) {
            ignoreAttr = intent.getBooleanExtra(EXTRA_IGNORE_ATTR_CHECK, false)
        }

        Timber.d("Start Service - Create Coroutine")
        coroutineScope?.launch { startWorker() }

        return START_NOT_STICKY
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

    private suspend fun startWorker() {
        runCatching {
            if (!hasCredentials()) {
                Timber.w("There are no user credentials")
                finish()
                return
            }
            if (!hasPreferences()) {
                Timber.w("Preferences not defined, so not enabled")
                finish()
                return
            }
            if (!isCameraUploadSyncEnabled()) {
                Timber.w("Sync enabled not defined or not enabled")
                finish()
                return
            }
            if (!Util.isOnline(applicationContext)) {
                Timber.w("Not online")
                finish()
                return
            }
            if (isDeviceLowOnBattery(batteryIntent)) {
                finish()
                return
            }
            if (TextUtil.isTextEmpty(localPath())) {
                Timber.w("LocalPath is not defined, so not enabled")
                finish()
                return
            }
            if (isWifiNotSatisfied()) {
                Timber.w("Cannot start, WiFi required")
                finish()
                return
            }
            val result = shouldRun()
            Timber.d("Should run result: %s", result)
            when (result) {
                0 -> startCameraUploads()
                LOGIN_IN, CHECKING_USER_ATTRIBUTE, TARGET_FOLDER_NOT_EXIST, SETTING_USER_ATTRIBUTE ->
                    Timber.d("Wait for login or check user attribute.")
                else -> finish()
            }
        }.onFailure { exception ->
            Timber.e(exception)
            handler?.removeCallbacksAndMessages(null)
            releaseLocks()
            if (isOverQuota) {
                showStorageOverQuotaNotification()
            }
            canceled = true
            running = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            cancelNotification()
            finish()
        }
    }

    private suspend fun startCameraUploads() {
        showNotification(
            getString(R.string.section_photo_sync),
            getString(R.string.settings_camera_notif_checking_title),
            pendingIntent,
            false
        )
        // Start the real uploading process, before is checking settings.
        filesFromMediaStore()
    }

    private fun isFilePathValid(media: CameraUploadMedia, parentPath: String?) =
        media.filePath != null && !parentPath.isNullOrBlank()
                && media.filePath!!.startsWith(parentPath)

    private fun getUploadMediaFromCursor(
        cursor: Cursor,
        dataColumn: Int,
        addedColumn: Int,
        modifiedColumn: Int,
    ): CameraUploadMedia {
        val filePath = cursor.getString(dataColumn)
        val addedTime = cursor.getLong(addedColumn) * 1000
        val modifiedTime = cursor.getLong(modifiedColumn) * 1000
        val timestamp = max(addedTime, modifiedTime)
        val media = CameraUploadMedia(filePath = filePath, timestamp = timestamp)
        Timber.d("Extract from cursor, add time: $addedTime, modify time: $modifiedTime, chosen time: $timestamp")
        return media
    }

    private fun extractMedia(cursor: Cursor, parentPath: String?): Queue<CameraUploadMedia> {
        return LinkedList<CameraUploadMedia>().apply {
            try {
                @Suppress("DEPRECATION")
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val addedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val modifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    getUploadMediaFromCursor(
                        cursor,
                        dataColumn,
                        addedColumn,
                        modifiedColumn
                    ).takeIf {
                        isFilePathValid(it, parentPath)
                    }?.let(this::add)
                }
            } catch (exception: Exception) {
                Timber.e(exception)
            }
        }
    }

    private fun buildMediaQueue(
        uri: Uri,
        parentPath: String?,
        pageSize: Int,
        selectionQuery: String?,
    ): Queue<CameraUploadMedia> =
        createMediaCursor(parentPath, selectionQuery, pageSize, uri)?.let {
            Timber.d("Extract ${it.count} Media from Cursor")
            extractMedia(it, parentPath)
        } ?: LinkedList<CameraUploadMedia>().also {
            Timber.d("Extract 0 Media - Cursor is NULL")
        }

    private fun createMediaCursor(
        parentPath: String?,
        selectionQuery: String?,
        pageSize: Int,
        uri: Uri,
    ): Cursor? {
        val projection = getProjection()
        val mediaOrder = MediaStore.MediaColumns.DATE_MODIFIED + " ASC "
        return if (shouldPageCursor(parentPath)) {
            mediaOrder.getPagedMediaCursor(selectionQuery, pageSize, uri, projection)
        } else {
            app?.contentResolver?.query(uri, projection, selectionQuery, null, mediaOrder)
        }
    }

    @Suppress("DEPRECATION")
    private fun getProjection() = arrayOf(
        MediaStore.MediaColumns.DATA,
        MediaStore.MediaColumns.DATE_ADDED,
        MediaStore.MediaColumns.DATE_MODIFIED
    )

    /**
     *  Only paging for files in internal storage
     *  Files on SD card usually have the same timestamp (the time when the SD is loaded)
     */
    private fun shouldPageCursor(parentPath: String?) =
        !SDCardUtils.isLocalFolderOnSDCard(this, parentPath)

    private fun String.getPagedMediaCursor(
        selectionQuery: String?,
        pageSize: Int,
        uri: Uri,
        projection: Array<String>,
    ): Cursor? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val args = Bundle()
            args.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, this)
            args.putString(ContentResolver.QUERY_ARG_OFFSET, "0")
            args.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selectionQuery)
            args.putString(ContentResolver.QUERY_ARG_SQL_LIMIT, pageSize.toString())
            app?.contentResolver?.query(uri, projection, args, null)
        } else {
            val mediaOrderPreR = "$this LIMIT 0,$pageSize"
            app?.contentResolver?.query(uri, projection, selectionQuery, null, mediaOrderPreR)
        }
    }

    private suspend fun filesFromMediaStore() {
        Timber.d("Get Pending Files from Media Store Database")
        val primaryUploadNode = getNodeByHandle(getPrimarySyncHandle())
        if (primaryUploadNode == null) {
            Timber.d("ERROR: Primary Parent Folder is NULL")
            finish()
            return
        }
        val secondaryEnabled = isSecondaryFolderEnabled()
        val secondaryUploadNode = if (secondaryEnabled) {
            Timber.d("Secondary Upload is ENABLED")
            getNodeByHandle(getSecondarySyncHandle())
        } else {
            null
        }

        val primaryPhotos: Queue<CameraUploadMedia> = LinkedList()
        val primaryVideos: Queue<CameraUploadMedia> = LinkedList()
        val secondaryPhotos: Queue<CameraUploadMedia> = LinkedList()
        val secondaryVideos: Queue<CameraUploadMedia> = LinkedList()

        val pageSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) 1000 else 400
        val pageSizeVideo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) 50 else 10
        val uris = getSyncFileUploadUris()

        for (uri in uris) {
            if (uri == MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                || uri == MediaStore.Images.Media.INTERNAL_CONTENT_URI
            ) {
                // Photos
                primaryPhotos.addAll(
                    buildMediaQueue(
                        uri,
                        localPath(),
                        pageSize,
                        selectionQuery(SyncTimeStamp.PRIMARY_PHOTO)
                    )
                )
                if (secondaryEnabled) {
                    secondaryPhotos.addAll(
                        buildMediaQueue(
                            uri,
                            localPathSecondary(),
                            pageSize,
                            selectionQuery(SyncTimeStamp.SECONDARY_PHOTO)
                        )
                    )
                }
            } else if (uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                || uri == MediaStore.Video.Media.INTERNAL_CONTENT_URI
            ) {
                // Videos
                primaryVideos.addAll(
                    buildMediaQueue(
                        uri,
                        localPath(),
                        pageSizeVideo,
                        selectionQuery(SyncTimeStamp.PRIMARY_VIDEO)
                    )
                )
                if (secondaryEnabled) {
                    secondaryVideos.addAll(
                        buildMediaQueue(
                            uri,
                            localPathSecondary(),
                            pageSizeVideo,
                            selectionQuery(SyncTimeStamp.SECONDARY_VIDEO)
                        )
                    )
                }
            }
        }

        totalUploaded = 0
        prepareUpload(
            primaryPhotos,
            primaryVideos,
            secondaryPhotos,
            secondaryVideos,
            secondaryEnabled,
            primaryUploadNode,
            secondaryUploadNode
        )
    }

    private suspend fun prepareUpload(
        primaryPhotos: Queue<CameraUploadMedia>,
        primaryVideos: Queue<CameraUploadMedia>,
        secondaryPhotos: Queue<CameraUploadMedia>,
        secondaryVideos: Queue<CameraUploadMedia>,
        secondaryEnabled: Boolean,
        primaryUploadNode: MegaNode?,
        secondaryUploadNode: MegaNode?,
    ) {
        Timber.d(
            "\nPrimary photo count from media store database: %d\nSecondary photo count from media store database: %d\nPrimary video count from media store database: %d\nSecondary video count from media store database: %d",
            primaryPhotos.size,
            secondaryPhotos.size,
            primaryVideos.size,
            secondaryVideos.size
        )

        val pendingUploadsList =
            getPendingList(primaryPhotos, isSecondary = false, isVideo = false)
        Timber.d("Primary photo pending list size: %s", pendingUploadsList.size)
        saveSyncRecordsToDB(pendingUploadsList, primaryUploadNode, secondaryUploadNode, tempRoot)

        val pendingVideoUploadsList = getPendingList(
            primaryVideos,
            isSecondary = false,
            isVideo = true
        )
        Timber.d("Primary video pending list size: %s", pendingVideoUploadsList.size)
        saveSyncRecordsToDB(
            pendingVideoUploadsList,
            primaryUploadNode,
            secondaryUploadNode,
            tempRoot
        )

        if (secondaryEnabled) {
            val pendingUploadsListSecondary = getPendingList(
                secondaryPhotos,
                isSecondary = true,
                isVideo = false
            )
            Timber.d("Secondary photo pending list size: %s", pendingUploadsListSecondary.size)
            saveSyncRecordsToDB(
                pendingUploadsListSecondary,
                primaryUploadNode,
                secondaryUploadNode,
                tempRoot
            )

            val pendingVideoUploadsListSecondary = getPendingList(
                secondaryVideos,
                isSecondary = true,
                isVideo = true
            )
            Timber.d(
                "Secondary video pending list size: %s",
                pendingVideoUploadsListSecondary.size
            )
            saveSyncRecordsToDB(
                pendingVideoUploadsListSecondary,
                primaryUploadNode,
                secondaryUploadNode, tempRoot
            )
        }
        yield()

        // Need to maintain timestamp for better performance
        updateTimeStamp(null, SyncTimeStamp.PRIMARY_PHOTO)
        updateTimeStamp(null, SyncTimeStamp.PRIMARY_VIDEO)
        updateTimeStamp(null, SyncTimeStamp.SECONDARY_PHOTO)
        updateTimeStamp(null, SyncTimeStamp.SECONDARY_VIDEO)

        // Reset backup state as active.
        updatePrimaryFolderBackupState(BackupState.ACTIVE)
        updateSecondaryFolderBackupState(BackupState.ACTIVE)

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
                finish()
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

            if (file.type == SyncRecordType.TYPE_PHOTO.value && !file.isCopyOnly) {
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
                            finish()
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
            if (isCompressedVideo || file.type == SyncRecordType.TYPE_PHOTO.value || file.type == SyncRecordType.TYPE_VIDEO.value && shouldCompressVideo()) {
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
        @Suppress("DEPRECATION")
        if ((megaApi?.numPendingUploads ?: 1) <= 0) {
            megaApi?.resetTotalUploads()
        }
        totalUploaded = 0
        totalToUpload = 0
        reportUploadFinish()
        stopActiveHeartbeat()
        finish()
    }

    private suspend fun getPendingList(
        mediaList: Queue<CameraUploadMedia>,
        isSecondary: Boolean,
        isVideo: Boolean,
    ): List<SyncRecord> {
        Timber.d(
            "Get pending list, is secondary upload: %s, is video: %s",
            isSecondary,
            isVideo
        )
        val pendingList = mutableListOf<SyncRecord>()
        val parentNodeHandle = if (isSecondary) getSecondarySyncHandle() else getPrimarySyncHandle()
        val parentNode = getNodeByHandle(parentNodeHandle)
        Timber.d("Upload to parent node which handle is: %s", parentNodeHandle)
        val type =
            if (isVideo) SyncRecordType.TYPE_VIDEO.value else SyncRecordType.TYPE_PHOTO.value

        while (mediaList.size > 0) {
            yield()
            val media = mediaList.poll() ?: continue

            if (media.filePath?.let { mediaLocalPathExists(it, isSecondary) } == true) {
                Timber.d("Skip media with timestamp: %s", media.timestamp)
                continue
            }

            // Source file
            val sourceFile = media.filePath?.let { File(it) }
            val localFingerPrint = media.filePath?.let { getFingerprint(it) }
            var nodeExists: MegaNode? = null
            try {
                nodeExists = parentNode?.let { node ->
                    localFingerPrint?.let { fingerprint ->
                        getNodeFromCloud(fingerprint, node)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            if (nodeExists == null) {
                Timber.d("Possible node with same fingerprint is null.")
                val gpsData = sourceFile?.let { getGPSCoordinates(it.absolutePath, isVideo) }
                val record = SyncRecord(
                    0,
                    sourceFile?.absolutePath,
                    null,
                    localFingerPrint,
                    null,
                    media.timestamp,
                    sourceFile?.name,
                    gpsData?.get(1),
                    gpsData?.get(0),
                    if (shouldCompressVideo() && type == SyncRecordType.TYPE_VIDEO.value) SyncStatus.STATUS_TO_COMPRESS.value else SyncStatus.STATUS_PENDING.value,
                    type,
                    null,
                    false,
                    isSecondary
                )
                Timber.d(
                    "Add local file with timestamp: %d to pending list, for upload.",
                    record.timestamp
                )
                pendingList.add(record)
            } else {
                Timber.d(
                    "Possible node with same fingerprint which handle is: %s",
                    nodeExists.handle
                )
                if (getParentMegaNode(nodeExists)?.handle != parentNodeHandle) {
                    val record = SyncRecord(
                        0,
                        media.filePath,
                        null,
                        nodeExists.originalFingerprint,
                        nodeExists.fingerprint,
                        media.timestamp,
                        sourceFile?.name,
                        nodeExists.longitude.toFloat(),
                        nodeExists.latitude.toFloat(),
                        SyncStatus.STATUS_PENDING.value,
                        type,
                        nodeExists.handle,
                        true,
                        isSecondary
                    )
                    Timber.d(
                        "Add local file with handle: %d to pending list, for copy.",
                        record.nodeHandle
                    )
                    pendingList.add(record)
                } else {
                    if (isVideo) {
                        updateTimeStamp(media.timestamp, SyncTimeStamp.PRIMARY_VIDEO)
                        updateTimeStamp(media.timestamp, SyncTimeStamp.SECONDARY_VIDEO)
                    } else {
                        updateTimeStamp(media.timestamp, SyncTimeStamp.PRIMARY_PHOTO)
                        updateTimeStamp(media.timestamp, SyncTimeStamp.SECONDARY_PHOTO)
                    }
                }
            }
        }
        return pendingList
    }

    private suspend fun shouldRun(): Int {
        if (!isLocalPrimaryFolderSet()) {
            localFolderUnavailableNotification(
                R.string.camera_notif_primary_local_unavailable,
                LOCAL_FOLDER_REMINDER_PRIMARY
            )
            CameraUploadUtil.disableCameraUploadSettingProcess()
            setSyncLocalPath(Constants.INVALID_NON_NULL_VALUE)
            setSecondaryFolderPath(Constants.INVALID_NON_NULL_VALUE)
            // refresh settings fragment UI
            sendBroadcast(Intent(BroadcastConstants.ACTION_REFRESH_CAMERA_UPLOADS_SETTING))
            return SHOULD_RUN_STATE_FAILED
        } else {
            notificationManager?.cancel(LOCAL_FOLDER_REMINDER_PRIMARY)
        }

        if (!isLocalSecondaryFolderSet()) {
            localFolderUnavailableNotification(
                R.string.camera_notif_secondary_local_unavailable,
                LOCAL_FOLDER_REMINDER_SECONDARY
            )
            // disable media upload only
            CameraUploadUtil.disableMediaUploadProcess()
            setSecondaryFolderPath(SettingsConstants.INVALID_PATH)
            sendBroadcast(Intent(BroadcastConstants.ACTION_DISABLE_MEDIA_UPLOADS_SETTING))
            return SHOULD_RUN_STATE_FAILED
        } else {
            notificationManager?.cancel(LOCAL_FOLDER_REMINDER_SECONDARY)
        }

        if (megaApi?.rootNode == null && !MegaApplication.isLoggingIn) {
            Timber.w("RootNode = null")
            running = true
            MegaApplication.isLoggingIn = true
            // TODO Remove DbHandler and Refactor in MegaApi dependency removal with use cases:
            // GetSession, FastLogin, InitMegaChat (already provided)
            megaApi?.fastLogin(tempDbHandler.credentials?.session, this)
            ChatUtil.initMegaChatApi(tempDbHandler.credentials?.session)
            return LOGIN_IN
        }

        // Prevent checking while app alive because it has been handled by global event
        Timber.d("ignoreAttr: %s", ignoreAttr)
        if (!ignoreAttr && !isPrimaryHandleSynced) {
            Timber.d("Try to get Camera Uploads primary target folder from CU attribute.")
            megaApi?.getUserAttribute(
                MegaApiJava.USER_ATTR_CAMERA_UPLOADS_FOLDER,
                getAttrUserListener
            )
            return CHECKING_USER_ATTRIBUTE
        }
        return checkTargetFolders()
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
     * Before CU process launches, check CU and MU folder.
     *
     * @return 0, if both folders are alright, CU will start normally.
     * TARGET_FOLDER_NOT_EXIST, CU or MU folder is deleted, will create new folder. CU process will launch after the creation completes.
     * SETTING_USER_ATTRIBUTE, set CU attributes with valid handle. CU process will launch after the setting completes.
     */
    private suspend fun checkTargetFolders(): Int {
        var primaryToSet = MegaApiJava.INVALID_HANDLE
        // If CU folder in local setting is deleted, then need to reset.
        val needToSetPrimary = isNodeInRubbishOrDeleted(getPrimarySyncHandle())
        val secondaryEnabled = isSecondaryFolderEnabled()

        if (needToSetPrimary) {
            // Try to find a folder which name is "Camera Uploads" from root.
            val primaryHandle = getDefaultNodeHandle(getString(R.string.section_photo_sync)).also {
                setPrimarySyncHandle(it)
            }
            // Cannot find a folder with the name, create one.
            if (primaryHandle == MegaApiJava.INVALID_HANDLE) {
                // Flag, prevent to create duplicate folder.
                if (!isCreatingPrimary) {
                    Timber.d("Must create CU folder.")
                    isCreatingPrimary = true
                    // Create a folder with name "Camera Uploads" at root.
                    megaApi?.createFolder(
                        getString(R.string.section_photo_sync),
                        megaApi?.rootNode,
                        createFolderListener
                    )
                }
                if (!secondaryEnabled) {
                    return TARGET_FOLDER_NOT_EXIST
                }
            } else {
                // Found, prepare to set the folder as CU folder.
                primaryToSet = primaryHandle
            }
        }

        var secondaryToSet = MegaApiJava.INVALID_HANDLE
        var needToSetSecondary = false
        // Only check MU folder when secondary upload is enabled.
        if (secondaryEnabled) {
            Timber.d("Secondary uploads are enabled.")
            // If MU folder in local setting is deleted, then need to reset.
            needToSetSecondary = isNodeInRubbishOrDeleted(getSecondarySyncHandle())
            if (needToSetSecondary) {
                // Try to find a folder which name is "Media Uploads" from root.
                val secondaryHandle =
                    getDefaultNodeHandle(getString(R.string.section_secondary_media_uploads)).also {
                        setSecondarySyncHandle(it)
                    }
                // Cannot find a folder with the name, create one.
                if (secondaryHandle == MegaApiJava.INVALID_HANDLE) {
                    // Flag, prevent to create duplicate folder.
                    if (!isCreatingSecondary) {
                        Timber.d("Must create MU folder.")
                        isCreatingSecondary = true
                        // Create a folder with name "Media Uploads" at root.
                        megaApi?.createFolder(
                            getString(R.string.section_secondary_media_uploads),
                            megaApi?.rootNode,
                            createFolderListener
                        )
                    }
                    return TARGET_FOLDER_NOT_EXIST
                } else {
                    // Found, prepare to set the folder as MU folder.
                    secondaryToSet = secondaryHandle
                }
            }
        } else {
            Timber.d("Secondary NOT Enabled")
        }

        if (needToSetPrimary || needToSetSecondary) {
            Timber.d("Set CU attribute: %d %d", primaryToSet, secondaryToSet)
            megaApi?.setCameraUploadsFolders(primaryToSet, secondaryToSet, setAttrUserListener)
            return SETTING_USER_ATTRIBUTE
        }
        return 0
    }

    private fun initService() {
        registerNetworkTypeChangeReceiver()
        try {
            app = application as MegaApplication
        } catch (ex: Exception) {
            finish()
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
        @Suppress("DEPRECATION")
        handler = Handler()

        megaApi = app?.megaApi
        megaApiFolder = app?.megaApiFolder

        if (megaApi == null) {
            finish()
            return
        }

        val previousIP = app?.localIpAddress
        // the new logic implemented in NetworkStateReceiver
        val currentIP = Util.getLocalIpAddress(applicationContext)
        app?.localIpAddress = currentIP
        if (currentIP != null && currentIP.isNotEmpty() && currentIP.compareTo("127.0.0.1") != 0) {
            if (previousIP == null || currentIP.compareTo(previousIP) != 0) {
                Timber.d("Reconnecting...")
                megaApi?.reconnect()
            } else {
                Timber.d("Retrying pending connections...")
                megaApi?.retryPendingConnections()
            }
        }
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

    private fun finish() {
        Timber.d("Finish Camera upload process.")
        handler?.removeCallbacksAndMessages(null)

        running = false
        cancel()
    }

    private fun cancel() {
        releaseLocks()
        if (isOverQuota) {
            showStorageOverQuotaNotification()
            JobUtil.fireStopCameraUploadJob(this)
        }

        videoCompressor?.stop()
        cuTransfers.clear()
        canceled = true
        running = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
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
        if (request.type == MegaRequest.TYPE_LOGIN) {
            if (e.errorCode == MegaError.API_OK) {
                Timber.d("Logged in. Setting account auth token for folder links.")
                megaApiFolder?.accountAuth = megaApi?.accountAuth
                Timber.d("Fast login OK, Calling fetchNodes from CameraSyncService")
                megaApi?.fetchNodes(this)

                // Get cookies settings after login.
                MegaApplication.getInstance().checkEnabledCookies()
            } else {
                Timber.d("ERROR: %s", e.errorString)
                MegaApplication.isLoggingIn = false
                finish()
            }
        } else if (request.type == MegaRequest.TYPE_FETCH_NODES) {
            if (e.errorCode == MegaError.API_OK) {
                Timber.d("fetch nodes ok")
                MegaApplication.isLoggingIn = false
                Timber.d("Start service here MegaRequest.TYPE_FETCH_NODES")
                startWorker()
            } else {
                Timber.d("ERROR: %s", e.errorString)
                MegaApplication.isLoggingIn = false
                finish()
            }
        } else if (request.type == MegaRequest.TYPE_CANCEL_TRANSFER) {
            Timber.d("Cancel transfer received")
            if (e.errorCode == MegaError.API_OK) {
                @Suppress("DEPRECATION")
                Handler().postDelayed({
                    if ((megaApi?.numPendingUploads ?: 1) <= 0) {
                        megaApi?.resetTotalUploads()
                    }
                }, 200)
            } else {
                finish()
            }
        } else if (request.type == MegaRequest.TYPE_CANCEL_TRANSFERS) {
            @Suppress("DEPRECATION")
            if (e.errorCode == MegaError.API_OK && (megaApi?.numPendingUploads ?: 1) <= 0) {
                megaApi?.resetTotalUploads()
            }
        } else if (request.type == MegaRequest.TYPE_PAUSE_TRANSFERS) {
            Timber.d("PauseTransfer false received")
            if (e.errorCode == MegaError.API_OK) {
                finish()
            }
        } else if (request.type == MegaRequest.TYPE_COPY) {
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

    /**
     * Callback when getting CU folder handle from CU attributes completes.
     *
     * @param handle      CU folder handle stored in CU attributes.
     * @param errorCode   Used to get error code to see if the request is successful.
     * @param shouldStart If should start CU process.
     */
    fun onGetPrimaryFolderAttribute(handle: Long, errorCode: Int, shouldStart: Boolean) {
        if (errorCode == MegaError.API_OK || errorCode == MegaError.API_ENOENT) {
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
        } else {
            Timber.w("On Get Primary - Failed")
            finish()
        }
    }

    /**
     * Callback when getting MU folder handle from CU attributes completes.
     *
     * @param handle    MU folder handle stored in CU attributes.
     * @param errorCode Used to get error code to see if the request is successful.
     */
    fun onGetSecondaryFolderAttribute(handle: Long, errorCode: Int) {
        if (errorCode == MegaError.API_OK || errorCode == MegaError.API_ENOENT) {
            coroutineScope?.launch {
                if (getSecondarySyncHandle() != handle) {
                    setSecondarySyncHandle(handle)
                }
                // Start upload now - unlike in onGetPrimaryFolderAttribute where it needs to wait for getting Media Uploads folder handle to complete
                Timber.d("On Get Secondary - Start Coroutine")
                startWorker()
            }
        } else {
            Timber.w("On Get Secondary - Failed")
            finish()
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
            finish()
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
            megaApi?.cancelTransfer(transfer)
            cancel()
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
            cancel()
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
            cancel()
        } else {
            Timber.w("Image Sync FAIL: %d___%s", transfer.nodeHandle, e.errorString)
        }
        if (canceled) {
            Timber.w("Image sync cancelled: %s", transfer.nodeHandle)
            cancel()
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
        if ((megaApi?.numPendingUploads ?: 1) <= 0) {
            megaApi?.resetTotalUploads()
        }
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
            finish()
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
        finish()
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
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder?.setSubText(subText)
        } else {
            builder?.setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                ?.setContentInfo(subText)
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
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setContentText(contentText)
        } else {
            builder.setContentInfo(contentText).color =
                ContextCompat.getColor(this, R.color.red_600_red_300)
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

    private fun getGPSCoordinates(filePath: String, isVideo: Boolean): FloatArray {
        val output = FloatArray(2)
        try {
            if (isVideo) {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(filePath)

                val location =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
                if (location != null) {
                    var secondTry = false
                    try {
                        val mid = location.length / 2 // get the middle of the String
                        val parts = arrayOf(location.substring(0, mid), location.substring(mid))
                        output[0] = parts[0].toFloat()
                        output[1] = parts[1].toFloat()
                    } catch (ex: Exception) {
                        secondTry = true
                        Timber.e(ex)
                    }

                    if (secondTry) {
                        try {
                            val latString = location.substring(0, 7)
                            val lonString = location.substring(8, 17)
                            output[0] = latString.toFloat()
                            output[1] = lonString.toFloat()
                        } catch (ex: Exception) {
                            Timber.e(ex)
                        }
                    }
                } else {
                    Timber.w("No location info")
                }
                retriever.release()
            } else {
                val exif = ExifInterface(filePath)
                val latLong = exif.latLong
                if (latLong != null) {
                    output[0] = latLong[0].toFloat()
                    output[1] = latLong[1].toFloat()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Timber.e(e)
        }
        return output
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

    private fun isDeviceLowOnBattery(intent: Intent?): Boolean {
        if (intent == null) {
            return false
        }
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        Timber.d("Device battery level is %s", level)
        return level <= LOW_BATTERY_LEVEL && !Util.isCharging(this@CameraUploadsService)
    }
}
