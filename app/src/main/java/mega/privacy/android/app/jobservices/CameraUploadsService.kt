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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.app.AndroidCompletedTransfer
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.VideoCompressor
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.constants.EventConstants.EVENT_TRANSFER_UPDATE
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.di.ApplicationScope
import mega.privacy.android.app.domain.usecase.GetCameraUploadLocalPath
import mega.privacy.android.app.domain.usecase.GetCameraUploadLocalPathSecondary
import mega.privacy.android.app.domain.usecase.GetCameraUploadSelectionQuery
import mega.privacy.android.app.domain.usecase.GetSyncFileUploadUris
import mega.privacy.android.app.domain.usecase.IsLocalPrimaryFolderSet
import mega.privacy.android.app.domain.usecase.IsLocalSecondaryFolderSet
import mega.privacy.android.app.domain.usecase.IsWifiNotSatisfied
import mega.privacy.android.app.globalmanagement.TransfersManagement.Companion.addCompletedTransfer
import mega.privacy.android.app.listeners.CreateFolderListener
import mega.privacy.android.app.listeners.CreateFolderListener.ExtraAction
import mega.privacy.android.app.listeners.GetCameraUploadAttributeListener
import mega.privacy.android.app.listeners.SetAttrUserListener
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.receivers.NetworkTypeChangeReceiver
import mega.privacy.android.app.receivers.NetworkTypeChangeReceiver.OnNetworkTypeChangeCallback
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.isActive
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.onUploadSuccess
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.reportUploadFinish
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.reportUploadInterrupted
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.startActiveHeartbeat
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.stopActiveHeartbeat
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.updatePrimaryBackupState
import mega.privacy.android.app.sync.camerauploads.CameraUploadSyncManager.updateSecondaryBackupState
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
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.CompressedVideoPending
import mega.privacy.android.domain.usecase.DeleteSyncRecord
import mega.privacy.android.domain.usecase.DeleteSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.FileNameExists
import mega.privacy.android.domain.usecase.GetChargingOnSizeString
import mega.privacy.android.domain.usecase.GetPendingSyncRecords
import mega.privacy.android.domain.usecase.GetRemoveGps
import mega.privacy.android.domain.usecase.GetSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.GetSyncRecordByPath
import mega.privacy.android.domain.usecase.GetVideoQuality
import mega.privacy.android.domain.usecase.GetVideoSyncRecordsByStatus
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.HasPreferences
import mega.privacy.android.domain.usecase.IsCameraUploadByWifi
import mega.privacy.android.domain.usecase.IsCameraUploadSyncEnabled
import mega.privacy.android.domain.usecase.IsChargingRequired
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.KeepFileNames
import mega.privacy.android.domain.usecase.MediaLocalPathExists
import mega.privacy.android.domain.usecase.SaveSyncRecord
import mega.privacy.android.domain.usecase.SetSecondaryFolderPath
import mega.privacy.android.domain.usecase.SetSyncLocalPath
import mega.privacy.android.domain.usecase.SetSyncRecordPendingByPath
import mega.privacy.android.domain.usecase.ShouldCompressVideo
import mega.privacy.android.domain.usecase.UpdateCameraUploadTimeStamp
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
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
         * Cancel Camera Sync
         */
        const val ACTION_CANCEL = "CANCEL_SYNC"

        /**
         * Stop Camera Sync
         */
        const val ACTION_STOP = "STOP_SYNC"

        /**
         * Cancel all actions
         */
        const val ACTION_CANCEL_ALL = "CANCEL_ALL"

        /**
         * Camera Upload Logout
         */
        const val ACTION_LOGOUT = "LOGOUT_SYNC"

        /**
         * New photo or video folder
         */
        const val ACTION_LIST_PHOTOS_VIDEOS_NEW_FOLDER = "PHOTOS_VIDEOS_NEW_FOLDER"

        /**
         * Ignore extra attributes
         */
        const val EXTRA_IGNORE_ATTR_CHECK = "EXTRA_IGNORE_ATTR_CHECK"

        /**
         * Camera Uploads Cache Folder
         */
        const val CU_CACHE_FOLDER = "cu"

        private var PAGE_SIZE = 200
        private var PAGE_SIZE_VIDEO = 10
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
     * SaveSyncRecord
     */
    @Inject
    lateinit var saveSyncRecord: SaveSyncRecord

    /**
     * FileNameExists
     */
    @Inject
    lateinit var fileNameExists: FileNameExists

    /**
     * KeepFileNames
     */
    @Inject
    lateinit var keepFileNames: KeepFileNames

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
     * GetSyncRecordByFingerprint
     */
    @Inject
    lateinit var getSyncRecordByFingerprint: GetSyncRecordByFingerprint

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
     * DatabaseHandler
     */
    @Inject
    lateinit var tempDbHandler: DatabaseHandler

    /**
     * Coroutine scope for service
     */
    @ApplicationScope
    @Inject
    lateinit var sharingScope: CoroutineScope

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
    private var stopped = false
    private var totalUploaded = 0
    private var totalToUpload = 0
    private var lastUpdated: Long = 0
    private var cameraUploadHandle = MegaApiJava.INVALID_HANDLE
    private var secondaryUploadHandle = MegaApiJava.INVALID_HANDLE
    private var cameraUploadNode: MegaNode? = null
    private var secondaryUploadNode: MegaNode? = null
    private var getAttrUserListener: GetCameraUploadAttributeListener? = null
    private var setAttrUserListener: SetAttrUserListener? = null
    private var createFolderListener: CreateFolderListener? = null
    private val cuTransfers: MutableList<MegaTransfer> = mutableListOf()

    private class Media {
        var filePath: String? = null
        var timestamp: Long = 0
    }

    private val pauseReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            @Suppress("DEPRECATION")
            Handler().postDelayed({ updateProgressNotification() }, 1000)
        }
    }

    private val chargingStopReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            sharingScope.launch {
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
                Timber.d("Device is on low battery.")
                stopped = true
                for (transfer in cuTransfers) {
                    megaApi?.cancelTransfer(transfer)
                }
                finish()
            }
        }
    }

    /**
     * Service starts
     */
    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        registerReceiver(chargingStopReceiver, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))
        registerReceiver(batteryInfoReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(pauseReceiver,
            IntentFilter(Constants.BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION))
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

        // Camera Upload process is running, but interrupted.
        if (isActive()) {
            // Update backups' state.
            updatePrimaryBackupState(CameraUploadSyncManager.State.CU_SYNC_STATE_TEMPORARY_DISABLED)
            updateSecondaryBackupState(CameraUploadSyncManager.State.CU_SYNC_STATE_TEMPORARY_DISABLED)
            // Send failed heartbeat.
            reportUploadInterrupted()
        }
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
        sharingScope.launch {
            stopByNetworkStateChange =
                type == NetworkTypeChangeReceiver.MOBILE && isCameraUploadByWifi()
            if (stopByNetworkStateChange) {
                for (transfer in cuTransfers) {
                    megaApi?.cancelTransfer(transfer, this@CameraUploadsService)
                }
                stopped = true
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
            if (intent.action == ACTION_CANCEL || intent.action == ACTION_STOP || intent.action == ACTION_LIST_PHOTOS_VIDEOS_NEW_FOLDER) {
                Timber.d("Cancel all CameraUpload transfers.")
                for (transfer in cuTransfers) {
                    megaApi?.cancelTransfer(transfer, this)
                }
            } else if (ACTION_CANCEL_ALL == intent.action || intent.action == ACTION_LOGOUT) {
                Timber.d("Cancel all transfers.")
                megaApi?.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this)
            }
            stopped = true
            finish()
            return START_NOT_STICKY
        }

        if (intent != null) {
            ignoreAttr = intent.getBooleanExtra(EXTRA_IGNORE_ATTR_CHECK, false)
        }

        Timber.d("Start Service - Create Coroutine")
        startWorkerCoroutine()
        return START_NOT_STICKY
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
        val notification = createNotification(getString(R.string.section_photo_sync),
            getString(R.string.settings_camera_notif_initializing_title),
            null,
            false)
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

    private fun startWorkerCoroutine() = sharingScope.launch {
        runCatching {
            if (!hasCredentials()) {
                Timber.w("There are no user credentials")
                finish()
                return@launch
            }
            if (!hasPreferences()) {
                Timber.w("Preferences not defined, so not enabled")
                finish()
                return@launch
            }
            if (!isCameraUploadSyncEnabled()) {
                Timber.w("Sync enabled not defined or not enabled")
                finish()
                return@launch
            }
            if (!Util.isOnline(applicationContext)) {
                Timber.w("Not online")
                finish()
                return@launch
            }
            if (isDeviceLowOnBattery(batteryIntent)) {
                finish()
                return@launch
            }
            if (TextUtil.isTextEmpty(localPath())) {
                Timber.w("LocalPath is not defined, so not enabled")
                finish()
                return@launch
            }
            if (isWifiNotSatisfied()) {
                Timber.w("Cannot start, WiFi required")
                finish()
                return@launch
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
            stopForeground(true)
            cancelNotification()
            finish()
        }
    }

    private suspend fun startCameraUploads() {
        showNotification(getString(R.string.section_photo_sync),
            getString(R.string.settings_camera_notif_checking_title),
            pendingIntent,
            false)
        // Start the real uploading process, before is checking settings.
        filesFromMediaStore()
    }

    private fun extractMedia(cursor: Cursor, parentPath: String?): Queue<Media> {
        val queuedMedia: Queue<Media> = LinkedList()
        try {
            @Suppress("DEPRECATION")
            val dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            var modifiedColumn = 0
            var addedColumn = 0
            if (cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED) != -1) {
                modifiedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            }
            if (cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED) != -1) {
                addedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            }
            while (cursor.moveToNext()) {
                val media = Media()
                media.filePath = cursor.getString(dataColumn)
                val addedTime = cursor.getLong(addedColumn) * 1000
                val modifiedTime = cursor.getLong(modifiedColumn) * 1000
                media.timestamp = max(addedTime, modifiedTime)
                Timber.d("Extract from cursor, add time: %d, modify time: %d, chosen time: %d",
                    addedTime,
                    modifiedTime,
                    media.timestamp)
                val isFileValid =
                    media.filePath != null && !parentPath.isNullOrBlank()
                            && media.filePath!!.startsWith(parentPath)
                if (isFileValid) {
                    queuedMedia.add(media)
                }
            }
        } catch (exception: Exception) {
            Timber.e(exception)
        }
        return queuedMedia
    }

    private suspend fun filesFromMediaStore() {
        Timber.d("Get pending files from media store database.")
        cameraUploadNode = megaApi?.getNodeByHandle(cameraUploadHandle)
        if (cameraUploadNode == null) {
            Timber.d("ERROR: primary parent folder is null.")
            finish()
            return
        }
        val secondaryEnabled = isSecondaryFolderEnabled()
        if (secondaryEnabled) {
            Timber.d("Secondary upload is enabled.")
            secondaryUploadNode = megaApi?.getNodeByHandle(secondaryUploadHandle)
        }

        val primaryPhotos: Queue<Media> = LinkedList()
        val primaryVideos: Queue<Media> = LinkedList()
        val secondaryPhotos: Queue<Media> = LinkedList()
        val secondaryVideos: Queue<Media> = LinkedList()

        @Suppress("DEPRECATION") val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        val selectionPrimaryPhoto = selectionQuery(SyncTimeStamp.PRIMARY_PHOTO)
        val selectionPrimaryVideo = selectionQuery(SyncTimeStamp.PRIMARY_VIDEO)
        val selectionSecondaryPhoto = selectionQuery(SyncTimeStamp.SECONDARY_PHOTO)
        val selectionSecondaryVideo = selectionQuery(SyncTimeStamp.SECONDARY_VIDEO)

        val uris = getSyncFileUploadUris()
        val cameraLocalPath = localPath()

        for (i in uris.indices) {
            val uri = uris[i]
            val isVideo =
                uri == MediaStore.Video.Media.EXTERNAL_CONTENT_URI || uri == MediaStore.Video.Media.INTERNAL_CONTENT_URI

            //Primary Media Folder
            var cursorPrimary: Cursor?
            var orderVideo = MediaStore.MediaColumns.DATE_MODIFIED + " ASC "
            var orderImage = MediaStore.MediaColumns.DATE_MODIFIED + " ASC "

            // Only paging for files in internal storage, because files on SD card usually have same timestamp(the time when the SD is loaded).
            val shouldPagingPrimary = !SDCardUtils.isLocalFolderOnSDCard(this, cameraLocalPath)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && shouldPagingPrimary) {
                val args = Bundle()
                args.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, orderVideo)
                args.putString(ContentResolver.QUERY_ARG_OFFSET, "0")
                if (isVideo) {
                    args.putString(ContentResolver.QUERY_ARG_SQL_SELECTION,
                        selectionPrimaryVideo)
                    args.putString(ContentResolver.QUERY_ARG_SQL_LIMIT, PAGE_SIZE_VIDEO.toString())
                } else {
                    args.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selectionPrimaryPhoto)
                    args.putString(ContentResolver.QUERY_ARG_SQL_LIMIT, PAGE_SIZE.toString())
                }
                cursorPrimary = app?.contentResolver?.query(uri, projection, args, null)
            } else {
                if (shouldPagingPrimary) {
                    orderVideo = "$orderVideo LIMIT 0,$PAGE_SIZE_VIDEO"
                    orderImage = "$orderImage LIMIT 0,$PAGE_SIZE"
                }
                cursorPrimary = if (isVideo) {
                    app?.contentResolver?.query(uri,
                        projection,
                        selectionPrimaryVideo,
                        null,
                        orderVideo)
                } else {
                    app?.contentResolver?.query(uri,
                        projection,
                        selectionPrimaryPhoto,
                        null,
                        orderImage)
                }
            }
            if (cursorPrimary != null) {
                Timber.d("Extract %d media from cursor, is secondary: %s, is video: %s",
                    cursorPrimary.count,
                    false,
                    isVideo)
                val parentPath = localPath()
                if (!isVideo) {
                    primaryPhotos.addAll(extractMedia(cursorPrimary, parentPath))
                } else {
                    primaryVideos.addAll(extractMedia(cursorPrimary, parentPath))
                }
            }

            //Secondary Media Folder
            if (secondaryEnabled) {
                Timber.d("Secondary is enabled.")
                var cursorSecondary: Cursor?
                var orderVideoSecondary = MediaStore.MediaColumns.DATE_MODIFIED + " ASC "
                var orderImageSecondary = MediaStore.MediaColumns.DATE_MODIFIED + " ASC "
                val shouldPagingSecondary =
                    !SDCardUtils.isLocalFolderOnSDCard(this, localPathSecondary())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && shouldPagingSecondary) {
                    val args = Bundle()
                    args.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, orderVideo)
                    args.putString(ContentResolver.QUERY_ARG_OFFSET, "0")
                    if (isVideo) {
                        args.putString(ContentResolver.QUERY_ARG_SQL_SELECTION,
                            selectionSecondaryVideo)
                        args.putString(ContentResolver.QUERY_ARG_SQL_LIMIT,
                            PAGE_SIZE_VIDEO.toString())
                    } else {
                        args.putString(ContentResolver.QUERY_ARG_SQL_SELECTION,
                            selectionSecondaryPhoto)
                        args.putString(ContentResolver.QUERY_ARG_SQL_LIMIT, PAGE_SIZE.toString())
                    }
                    cursorSecondary = app?.contentResolver?.query(uri, projection, args, null)
                } else {
                    if (shouldPagingSecondary) {
                        orderVideoSecondary = "$orderVideoSecondary LIMIT 0,$PAGE_SIZE_VIDEO"
                        orderImageSecondary = "$orderImageSecondary LIMIT 0,$PAGE_SIZE"
                    }
                    cursorSecondary = if (isVideo) {
                        app?.contentResolver?.query(uri,
                            projection,
                            selectionSecondaryVideo,
                            null,
                            orderVideoSecondary)
                    } else {
                        app?.contentResolver?.query(uri,
                            projection,
                            selectionSecondaryPhoto,
                            null,
                            orderImageSecondary)
                    }
                }
                if (cursorSecondary != null) {
                    Timber.d("Extract %d media from cursor, is secondary: %s, is video: %s",
                        cursorSecondary.count,
                        true,
                        isVideo)
                    val parentPath = localPathSecondary()
                    if (!isVideo) {
                        secondaryPhotos.addAll(extractMedia(cursorSecondary, parentPath))
                    } else {
                        secondaryVideos.addAll(extractMedia(cursorSecondary, parentPath))
                    }
                }
            }
        }

        totalUploaded = 0
        prepareUpload(primaryPhotos,
            primaryVideos,
            secondaryPhotos,
            secondaryVideos,
            secondaryEnabled)
    }

    private suspend fun prepareUpload(
        primaryPhotos: Queue<Media>,
        primaryVideos: Queue<Media>,
        secondaryPhotos: Queue<Media>,
        secondaryVideos: Queue<Media>,
        secondaryEnabled: Boolean,
    ) {
        Timber.d("\nPrimary photo count from media store database: %d\nSecondary photo count from media store database: %d\nPrimary video count from media store database: %d\nSecondary video count from media store database: %d",
            primaryPhotos.size,
            secondaryPhotos.size,
            primaryVideos.size,
            secondaryVideos.size)

        val pendingUploadsList = getPendingList(primaryPhotos, isSecondary = false, isVideo = false)
        Timber.d("Primary photo pending list size: %s", pendingUploadsList.size)
        saveDataToDB(pendingUploadsList)

        val pendingVideoUploadsList = getPendingList(primaryVideos,
            isSecondary = false,
            isVideo = true)
        Timber.d("Primary video pending list size: %s", pendingVideoUploadsList.size)
        saveDataToDB(pendingVideoUploadsList)

        if (secondaryEnabled) {
            val pendingUploadsListSecondary = getPendingList(secondaryPhotos,
                isSecondary = true,
                isVideo = false)
            Timber.d("Secondary photo pending list size: %s", pendingUploadsListSecondary.size)
            saveDataToDB(pendingUploadsListSecondary)

            val pendingVideoUploadsListSecondary = getPendingList(secondaryVideos,
                isSecondary = true,
                isVideo = true)
            Timber.d("Secondary video pending list size: %s", pendingVideoUploadsListSecondary.size)
            saveDataToDB(pendingVideoUploadsListSecondary)
        }
        if (stopped) return

        // Need to maintain timestamp for better performance
        updateTimeStamp(null, SyncTimeStamp.PRIMARY_PHOTO)
        updateTimeStamp(null, SyncTimeStamp.PRIMARY_VIDEO)
        updateTimeStamp(null, SyncTimeStamp.SECONDARY_PHOTO)
        updateTimeStamp(null, SyncTimeStamp.SECONDARY_VIDEO)

        // Reset backup state as active.
        updatePrimaryBackupState(CameraUploadSyncManager.State.CU_SYNC_STATE_ACTIVE)
        updateSecondaryBackupState(CameraUploadSyncManager.State.CU_SYNC_STATE_ACTIVE)

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
        startActiveHeartbeat(finalList)
        for (file in finalList) {
            if (!running) break
            val isSec = file.isSecondary
            val parent = (if (isSec) secondaryUploadNode else cameraUploadNode) ?: continue

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
                            val pendingIntent = PendingIntent.getActivity(this,
                                0,
                                intent,
                                PendingIntent.FLAG_IMMUTABLE)
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
                    megaApi?.copyNode(megaApi?.getNodeByHandle(it),
                        parent,
                        file.fileName,
                        this)
                }
            } else {
                val toUpload = path?.let { File(it) }
                if ((toUpload != null) && toUpload.exists()) {
                    // compare size
                    val node = checkExistBySize(parent, toUpload.length())
                    if (node != null && node.originalFingerprint == null) {
                        Timber.d("Node with handle: %d already exists, delete record from database.",
                            node.handle)
                        path?.let {
                            deleteSyncRecord(it, isSec)
                        }
                    } else {
                        totalToUpload++
                        val lastModified = getLastModifiedTime(file)
                        megaApi?.startUpload(path, parent, file.fileName, lastModified / 1000,
                            Constants.APP_DATA_CU, false, false, null, this)
                    }
                } else {
                    Timber.d("Local file is unavailable, delete record from database.")
                    path?.let {
                        deleteSyncRecord(it, isSec)
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

    private fun checkExistBySize(parent: MegaNode, size: Long): MegaNode? {
        val nodeList = megaApi?.getChildren(parent, MegaApiJava.ORDER_ALPHABETICAL_ASC)
        if (nodeList != null) {
            for (node in nodeList) {
                if (node.size == size) {
                    return node
                }
            }
        }
        return null
    }

    private fun getLastModifiedTime(file: SyncRecord): Long {
        val source = file.localPath?.let { File(it) }
        return source?.lastModified() ?: 0
    }

    private suspend fun saveDataToDB(list: List<SyncRecord>) {
        for (file in list) {
            run {
                Timber.d("Handle with local file which timestamp is: %s", file.timestamp)
                if (stopped) return

                val exist = getSyncRecordByFingerprint(file.originFingerprint,
                    file.isSecondary,
                    file.isCopyOnly)
                if (exist != null) {
                    exist.timestamp?.let { existTime ->
                        file.timestamp?.let { fileTime ->
                            if (existTime < fileTime) {
                                Timber.d("Got newer time stamp.")
                                exist.localPath?.let {
                                    deleteSyncRecordByLocalPath(it, exist.isSecondary)
                                }
                            } else {
                                Timber.w("Duplicate sync records.")
                                return@run
                            }
                        }
                    }
                }

                val isSec = file.isSecondary
                val parent = if (isSec) secondaryUploadNode else cameraUploadNode
                if (!file.isCopyOnly) {
                    val resFile = file.localPath?.let { File(it) }
                    if (resFile != null && !resFile.exists()) {
                        Timber.w("File does not exist, remove from database.")
                        file.localPath?.let {
                            deleteSyncRecordByLocalPath(it, isSec)
                        }
                        return@run
                    }
                }

                var fileName: String?
                var inCloud: Boolean
                var inDatabase = false
                var photoIndex = 0
                if (keepFileNames()) {
                    //Keep the file names as device but need to handle same file name in different location
                    val tempFileName = file.fileName
                    do {
                        if (stopped) return
                        fileName = getNoneDuplicatedDeviceFileName(tempFileName, photoIndex)
                        Timber.d("Keep file name as in device, name index is: %s", photoIndex)
                        photoIndex++
                        inCloud = megaApi?.getChildNode(parent, fileName) != null
                        fileName?.let {
                            inDatabase = fileNameExists(it, isSec)
                        }
                    } while (inCloud || inDatabase)
                } else {
                    do {
                        if (stopped) return
                        fileName = Util.getPhotoSyncNameWithIndex(getLastModifiedTime(file),
                            file.localPath,
                            photoIndex)
                        Timber.d("Use MEGA name, name index is: %s", photoIndex)
                        photoIndex++
                        inCloud = megaApi?.getChildNode(parent, fileName) != null
                        inDatabase = fileNameExists(fileName, isSec)
                    } while (inCloud || inDatabase)
                }

                var extension = ""
                fileName?.let {
                    val splitName = it.split("\\.").toTypedArray()
                    if (splitName.isNotEmpty()) {
                        extension = splitName[splitName.size - 1]
                    }
                }
                file.fileName = fileName
                val newPath = "$tempRoot${System.nanoTime()}.$extension"
                file.newPath = newPath
                Timber.d("Save file to database, new path is: %s", newPath)
                saveSyncRecord(file)
            }
        }
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
        mediaList: Queue<Media>,
        isSecondary: Boolean,
        isVideo: Boolean,
    ): List<SyncRecord> {
        Timber.d("Get pending list, is secondary upload: %s, is video: %s", isSecondary, isVideo)
        val pendingList = mutableListOf<SyncRecord>()
        val parentNodeHandle = if (isSecondary) secondaryUploadHandle else cameraUploadHandle
        val parentNode = megaApi?.getNodeByHandle(parentNodeHandle)
        Timber.d("Upload to parent node which handle is: %s", parentNodeHandle)
        val type = if (isVideo) SyncRecordType.TYPE_VIDEO.value else SyncRecordType.TYPE_PHOTO.value

        while (mediaList.size > 0) {
            if (stopped) break
            val media = mediaList.poll() ?: continue

            if (media.filePath?.let { mediaLocalPathExists(it, isSecondary) } == true) {
                Timber.d("Skip media with timestamp: %s", media.timestamp)
                continue
            }

            // Source file
            val sourceFile = media.filePath?.let { File(it) }
            val localFingerPrint = megaApi?.getFingerprint(media.filePath)
            var nodeExists: MegaNode? = null
            try {
                nodeExists = parentNode?.let { node ->
                    localFingerPrint?.let { fingerprint ->
                        getPossibleNodeFromCloud(fingerprint, node)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            if (nodeExists == null) {
                Timber.d("Possible node with same fingerprint is null.")
                val gpsData = sourceFile?.let { getGPSCoordinates(it.absolutePath, isVideo) }
                val record = SyncRecord(0,
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
                    isSecondary)
                Timber.d("Add local file with timestamp: %d to pending list, for upload.",
                    record.timestamp)
                pendingList.add(record)
            } else {
                Timber.d("Possible node with same fingerprint which handle is: %s",
                    nodeExists.handle)
                if (megaApi?.getParentNode(nodeExists)?.handle != parentNodeHandle) {
                    val record = SyncRecord(0,
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
                        isSecondary)
                    Timber.d("Add local file with handle: %d to pending list, for copy.",
                        record.nodeHandle)
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
            localFolderUnavailableNotification(R.string.camera_notif_primary_local_unavailable,
                LOCAL_FOLDER_REMINDER_PRIMARY)
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
            localFolderUnavailableNotification(R.string.camera_notif_secondary_local_unavailable,
                LOCAL_FOLDER_REMINDER_SECONDARY)
            // disable media upload only
            CameraUploadUtil.disableMediaUploadProcess()
            setSecondaryFolderPath(SettingsConstants.INVALID_PATH)
            sendBroadcast(Intent(BroadcastConstants.ACTION_DISABLE_MEDIA_UPLOADS_SETTING))
            return SHOULD_RUN_STATE_FAILED
        } else {
            notificationManager?.cancel(LOCAL_FOLDER_REMINDER_SECONDARY)
        }

        if (megaApi?.rootNode == null && !MegaApplication.isLoggingIn()) {
            Timber.w("RootNode = null")
            running = true
            MegaApplication.setLoggingIn(true)
            // TODO Remove DbHandler and Refactor in MegaApi dependency removal with use cases:
            // GetSession, FastLogin, InitMegaChat (already provided)
            megaApi?.fastLogin(tempDbHandler.credentials.session, this)
            ChatUtil.initMegaChatApi(tempDbHandler.credentials.session)
            return LOGIN_IN
        }
        cameraUploadHandle = CameraUploadUtil.getPrimaryFolderHandle()
        secondaryUploadHandle = CameraUploadUtil.getSecondaryFolderHandle()

        // Prevent checking while app alive because it has been handled by global event
        Timber.d("ignoreAttr: %s", ignoreAttr)
        if (!ignoreAttr && !isPrimaryHandleSynced) {
            Timber.d("Try to get Camera Uploads primary target folder from CU attribute.")
            megaApi?.getUserAttribute(MegaApiJava.USER_ATTR_CAMERA_UPLOADS_FOLDER,
                getAttrUserListener)
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
            notification = createNotification(getString(R.string.section_photo_sync),
                getString(resId),
                null,
                false)
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
        val needToSetPrimary = isNodeInRubbishOrDeleted(cameraUploadHandle)
        val secondaryEnabled = isSecondaryFolderEnabled()

        if (needToSetPrimary) {
            // Try to find a folder which name is "Camera Uploads" from root.
            cameraUploadHandle =
                CameraUploadUtil.findDefaultFolder(getString(R.string.section_photo_sync))
            // Cannot find a folder with the name, create one.
            if (cameraUploadHandle == MegaApiJava.INVALID_HANDLE) {
                // Flag, prevent to create duplicate folder.
                if (!isCreatingPrimary) {
                    Timber.d("Must create CU folder.")
                    isCreatingPrimary = true
                    // Create a folder with name "Camera Uploads" at root.
                    megaApi?.createFolder(getString(R.string.section_photo_sync),
                        megaApi?.rootNode,
                        createFolderListener)
                }
                if (!secondaryEnabled) {
                    return TARGET_FOLDER_NOT_EXIST
                }
            } else {
                // Found, prepare to set the folder as CU folder.
                primaryToSet = cameraUploadHandle
            }
        }

        var secondaryToSet = MegaApiJava.INVALID_HANDLE
        var needToSetSecondary = false
        // Only check MU folder when secondary upload is enabled.
        if (secondaryEnabled) {
            Timber.d("Secondary uploads are enabled.")
            // If MU folder in local setting is deleted, then need to reset.
            needToSetSecondary = isNodeInRubbishOrDeleted(secondaryUploadHandle)
            if (needToSetSecondary) {
                // Try to find a folder which name is "Media Uploads" from root.
                secondaryUploadHandle =
                    CameraUploadUtil.findDefaultFolder(getString(R.string.section_secondary_media_uploads))
                // Cannot find a folder with the name, create one.
                if (secondaryUploadHandle == MegaApiJava.INVALID_HANDLE) {
                    // Flag, prevent to create duplicate folder.
                    if (!isCreatingSecondary) {
                        Timber.d("Must create MU folder.")
                        isCreatingSecondary = true
                        // Create a folder with name "Media Uploads" at root.
                        megaApi?.createFolder(getString(R.string.section_secondary_media_uploads),
                            megaApi?.rootNode,
                            createFolderListener)
                    }
                    return TARGET_FOLDER_NOT_EXIST
                } else {
                    // Found, prepare to set the folder as MU folder.
                    secondaryToSet = secondaryUploadHandle
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
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MegaDownloadServicePowerLock:")
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PAGE_SIZE = 1000
            PAGE_SIZE_VIDEO = 50
        } else {
            PAGE_SIZE = 400
            PAGE_SIZE_VIDEO = 10
        }

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

        sharingScope.launch {
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
        stopForeground(true)
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
            sharingScope.launch {
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
                MegaApplication.setLoggingIn(false)
                finish()
            }
        } else if (request.type == MegaRequest.TYPE_FETCH_NODES) {
            if (e.errorCode == MegaError.API_OK) {
                Timber.d("fetch nodes ok")
                MegaApplication.setLoggingIn(false)
                Timber.d("Start service here MegaRequest.TYPE_FETCH_NODES")
                startWorkerCoroutine()
            } else {
                Timber.d("ERROR: %s", e.errorString)
                MegaApplication.setLoggingIn(false)
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
                val node = megaApi?.getNodeByHandle(request.nodeHandle)
                val fingerPrint = node?.fingerprint
                val isSecondary = node?.parentHandle == secondaryUploadHandle
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
            if (cameraUploadHandle != handle) cameraUploadHandle = handle
            if (shouldStart) {
                Timber.d("On Get Primary - Start Coroutine")
                startWorkerCoroutine()
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
            if (handle != secondaryUploadHandle) secondaryUploadHandle = handle
            // Start to upload. Unlike onGetPrimaryFolderAttribute needs to wait for getting MU folder handle completes.
            Timber.d("On Get Secondary - Start Coroutine")
            startWorkerCoroutine()
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
        startWorkerCoroutine()
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
    override fun onTransferTemporaryError(api: MegaApiJava, transfer: MegaTransfer, e: MegaError) {
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
        Timber.d("Image sync finished, error code: %d, handle: %d, size: %d",
            e.errorCode,
            transfer.nodeHandle,
            transfer.transferredBytes)
        try {
            LiveEventBus.get(EVENT_TRANSFER_UPDATE, Int::class.java).post(MegaTransfer.TYPE_UPLOAD)
            sharingScope.launch {
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
            addCompletedTransfer(AndroidCompletedTransfer(transfer, e),
                tempDbHandler)
        }

        if (e.errorCode == MegaError.API_OK) {
            Timber.d("Image Sync API_OK")
            val node = megaApi?.getNodeByHandle(transfer.nodeHandle)
            val isSecondary = node?.parentHandle == secondaryUploadHandle
            val record = getSyncRecordByPath(path, isSecondary)
            if (record != null) {
                node?.let { onUploadSuccess(it, record.isSecondary) }
                val originalFingerprint = record.originFingerprint
                megaApi?.setOriginalFingerprint(node, originalFingerprint, this)
                record.latitude?.let { latitude ->
                    record.longitude?.let { longitude ->
                        megaApi?.setNodeCoordinates(node,
                            latitude.toDouble(),
                            longitude.toDouble(),
                            null)
                    }
                }
                val src = record.localPath?.let { File(it) }
                if (src != null && src.exists()) {
                    Timber.d("Creating preview")
                    val previewDir = PreviewUtils.getPreviewFolder(this)
                    val preview = File(previewDir,
                        MegaApiAndroid.handleToBase64(transfer.nodeHandle) + FileUtil.JPG_EXTENSION)
                    val thumbDir = ThumbnailUtils.getThumbFolder(this)
                    val thumb = File(thumbDir,
                        MegaApiAndroid.handleToBase64(transfer.nodeHandle) + FileUtil.JPG_EXTENSION)
                    if (FileUtil.isVideoFile(transfer.path)) {
                        val img = record.localPath?.let { File(it) }
                        if (!preview.exists()) {
                            ImageProcessor.createVideoPreview(this@CameraUploadsService,
                                img,
                                preview)
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
        Timber.d("Total to upload: %d Total uploaded: %d Pending uploads: %d",
            totalToUpload,
            totalUploaded,
            megaApi?.numPendingUploads)
        if (totalToUpload == totalUploaded) {
            Timber.d("Photo upload finished, now checking videos")
            if (compressedVideoPending() && !canceled && isCompressorAvailable()) {
                Timber.d("Got pending videos, will start compress")
                startVideoCompression()
            } else {
                Timber.d("No pending videos, finish")
                onQueueComplete()
                sendBroadcast(Intent(BroadcastConstants.ACTION_UPDATE_CU)
                    .putExtra(BroadcastConstants.PROGRESS, 100)
                    .putExtra(BroadcastConstants.PENDING_TRANSFERS, 0))
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
        Timber.d("Total videos count are %d, %d mb to Conversion",
            fullList.size,
            totalPendingSizeInMB)

        if (shouldStartVideoCompression(totalPendingSizeInMB)) {
            sharingScope.launch {
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
            val message = getString(R.string.message_compression_size_over_limit,
                getString(R.string.label_file_size_mega_byte, size))
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
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
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
            val subText = getString(R.string.title_compress_video,
                videoCompressor?.currentFileIndex,
                videoCompressor?.totalCount)
            showProgressNotification(progress, pendingIntent, message, subText, "")
        }
    }

    /**
     * Compression successful
     */
    @Synchronized
    override fun onCompressSuccessful(record: SyncRecord) {
        sharingScope.launch {
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
                    sharingScope.launch {
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
            sharingScope.launch {
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
        sharingScope.launch {
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

                sendBroadcast(Intent(BroadcastConstants.ACTION_UPDATE_CU)
                    .putExtra(BroadcastConstants.PROGRESS, progressPercent)
                    .putExtra(BroadcastConstants.PENDING_TRANSFERS, pendingTransfers))

                message = if (megaApi?.areTransfersPaused(MegaTransfer.TYPE_UPLOAD) == true) {
                    StringResourcesUtils.getString(R.string.upload_service_notification_paused,
                        inProgress,
                        totalTransfers)
                } else {
                    StringResourcesUtils.getString(R.string.upload_service_notification,
                        inProgress,
                        totalTransfers)
                }
            }

            val info = Util.getProgressSize(this, totalSizeTransferred, totalSizePendingTransfer)
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            showProgressNotification(progressPercent,
                pendingIntent,
                message,
                info,
                getString(R.string.settings_camera_notif_title))
        }
    }

    private fun createNotification(
        title: String,
        content: String,
        intent: PendingIntent?,
        isAutoCancel: Boolean,
    ): Notification? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(notificationChannelId,
                notificationChannelName,
                NotificationManager.IMPORTANCE_DEFAULT)
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
            val channel = NotificationChannel(notificationChannelId,
                notificationChannelName,
                NotificationManager.IMPORTANCE_DEFAULT)
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
            .setContentIntent(PendingIntent.getActivity(this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            .setTicker(contentText)
            .setContentTitle(message)
            .setOngoing(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(OVER_QUOTA_NOTIFICATION_CHANNEL_ID,
                notificationChannelName,
                NotificationManager.IMPORTANCE_DEFAULT)
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

    private fun getNoneDuplicatedDeviceFileName(fileName: String?, index: Int): String? {
        if (index == 0) {
            return fileName
        }
        var name = ""
        var extension = ""
        val pos = fileName?.lastIndexOf(".")
        if (pos != null && pos > 0) {
            name = fileName.substring(0, pos)
            extension = fileName.substring(pos)
        }
        return "${name}_$index$extension"
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

    /**
     * Check if there's a node with the same fingerprint in cloud drive. In order to avoid uploading duplicate file.
     *
     *
     * NOTE: only looking for the node by original fingerprint is not enough,
     * because some old nodes don't have the attribute OriginalFingerprint.
     * In this case, should also looking for the node by attribute Fingerprint.
     *
     * @param localFingerPrint Fingerprint of the local file.
     * @param parentNode       Preferred parent node, could be null for searching all the place in cloud drive.
     * @return A node with the same fingerprint, or null when cannot find.
     */
    private fun getPossibleNodeFromCloud(
        localFingerPrint: String,
        parentNode: MegaNode,
    ): MegaNode? {
        var preferNode: MegaNode?

        // Try to find the node by original fingerprint from the selected parent folder.
        var possibleNodeListFPO =
            megaApi?.getNodesByOriginalFingerprint(localFingerPrint, parentNode)
        preferNode = getFirstNodeFromList(possibleNodeListFPO)
        if (preferNode != null) {
            Timber.d("Found node by original fingerprint with the same local fingerprint in node with handle: %d, node handle: %d",
                parentNode.handle,
                preferNode.handle)
            return preferNode
        }

        // Try to find the node by fingerprint from the selected parent folder.
        preferNode = megaApi?.getNodeByFingerprint(localFingerPrint, parentNode)
        if (preferNode != null) {
            Timber.d("Found node by fingerprint with the same local fingerprint in node with handle: %d, node handle: %d",
                parentNode.handle,
                preferNode.handle)
            return preferNode
        }

        // Try to find the node by original fingerprint in the account.
        possibleNodeListFPO = megaApi?.getNodesByOriginalFingerprint(localFingerPrint, null)
        preferNode = getFirstNodeFromList(possibleNodeListFPO)
        if (preferNode != null) {
            Timber.d("Found node by original fingerprint with the same local fingerprint in the account, node handle: %s",
                preferNode.handle)
            return preferNode
        }

        // Try to find the node by fingerprint in the account.
        preferNode = megaApi?.getNodeByFingerprint(localFingerPrint)
        if (preferNode != null) {
            Timber.d("Found node by fingerprint with the same local fingerprint in the account, node handle: %s",
                preferNode.handle)
            return preferNode
        }
        return null
    }

    private fun getFirstNodeFromList(megaNodeList: MegaNodeList?): MegaNode? {
        return if (megaNodeList != null && megaNodeList.size() > 0) {
            megaNodeList[0]
        } else null
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
