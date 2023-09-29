package mega.privacy.android.app

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.components.saver.AutoPlayInfo
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_REFRESH_CLEAR_OFFLINE_SETTING
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_TAKEN_DOWN_FILES
import mega.privacy.android.app.constants.BroadcastConstants.NUMBER_FILES
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.globalmanagement.TransfersManagement.Companion.createInitialServiceNotification
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.monitoring.CrashReporter
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.notifications.TransferOverQuotaNotification
import mega.privacy.android.app.presentation.offline.OfflineFragment
import mega.privacy.android.app.presentation.transfers.model.mapper.LegacyCompletedTransferMapper
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.CacheFolderManager.buildVoiceClipFile
import mega.privacy.android.app.utils.CacheFolderManager.getCacheFolder
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.SDCardOperator
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.facade.INTENT_EXTRA_NODE_HANDLE
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferFinishType
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.TransfersFinishedState
import mega.privacy.android.domain.entity.transfer.getSDCardDownloadAppData
import mega.privacy.android.domain.entity.transfer.isBackgroundTransfer
import mega.privacy.android.domain.entity.transfer.isSDCardDownload
import mega.privacy.android.domain.entity.transfer.isVoiceClip
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.BroadcastOfflineFileAvailabilityUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.file.EscapeFsIncompatibleUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.login.CompleteFastLoginUseCase
import mega.privacy.android.domain.usecase.login.GetSessionUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.offline.IsOfflineTransferUseCase
import mega.privacy.android.domain.usecase.offline.SaveOfflineNodeInformationUseCase
import mega.privacy.android.domain.usecase.transfers.BroadcastTransfersFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferDataUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorStopTransfersWorkUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.completed.AddCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.CancelAllDownloadTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetCurrentDownloadSpeedUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetNumPendingDownloadsNonBackgroundUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetTotalDownloadBytesUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetTotalDownloadedBytesUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetTotalDownloadsUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ResetTotalDownloadsUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorDownloadTransfersPausedLegacyUseCase
import mega.privacy.android.domain.usecase.transfers.sd.DeleteSdTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.sd.InsertSdTransferUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * Background service to download files
 */
@AndroidEntryPoint
internal class DownloadService : LifecycleService() {

    @Inject
    lateinit var crashReporter: CrashReporter

    @Inject
    lateinit var transfersManagement: TransfersManagement

    @Inject
    lateinit var dbH: LegacyDatabaseHandler

    @Inject
    lateinit var activityLifecycleHandler: ActivityLifecycleHandler

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    @Inject
    @MegaApiFolder
    lateinit var megaApiFolder: MegaApiAndroid

    @IoDispatcher
    @Inject
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var getNumPendingDownloadsNonBackgroundUseCase: GetNumPendingDownloadsNonBackgroundUseCase

    @Inject
    lateinit var rootNodeExistsUseCase: RootNodeExistsUseCase

    @Inject
    lateinit var broadcastTransferOverQuotaUseCase: BroadcastTransferOverQuotaUseCase

    @Inject
    lateinit var monitorDownloadPausedTransfersUseCase: MonitorDownloadTransfersPausedLegacyUseCase

    @Inject
    lateinit var monitorStopTransfersWorkUseCase: MonitorStopTransfersWorkUseCase

    @Inject
    lateinit var broadcastTransfersFinishedUseCase: BroadcastTransfersFinishedUseCase

    @Inject
    lateinit var broadcastOfflineNodeAvailability: BroadcastOfflineFileAvailabilityUseCase

    @Inject
    lateinit var addCompletedTransferUseCase: AddCompletedTransferUseCase

    @Inject
    lateinit var cancelAllDownloadTransfersUseCase: CancelAllDownloadTransfersUseCase

    @Inject
    lateinit var legacyCompletedTransferMapper: LegacyCompletedTransferMapper

    @Inject
    lateinit var getTransferDataUseCase: GetTransferDataUseCase

    @Inject
    lateinit var monitorTransferEventsUseCase: MonitorTransferEventsUseCase

    @Inject
    lateinit var cancelTransferByTagUseCase: CancelTransferByTagUseCase

    @Inject
    lateinit var isOfflineTransferUseCase: IsOfflineTransferUseCase

    @Inject
    lateinit var saveOfflineNodeInformationUseCase: SaveOfflineNodeInformationUseCase

    @Inject
    lateinit var completeFastLoginUseCase: CompleteFastLoginUseCase

    @Inject
    lateinit var getSessionUseCase: GetSessionUseCase

    @Inject
    lateinit var monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase

    @Inject
    lateinit var insertSdTransferUseCase: InsertSdTransferUseCase

    @Inject
    lateinit var deleteSdTransferByTagUseCase: DeleteSdTransferByTagUseCase

    @Inject
    lateinit var getFingerprintUseCase: GetFingerprintUseCase

    @Inject
    lateinit var getTransferByTagUseCase: GetTransferByTagUseCase

    @Inject
    lateinit var escapeFsIncompatibleUseCase: EscapeFsIncompatibleUseCase

    @Inject
    lateinit var resetTotalDownloadsUseCase: ResetTotalDownloadsUseCase

    @Inject
    lateinit var getTotalDownloadsUseCase: GetTotalDownloadsUseCase

    @Inject
    lateinit var getCurrentDownloadSpeedUseCase: GetCurrentDownloadSpeedUseCase

    @Inject
    lateinit var getTotalDownloadedBytesUseCase: GetTotalDownloadedBytesUseCase

    @Inject
    lateinit var getTotalDownloadBytesUseCase: GetTotalDownloadBytesUseCase

    @Inject
    lateinit var broadcastBusinessAccountExpiredUseCase: BroadcastBusinessAccountExpiredUseCase

    private var errorCount = 0
    private var alreadyDownloaded = 0
    private var isForeground = false
    private var canceled = false
    private var openFile = true
    private var downloadForPreview = false
    private var downloadByOpenWith = false
    private var alreadyLoggedIn = false
    private var type: String? = ""
    private var isOverQuota = false
    private var downloadedBytesToOverQuota: Long = 0
    private var pendingIntents = ArrayList<Intent>()
    lateinit var wifiLock: WifiManager.WifiLock
    lateinit var wakeLock: PowerManager.WakeLock
    private var currentFile: File? = null
    private var currentDir: File? = null
    private var currentDocument: MegaNode? = null
    private var transfersCount = 0
    private var backgroundTransfers: MutableSet<Int> = HashSet()
    private val storeToAdvancedDevices: MutableMap<Long, Uri> = mutableMapOf()
    private val fromMediaViewers: MutableMap<Long, Boolean> = mutableMapOf()
    private lateinit var mNotificationManager: NotificationManager
    private var lastUpdated: Long = 0
    private var intent: Intent? = null

    private val uiHandler = Handler(Looper.getMainLooper())

    // the flag to determine the rating dialog is showed for this download action
    private var isRatingShowed = false
    private var isDownloadForOffline = false

    /**
     * Contains the info of a node that to be opened in-app.
     */
    private var autoPlayInfo: AutoPlayInfo? = null

    private val intentFlow = MutableSharedFlow<Intent>()

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")
        initialiseService()
    }

    private fun initialiseService() {
        isForeground = false
        canceled = false
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        startForeground()
        initialiseWifiLock()
        initialiseWakeLock()
        setReceivers()
    }

    private fun initialiseWifiLock() {
        val wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF
        val wifiManager =
            applicationContext.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(wifiLockMode, "MegaDownloadServiceWifiLock")
    }

    private fun initialiseWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock =
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Mega:DownloadServicePowerLock")
    }

    @SuppressLint("WrongConstant")
    private fun setReceivers() {

        lifecycleScope.launch {
            monitorDownloadPausedTransfersUseCase().collectLatest {
                // delay 1 second to refresh the pause notification to prevent update is missed
                delay(TransfersManagement.WAIT_TIME_BEFORE_UPDATE)
                updateProgressNotification(it)
            }
        }

        lifecycleScope.launch {
            monitorStopTransfersWorkUseCase().conflate().collect {
                if (getNumPendingDownloadsNonBackgroundUseCase() == 0) stopForeground()
            }
        }

        lifecycleScope.launch {
            monitorTransferEventsUseCase()
                .filter {
                    it.transfer.transferType == TransferType.DOWNLOAD
                }
                .catch {
                    Timber.e(it)
                }
                .collect { transferEvent ->
                    when (transferEvent) {
                        is TransferEvent.TransferStartEvent -> {
                            doOnTransferStart(transferEvent.transfer)
                        }

                        is TransferEvent.TransferUpdateEvent -> {
                            doOnTransferUpdate(transferEvent.transfer)
                        }

                        is TransferEvent.TransferFinishEvent -> {
                            doOnTransferFinish(transferEvent.transfer, transferEvent.error)
                        }

                        is TransferEvent.TransferTemporaryErrorEvent -> {
                            doOnTransferTemporaryError(
                                transferEvent.transfer,
                                transferEvent.error
                            )
                        }

                        else -> {}
                    }
                }
        }

        lifecycleScope.launch {
            monitorFetchNodesFinishUseCase().conflate().collect {
                proceedWithPendingIntentsAfterLogin()
            }
        }

        lifecycleScope.launch {
            intentFlow.collect { intent ->
                runCatching {
                    onHandleIntent(intent)
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }

    private fun startForeground() {
        isForeground = runCatching {
            val notification = createInitialNotification()
            startForeground(
                Constants.NOTIFICATION_DOWNLOAD,
                notification
            )
        }.onFailure {
            Timber.e(it, "Exception starting foreground")
            crashReporter.log("Exception starting foreground: ${it.message}")
        }.isSuccess
    }

    @Suppress("DEPRECATION")
    private fun createInitialNotification() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createInitialServiceNotification(
                notificationChannelId = Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                notificationChannelName = Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                mNotificationManager = mNotificationManager,
                mBuilderCompat = NotificationCompat.Builder(
                    this@DownloadService,
                    Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
                )
            )

        } else {
            createInitialServiceNotification(
                Notification.Builder(this@DownloadService)
            )
        }


    private fun stopForeground() {
        isForeground = false
        stopForeground(true)
        mNotificationManager.cancel(Constants.NOTIFICATION_DOWNLOAD)
        stopSelf()
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        releaseLocks()
        // remove all the generated folders in cache folder on SD card.
        val fs = externalCacheDirs
        if (fs.size > 1 && fs[1] != null) {
            FileUtil.purgeDirectory(fs[1])
        }
        stopForeground()
        super.onDestroy()
    }

    private suspend fun cancelDownloadTransfers() {
        runCatching {
            cancelAllDownloadTransfersUseCase()
            cancel()
        }.onFailure {
            Timber.e(it)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.d("onStartCommand")
        canceled = false
        when {
            intent?.action == ACTION_CANCEL -> {
                Timber.d("Cancel intent")
                lifecycleScope.launch {
                    canceled = true
                    cancelDownloadTransfers()
                    stopForeground()
                }
            }

            intent != null -> {
                lifecycleScope.launch {
                    intentFlow.emit(intent)
                }
            }

            else -> stopForeground()
        }
        return START_NOT_STICKY
    }

    private suspend fun onHandleIntent(intent: Intent) = withContext(ioDispatcher) {
        Timber.d("onHandleIntent")
        this@DownloadService.intent = intent
        if (intent.action != null && intent.action == Constants.ACTION_RESTART_SERVICE) {
            transfersCount = 0
            getTransferDataUseCase()?.let { transferData ->
                val uploadsInProgress = transferData.numDownloads
                for (i in 0 until uploadsInProgress) {
                    coroutineContext.ensureActive()
                    getTransferByTagUseCase(transferData.downloadTags[i])?.let { transfer ->
                        if (!transfer.isVoiceClip() && !transfer.isBackgroundTransfer()) {
                            transfersCount++
                        }
                    }
                }
                if (transfersCount > 0) {
                    updateProgressNotification()
                } else {
                    stopForeground()
                }
            } ?: stopForeground()
            return@withContext
        }

        isDownloadForOffline = intent.getBooleanExtra(EXTRA_DOWNLOAD_FOR_OFFLINE, false)

        openFile = intent.getBooleanExtra(EXTRA_OPEN_FILE, true)
        downloadForPreview = intent.getBooleanExtra(EXTRA_DOWNLOAD_FOR_PREVIEW, false)
        downloadByOpenWith = intent.getBooleanExtra(EXTRA_DOWNLOAD_BY_OPEN_WITH, false)
        type = intent.getStringExtra(Constants.EXTRA_TRANSFER_TYPE)

        val isScheduleDownload = processIntent(intent)
        if (!isScheduleDownload && getNumPendingDownloadsNonBackgroundUseCase() <= 0) {
            cancel()
        }
    }

    private suspend fun processIntent(intent: Intent): Boolean {
        if (addPendingIntentIfNotLoggedIn(intent)) return false

        val isFolderLink = intent.getBooleanExtra(EXTRA_FOLDER_LINK, false)
        val fromMV = intent.getBooleanExtra(EXTRA_FROM_MV, false)
        Timber.d("fromMV: %s", fromMV)
        val contentUri = intent.getStringExtra(EXTRA_CONTENT_URI)?.let { Uri.parse(it) }
        val highPriority = intent.getBooleanExtra(Constants.HIGH_PRIORITY_TRANSFER, false)
        val node: MegaNode = getNodeForIntent(intent, isFolderLink) ?: return false

        fromMediaViewers[node.handle] = fromMV
        currentDir = getDir(intent)
        currentDir?.mkdirs()
        currentFile = if (currentDir?.isDirectory == true) {
            escapeFsIncompatibleUseCase(
                node.name,
                currentDir?.absolutePath + Constants.SEPARATOR
            )?.let { File(currentDir, it) }
        } else {
            currentDir
        }
        var appData = getSDCardAppData(intent)
        if (currentDocument?.let { checkCurrentFile(it) } != true) {
            Timber.d("checkCurrentFile == false")
            alreadyDownloaded++
            if (getNumPendingDownloadsNonBackgroundUseCase() <= 0) {
                onQueueComplete(node.handle)
            }
            return true
        }
        acquireLocks()
        if (contentUri != null || currentDir?.isDirectory == true) {
            if (contentUri != null) {
                //To download to Advanced Devices
                currentDir = File(intent.getStringExtra(EXTRA_PATH))
                currentDir?.mkdirs()
                if (currentDir?.isDirectory == false) {
                    Timber.w("currentDir is not a directory")
                }
                storeToAdvancedDevices[node.handle] = contentUri
            } else if (currentFile?.exists() == true) {
                //Check the fingerprint
                val localFingerprint = currentFile?.absolutePath?.let { getFingerprintUseCase(it) }

                if (!localFingerprint.isNullOrEmpty() && localFingerprint == node.fingerprint) {
                    Timber.d("Delete the old version")
                    currentFile?.delete()
                }
            }
            if (isFolderLink) {
                currentDocument = megaApiFolder.authorizeNode(currentDocument)
            }
            if (transfersManagement.isOnTransferOverQuota()) {
                checkTransferOverQuota(false)
            }
            Timber.d("CurrentDocument is not null")
            if (TextUtil.isTextEmpty(appData)) {
                appData =
                    if (type?.contains(Constants.APP_DATA_VOICE_CLIP) == true) Constants.APP_DATA_VOICE_CLIP else ""
            }
            val localPath = currentDir?.absolutePath + "/"
            val token = transfersManagement.addScanningTransfer(
                MegaTransfer.TYPE_DOWNLOAD,
                localPath, node, node.isFolder
            )
            megaApi.startDownload(
                currentDocument,
                localPath,
                node.name,
                appData,
                highPriority,
                token,
                MegaTransfer.COLLISION_CHECK_FINGERPRINT,
                MegaTransfer.COLLISION_RESOLUTION_NEW_WITH_N
            )
            return true
        } else {
            Timber.w("currentDir is not a directory")
        }

        return false
    }

    private suspend fun addPendingIntentIfNotLoggedIn(intent: Intent): Boolean {
        val accountSession = if (alreadyLoggedIn) null else getSessionUseCase()

        return when {
            //Already logged in, no more actions required
            alreadyLoggedIn -> false
            //User is not logged in, file or folder link download
            accountSession.isNullOrEmpty() -> false
            /*
             A login is already in progress, but we don't have a way to know if it started
             from this service or from other place, so we need to listen for fetch nodes finish.
             */
            MegaApplication.isLoggingIn -> {
                pendingIntents.add(intent)
                Timber.w("Another login is processing")
                false
            }
            //User is logged in and fast login is not required
            rootNodeExistsUseCase() -> {
                alreadyLoggedIn = true
                false
            }

            //User is logged in, but needs to check if a fast login is required
            else -> {
                pendingIntents.add(intent)
                MegaApplication.isLoggingIn = true
                val result = runCatching {
                    completeFastLoginUseCase(accountSession)
                }.onSuccess {
                    alreadyLoggedIn = true
                }.onFailure {
                    Timber.e("ERROR: $it")
                }
                MegaApplication.isLoggingIn = false
                result.isSuccess
            }
        }
    }

    private suspend fun proceedWithPendingIntentsAfterLogin() {
        // Get cookies settings after login.
        getInstance().checkEnabledCookies()
        pendingIntents.forEach { intent -> intentFlow.emit(intent) }
        pendingIntents.clear()
    }


    private fun getNodeForIntent(
        intent: Intent,
        isFolderLink: Boolean,
    ): MegaNode? {
        val serialize = intent.getStringExtra(Constants.EXTRA_SERIALIZE_STRING)
        val hash = intent.getLongExtra(EXTRA_HASH, -1)
        val node = when {
            serialize != null -> {
                deserialiseNode(serialize)
            }

            isFolderLink -> {
                megaApiFolder.getNodeByHandle(hash)
            }

            else -> {
                megaApi.getNodeByHandle(hash)
            }
        }
        if (node == null) {
            Timber.w("Node not found")
        }
        currentDocument = node
        return node
    }

    private fun deserialiseNode(
        serialize: String?,
    ): MegaNode? {
        Timber.d("serializeString: %s", serialize)
        val result = MegaNode.unserialize(serialize)
        if (result != null) {
            Timber.d("hash after unserialize: %s", result.handle)
        } else {
            Timber.w("Node is NULL after unserialize")
        }
        return result
    }

    /**
     * Checks if the download of the current Intent corresponds to a SD card download.
     * If so, stores the SD card paths on an app data String.
     * If not, do nothing.
     *
     * @param intent Current Intent.
     * @return The app data String.
     */
    private fun getSDCardAppData(intent: Intent?): String? {
        if (intent == null
            || !intent.getBooleanExtra(EXTRA_DOWNLOAD_TO_SDCARD, false)
        ) {
            return null
        }
        var sDCardAppData = Constants.APP_DATA_SD_CARD
        val targetPath = intent.getStringExtra(EXTRA_TARGET_PATH)
        if (!TextUtil.isTextEmpty(targetPath)) {
            sDCardAppData += Constants.APP_DATA_INDICATOR + targetPath
        }
        val targetUri = intent.getStringExtra(EXTRA_TARGET_URI)
        if (!TextUtil.isTextEmpty(targetUri)) {
            sDCardAppData += Constants.APP_DATA_INDICATOR + targetUri
        }
        return sDCardAppData
    }

    private suspend fun onQueueComplete(handle: Long) {
        Timber.d("onQueueComplete")
        releaseLocks()
        coroutineContext.ensureActive()
        showCompleteNotification(handle)
        val pendingDownloads = getNumPendingDownloadsNonBackgroundUseCase()
        Timber.d("onQueueComplete: total of files before reset %s", pendingDownloads)
        if (pendingDownloads <= 0) {
            Timber.d("onQueueComplete: reset total downloads")
            // When download a single file by tapping it, and auto play is enabled.
            val totalDownloads = getTotalDownloadsUseCase() - backgroundTransfers.size
            if (totalDownloads == 1 && autoPlayInfo != null && downloadForPreview) {
                // If the file is Microsoft file, send the corresponding broadcast
                TransfersFinishedState(
                    type = TransferFinishType.DOWNLOAD_FILE_AND_OPEN_FOR_PREVIEW,
                    nodeName = autoPlayInfo?.nodeName,
                    nodeId = autoPlayInfo?.nodeHandle,
                    nodeLocalPath = autoPlayInfo?.localPath,
                    isOpenWith = downloadByOpenWith
                )
            } else if (totalDownloads == 1 && java.lang.Boolean.parseBoolean(dbH.autoPlayEnabled)
                && autoPlayInfo != null
            ) {
                TransfersFinishedState(
                    type = TransferFinishType.DOWNLOAD_AND_OPEN,
                    nodeName = autoPlayInfo?.nodeName,
                    nodeId = autoPlayInfo?.nodeHandle,
                    nodeLocalPath = autoPlayInfo?.localPath
                )
            } else if (totalDownloads > 0) {
                TransfersFinishedState(
                    type =
                    if (isDownloadForOffline) TransferFinishType.DOWNLOAD_OFFLINE
                    else TransferFinishType.DOWNLOAD,
                    numberFiles = totalDownloads
                )
            } else {
                null
            }?.let { transfersFinishState ->
                broadcastOfflineNodeAvailability(handle)
                broadcastTransfersFinishedUseCase(transfersFinishState)
            }

            resetTotalDownloadsUseCase()
            backgroundTransfers.clear()
            errorEBlocked = 0
            errorCount = 0
            alreadyDownloaded = 0
        }
        stopForeground()
    }

    private fun releaseLocks() {
        if (wifiLock.isHeld) try {
            wifiLock.release()
        } catch (ex: Exception) {
            Timber.e(ex)
        }
        if (wakeLock.isHeld) try {
            wakeLock.release()
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    private fun sendTakenDownAlert() {
        if (errorEBlocked <= 0) return
        val intent = Intent(BROADCAST_ACTION_INTENT_TAKEN_DOWN_FILES)
        intent.putExtra(NUMBER_FILES, errorEBlocked)
        intent.setPackage(applicationContext.packageName)
        sendBroadcast(intent)
    }

    private fun getDir(intent: Intent): File {
        val toDownloads = !intent.hasExtra(EXTRA_PATH)
        val destDir = if (toDownloads) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        } else {
            File(intent.getStringExtra(EXTRA_PATH))
        }
        return destDir
    }

    fun checkCurrentFile(document: MegaNode): Boolean {
        val current = currentFile ?: return false
        Timber.d("checkCurrentFile")
        if (current?.exists() == true
            && document.size == current.length() && FileUtil.isFileDownloadedLatest(
                current,
                document
            )
        ) {
            current.setReadable(true, false)
            return false
        }
        if (document.size > 1024L * 1024 * 1024 * 4) {
            Timber.d("Show size alert: %s", document.size)
            uiHandler.post {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.error_file_size_greater_than_4gb),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        return true
    }

    /*
     * Show download success notification
     */
    private suspend fun showCompleteNotification(handle: Long) {
        Timber.d("showCompleteNotification")
        val notificationTitle: String
        val size: String
        val totalDownloads = getTotalDownloadsUseCase() - backgroundTransfers.size
        if (alreadyDownloaded > 0 && errorCount > 0) {
            val totalNumber = totalDownloads + errorCount + alreadyDownloaded
            notificationTitle =
                resources.getQuantityString(
                    R.plurals.download_service_final_notification_with_details,
                    totalNumber,
                    totalDownloads,
                    totalNumber
                )
            val copiedString = resources.getQuantityString(
                R.plurals.already_downloaded_service,
                alreadyDownloaded,
                alreadyDownloaded
            )
            val errorString =
                resources.getQuantityString(R.plurals.upload_service_failed, errorCount, errorCount)
            size = "$copiedString, $errorString"
        } else if (alreadyDownloaded > 0) {
            val totalNumber = totalDownloads + alreadyDownloaded
            notificationTitle =
                resources.getQuantityString(
                    R.plurals.download_service_final_notification_with_details,
                    totalNumber,
                    totalDownloads,
                    totalNumber
                )
            size = resources.getQuantityString(
                R.plurals.already_downloaded_service,
                alreadyDownloaded,
                alreadyDownloaded
            )
        } else if (errorCount > 0) {
            sendTakenDownAlert()
            val totalNumber = totalDownloads + errorCount
            notificationTitle =
                resources.getQuantityString(
                    R.plurals.download_service_final_notification_with_details,
                    totalNumber,
                    totalDownloads,
                    totalNumber
                )
            size = resources.getQuantityString(
                R.plurals.download_service_failed,
                errorCount,
                errorCount
            )
        } else {
            notificationTitle =
                resources.getQuantityString(
                    R.plurals.download_service_final_notification,
                    totalDownloads,
                    totalDownloads
                )
            val totalBytes = Util.getSizeString(getTotalDownloadedBytesUseCase(), this)
            size = getString(R.string.general_total_size, totalBytes)
        }
        val intent = Intent(applicationContext, ManagerActivity::class.java)
        intent.action = Constants.ACTION_SHOW_TRANSFERS
        intent.putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.COMPLETED_TAB)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (totalDownloads != 1) {
            Timber.d("Show notification")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                    Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.setShowBadge(true)
                channel.setSound(null, null)
                mNotificationManager.createNotificationChannel(channel)
                val mBuilderCompatO = NotificationCompat.Builder(
                    applicationContext, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
                )
                mBuilderCompatO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true).setTicker(notificationTitle)
                    .setContentTitle(notificationTitle).setContentText(size)
                    .setOngoing(false)
                mNotificationManager.notify(
                    Constants.NOTIFICATION_DOWNLOAD_FINAL,
                    mBuilderCompatO.build()
                )
            } else {
                val builder = NotificationCompat.Builder(this)
                builder
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true).setTicker(notificationTitle)
                    .setContentTitle(notificationTitle).setContentText(size)
                    .setOngoing(false)
                mNotificationManager.notify(
                    Constants.NOTIFICATION_DOWNLOAD_FINAL,
                    builder.build()
                )
            }
        } else {
            try {
                val autoPlayEnabled = java.lang.Boolean.parseBoolean(dbH.autoPlayEnabled)
                if (downloadForPreview || openFile && autoPlayEnabled) {

                    val path = FileUtil.getLocalFile(
                        megaApi.getNodeByHandle(handle)
                    )
                    currentFile?.let { file ->
                        val fileLocalPath: String = path ?: file.absolutePath
                        currentDocument?.let {
                            autoPlayInfo = AutoPlayInfo(
                                it.name,
                                it.handle,
                                fileLocalPath,
                                true
                            )
                            Timber.d("Both openFile and autoPlayEnabled are true")
                        }
                    }


                    var fromMV = false
                    if (fromMediaViewers.containsKey(handle)) {
                        val result = fromMediaViewers[handle]
                        fromMV = result != null && result
                    }
                    if (MimeTypeList.typeForName(currentFile?.name).isPdf) {
                        Timber.d("Pdf file")
                        if (fromMV) {
                            Timber.d("Show notification")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val channel =
                                    NotificationChannel(
                                        Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                                        Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                                        NotificationManager.IMPORTANCE_DEFAULT
                                    )
                                channel.setShowBadge(true)
                                channel.setSound(null, null)
                                mNotificationManager.createNotificationChannel(channel)
                                val mBuilderCompatO = NotificationCompat.Builder(
                                    applicationContext, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
                                )
                                mBuilderCompatO
                                    .setSmallIcon(R.drawable.ic_stat_notify)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true).setTicker(notificationTitle)
                                    .setContentTitle(notificationTitle).setContentText(size)
                                    .setOngoing(false)
                                mNotificationManager.notify(
                                    Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                    mBuilderCompatO.build()
                                )
                            } else {
                                val builder = NotificationCompat.Builder(this)
                                builder
                                    .setSmallIcon(R.drawable.ic_stat_notify)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true).setTicker(notificationTitle)
                                    .setContentTitle(notificationTitle).setContentText(size)
                                    .setOngoing(false)
                                mNotificationManager.notify(
                                    Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                    builder.build()
                                )
                            }
                        }
                    } else if (MimeTypeList.typeForName(currentFile?.name).isVideoMimeType || MimeTypeList.typeForName(
                            currentFile?.name
                        ).isAudio
                    ) {
                        Timber.d("Video/Audio file")
                        if (fromMV) {
                            Timber.d("Show notification")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val channel =
                                    NotificationChannel(
                                        Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                                        Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                                        NotificationManager.IMPORTANCE_DEFAULT
                                    )
                                channel.setShowBadge(true)
                                channel.setSound(null, null)
                                mNotificationManager.createNotificationChannel(channel)
                                val mBuilderCompatO = NotificationCompat.Builder(
                                    applicationContext, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
                                )
                                mBuilderCompatO
                                    .setSmallIcon(R.drawable.ic_stat_notify)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true).setTicker(notificationTitle)
                                    .setContentTitle(notificationTitle).setContentText(size)
                                    .setOngoing(false)
                                mNotificationManager.notify(
                                    Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                    mBuilderCompatO.build()
                                )
                            } else {
                                val builder = NotificationCompat.Builder(this)
                                builder
                                    .setSmallIcon(R.drawable.ic_stat_notify)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true).setTicker(notificationTitle)
                                    .setContentTitle(notificationTitle).setContentText(size)
                                    .setOngoing(false)
                                mNotificationManager.notify(
                                    Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                    builder.build()
                                )
                            }
                        }
                    } else if (MimeTypeList.typeForName(currentFile?.name).isImage) {
                        Timber.d("Download is IMAGE")
                        if (fromMV) {
                            Timber.d("Show notification")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val channel =
                                    NotificationChannel(
                                        Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                                        Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                                        NotificationManager.IMPORTANCE_DEFAULT
                                    )
                                channel.setShowBadge(true)
                                channel.setSound(null, null)
                                mNotificationManager.createNotificationChannel(channel)
                                val mBuilderCompatO = NotificationCompat.Builder(
                                    applicationContext, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
                                )
                                mBuilderCompatO
                                    .setSmallIcon(R.drawable.ic_stat_notify)
                                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true).setTicker(notificationTitle)
                                    .setContentTitle(notificationTitle).setContentText(size)
                                    .setOngoing(false)
                                mNotificationManager.notify(
                                    Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                    mBuilderCompatO.build()
                                )
                            } else {
                                val builder = NotificationCompat.Builder(this)
                                builder
                                    .setSmallIcon(R.drawable.ic_stat_notify)
                                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true).setTicker(notificationTitle)
                                    .setContentTitle(notificationTitle).setContentText(size)
                                    .setOngoing(false)
                                mNotificationManager.notify(
                                    Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                    builder.build()
                                )
                            }
                        }
                    } else {
                        Timber.d("Show notification")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channel =
                                NotificationChannel(
                                    Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                                    Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                                    NotificationManager.IMPORTANCE_DEFAULT
                                )
                            channel.setShowBadge(true)
                            channel.setSound(null, null)
                            mNotificationManager.createNotificationChannel(channel)
                            val mBuilderCompatO = NotificationCompat.Builder(
                                applicationContext, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
                            )
                            mBuilderCompatO
                                .setSmallIcon(R.drawable.ic_stat_notify)
                                .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true).setTicker(notificationTitle)
                                .setContentTitle(notificationTitle).setContentText(size)
                                .setOngoing(false)
                            mNotificationManager.notify(
                                Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                mBuilderCompatO.build()
                            )
                        } else {
                            val builder = NotificationCompat.Builder(this)
                            builder
                                .setSmallIcon(R.drawable.ic_stat_notify)
                                .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true).setTicker(notificationTitle)
                                .setContentTitle(notificationTitle).setContentText(size)
                                .setOngoing(false)
                            mNotificationManager.notify(
                                Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                builder.build()
                            )
                        }
                    }
                } else {
                    openFile = true //Set the openFile to the default
                    Timber.d("Show notification")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channel =
                            NotificationChannel(
                                Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                                Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                                NotificationManager.IMPORTANCE_DEFAULT
                            )
                        channel.setShowBadge(true)
                        channel.setSound(null, null)
                        mNotificationManager.createNotificationChannel(channel)
                        val mBuilderCompatO = NotificationCompat.Builder(
                            applicationContext, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
                        )
                        mBuilderCompatO
                            .setSmallIcon(R.drawable.ic_stat_notify)
                            .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true).setTicker(notificationTitle)
                            .setContentTitle(notificationTitle).setContentText(size)
                            .setOngoing(false)
                        mNotificationManager.notify(
                            Constants.NOTIFICATION_DOWNLOAD_FINAL,
                            mBuilderCompatO.build()
                        )
                    } else {
                        val builder = NotificationCompat.Builder(this)
                        builder
                            .setSmallIcon(R.drawable.ic_stat_notify)
                            .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true).setTicker(notificationTitle)
                            .setContentTitle(notificationTitle).setContentText(size)
                            .setOngoing(false)
                        mNotificationManager.notify(
                            Constants.NOTIFICATION_DOWNLOAD_FINAL,
                            builder.build()
                        )
                    }
                }
            } catch (e: Exception) {
                openFile = true //Set the openFile to the default
                Timber.e(e)
                Timber.d("Show notification")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                        Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                    channel.setShowBadge(true)
                    channel.setSound(null, null)
                    mNotificationManager.createNotificationChannel(channel)
                    val mBuilderCompatO = NotificationCompat.Builder(
                        applicationContext, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
                    )
                    mBuilderCompatO
                        .setSmallIcon(R.drawable.ic_stat_notify)
                        .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true).setTicker(notificationTitle)
                        .setContentTitle(notificationTitle).setContentText(size)
                        .setOngoing(false)
                    mNotificationManager.notify(
                        Constants.NOTIFICATION_DOWNLOAD_FINAL,
                        mBuilderCompatO.build()
                    )
                } else {
                    val builder = NotificationCompat.Builder(this)
                    builder
                        .setSmallIcon(R.drawable.ic_stat_notify)
                        .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true).setTicker(notificationTitle)
                        .setContentTitle(notificationTitle).setContentText(size)
                        .setOngoing(false)
                    mNotificationManager.notify(
                        Constants.NOTIFICATION_DOWNLOAD_FINAL,
                        builder.build()
                    )
                }
            }
        }
    }

    /*
     * Update notification download progress
     */
    @SuppressLint("NewApi")
    private suspend fun updateProgressNotification(pausedTransfers: Boolean = false) {
        // make sure app running to avoid DeadSystemException when show notification
        coroutineContext.ensureActive()
        val pendingTransfers = getNumPendingDownloadsNonBackgroundUseCase()
        val totalTransfers = getTotalDownloadsUseCase() - backgroundTransfers.size
        val totalSizePendingTransfer = getTotalDownloadBytesUseCase()
        val totalSizeTransferred = getTotalDownloadedBytesUseCase()
        val update: Boolean
        if (isOverQuota) {
            Timber.d("Overquota flag! is TRUE")
            if (downloadedBytesToOverQuota <= totalSizeTransferred) {
                update = false
            } else {
                update = true
                Timber.d("Change overquota flag")
                isOverQuota = false
            }
        } else {
            Timber.d("NOT overquota flag")
            update = true
        }
        if (update) {
            /* refresh UI every 1 seconds to avoid too much workload on main thread
             * while in paused status, the update should not be avoided*/
            if (!isOverQuota) {
                val now = System.currentTimeMillis()
                lastUpdated =
                    if (now - lastUpdated > ON_TRANSFER_UPDATE_REFRESH_MILLIS || dbH.transferQueueStatus) {
                        now
                    } else {
                        return
                    }
            }
            val progressPercent =
                Math.round(totalSizeTransferred.toDouble() / totalSizePendingTransfer * 100).toInt()
            Timber.d("Progress: $progressPercent%")
            showRating(totalSizePendingTransfer, getCurrentDownloadSpeedUseCase())
            var message: String? = ""
            message = if (totalTransfers == 0) {
                getString(R.string.download_preparing_files)
            } else {
                val inProgress =
                    if (pendingTransfers == 0) totalTransfers else totalTransfers - pendingTransfers + 1
                if (pausedTransfers) {
                    getString(
                        R.string.download_service_notification_paused,
                        inProgress,
                        totalTransfers
                    )
                } else {
                    getString(
                        R.string.download_service_notification,
                        inProgress,
                        totalTransfers
                    )
                }
            }
            val info = Util.getProgressSize(
                this@DownloadService,
                totalSizeTransferred,
                totalSizePendingTransfer
            )
            var notification: Notification? = null
            val contentText = getString(R.string.download_touch_to_show)
            val intent = Intent(this@DownloadService, ManagerActivity::class.java)
            intent.action = Constants.ACTION_SHOW_TRANSFERS
            intent.putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.PENDING_TAB)
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                this@DownloadService,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (mNotificationManager.getNotificationChannel(Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID) == null) {
                    val channel = NotificationChannel(
                        Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                        Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    )
                    channel.setShowBadge(true)
                    channel.setSound(null, null)
                    mNotificationManager.createNotificationChannel(channel)
                }
                val mBuilderCompat = NotificationCompat.Builder(
                    applicationContext,
                    Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID
                )
                mBuilderCompat
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                    .setProgress(100, progressPercent, false)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true).setContentTitle(message).setSubText(info)
                    .setContentText(contentText)
                    .setOnlyAlertOnce(true)
                mBuilderCompat.build()
            } else {
                val mBuilder = Notification.Builder(this@DownloadService)
                mBuilder
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                    .setProgress(100, progressPercent, false)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true).setContentTitle(message).setContentInfo(info)
                    .setContentText(contentText)
                    .setOnlyAlertOnce(true)
                mBuilder.build()
            }
            if (!isForeground) {
                Timber.d("Starting foreground!")
                isForeground = try {
                    startForeground(Constants.NOTIFICATION_DOWNLOAD, notification)
                    true
                } catch (e: Exception) {
                    false
                }
            } else {
                try {
                    mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD, notification)
                } catch (e: Exception) {
                    Timber.w("Exception updating notification progress: %s", e.message)
                }
            }
        }
    }

    /**
     * Determine if should show the rating page to users
     *
     * @param total                the total size of uploading file
     * @param currentDownloadSpeed current downloading speed
     */
    private fun showRating(total: Long, currentDownloadSpeed: Int) {
        if (!isRatingShowed) {
            RatingHandlerImpl(this)
                .showRatingBaseOnSpeedAndSize(
                    total,
                    currentDownloadSpeed.toLong()
                ) { isRatingShowed = true }
        }
    }

    private fun cancel() {
        Timber.d("cancel")
        canceled = true
        stopForeground()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private suspend fun doOnTransferStart(transfer: Transfer) = withContext(ioDispatcher) {
        Timber.d("Download start: ${transfer.nodeHandle}")
        if (transfer.isStreamingTransfer || transfer.isVoiceClip()) return@withContext
        if (transfer.isBackgroundTransfer()) {
            backgroundTransfers.add(transfer.tag)
            return@withContext
        }
        if (transfer.isSDCardDownload()) {
            insertSdTransferUseCase(
                SdTransfer(
                    transfer.tag,
                    transfer.fileName,
                    Util.getSizeString(transfer.totalBytes, this@DownloadService),
                    transfer.nodeHandle.toString(),
                    transfer.localPath,
                    transfer.appData
                )
            )
        }
        transfersManagement.checkScanningTransfer(transfer, TransfersManagement.Check.ON_START)
        transfersCount++
        updateProgressNotification()
    }

    private suspend fun doOnTransferFinish(
        transfer: Transfer,
        error: MegaException?,
    ) = withContext(ioDispatcher) {
        Timber.d("Node handle: " + transfer.nodeHandle + ", Type = " + transfer.transferType)
        if (transfer.isStreamingTransfer) return@withContext
        if (error?.errorCode == MegaError.API_EBUSINESSPASTDUE) {
            broadcastBusinessAccountExpiredUseCase()
        }
        transfersManagement.checkScanningTransfer(transfer, TransfersManagement.Check.ON_FINISH)
        val isVoiceClip = transfer.isVoiceClip()
        val isBackgroundTransfer = transfer.isBackgroundTransfer()
        if (!isVoiceClip && !isBackgroundTransfer) transfersCount--
        val path = transfer.localPath
        val sdCardDownloadAppData = transfer.getSDCardDownloadAppData()
        val targetPath = sdCardDownloadAppData?.targetPath
        if (!transfer.isFolderTransfer) {
            if (!isVoiceClip && !isBackgroundTransfer) {
                val completedTransfer =
                    AndroidCompletedTransfer(transfer, error, this@DownloadService)
                if (!TextUtil.isTextEmpty(targetPath)) {
                    completedTransfer.path = targetPath
                }
                addCompletedTransferUseCase(
                    legacyCompletedTransferMapper(
                        completedTransfer
                    )
                )
            }
            if (transfer.state == TransferState.STATE_FAILED) {
                transfersManagement.setAreFailedTransfers(true)
            }
            if (!isVoiceClip && !isBackgroundTransfer) {
                updateProgressNotification()
            }
        }
        if (canceled) {
            releaseLocks()
            Timber.d("Download canceled: %s", transfer.nodeHandle)
            if (isVoiceClip) {
                resultTransfersVoiceClip(
                    transfer.nodeHandle,
                    Constants.ERROR_VOICE_CLIP_TRANSFER
                )
                val localFile = buildVoiceClipFile(transfer.fileName)
                if (FileUtil.isFileAvailable(localFile)) {
                    Timber.d("Delete own voiceclip : exists")
                    localFile?.delete()
                }
            } else {
                val file = File(transfer.localPath)
                file.delete()
            }
            cancel()
        } else {
            if (error == null) {
                Timber.d("Download OK - Node handle: %s", transfer.nodeHandle)
                if (isVoiceClip) {
                    resultTransfersVoiceClip(
                        transfer.nodeHandle,
                        Constants.SUCCESSFUL_VOICE_CLIP_TRANSFER
                    )
                }

                //need to move downloaded file to a location on sd card.
                if (targetPath != null) {
                    val source = File(path)
                    try {
                        val sdCardOperator = SDCardOperator(this@DownloadService)
                        val isSuccess = sdCardOperator.moveDownloadedFileToDestinationPath(
                            source,
                            targetPath,
                            sdCardDownloadAppData.targetUri,
                        )
                        if (isSuccess) {
                            deleteSdTransferByTagUseCase(transfer.tag)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error moving file to the sd card path.")
                    }
                }
                //To update thumbnails for videos
                if (FileUtil.isVideoFile(transfer.localPath)) {
                    Timber.d("Is video!")
                    val videoNode = megaApi.getNodeByHandle(transfer.nodeHandle)
                    if (videoNode != null) {
                        if (!videoNode.hasThumbnail()) {
                            Timber.d("The video has not thumb")
                            ThumbnailUtils.createThumbnailVideo(
                                this@DownloadService,
                                path,
                                megaApi,
                                transfer.nodeHandle
                            )
                        }
                    } else {
                        Timber.w("videoNode is NULL")
                    }
                } else {
                    Timber.d("NOT video!")
                }
                if (!TextUtil.isTextEmpty(path)) {
                    FileUtil.sendBroadcastToUpdateGallery(this@DownloadService, File(path))
                }
                storeToAdvancedDevices[transfer.nodeHandle]?.let { transfersUri ->
                    Timber.d("Now copy the file to the SD Card")
                    openFile = false
                    val node = megaApi.getNodeByHandle(transfer.nodeHandle)
                    alterDocument(transfersUri, node?.name)
                }
                if (isOfflineTransferUseCase(transfer)) {
                    Timber.d("It is Offline file")
                    saveOfflineNodeInformationUseCase(NodeId(transfer.nodeHandle))
                    openFile = false
                    refreshOfflineFragment()
                    refreshSettingsFragment()
                }
            }
            error?.let {
                Timber.e("Download ERROR: %s", transfer.nodeHandle)
                if (isVoiceClip) {
                    resultTransfersVoiceClip(
                        transfer.nodeHandle,
                        Constants.ERROR_VOICE_CLIP_TRANSFER
                    )
                    val localFile = buildVoiceClipFile(transfer.fileName)
                    if (FileUtil.isFileAvailable(localFile)) {
                        Timber.d("Delete own voice clip : exists")
                        localFile?.delete()
                    }
                } else {
                    if (error.errorCode == MegaError.API_EBLOCKED) {
                        errorEBlocked++
                    }
                    if (!transfer.isFolderTransfer) {
                        errorCount++
                    }
                    if (!TextUtil.isTextEmpty(transfer.localPath)) {
                        val file = File(transfer.localPath)
                        file.delete()
                    }
                }
            }

        }
        if (isVoiceClip || isBackgroundTransfer) return@withContext
        if (getNumPendingDownloadsNonBackgroundUseCase() <= 0 && transfersCount == 0) {
            onQueueComplete(transfer.nodeHandle)
        }
    }

    private fun resultTransfersVoiceClip(nodeHandle: Long, result: Int) {
        Timber.d("nodeHandle =  $nodeHandle, the result is $result")
        val intent = Intent(Constants.BROADCAST_ACTION_INTENT_VOICE_CLIP_DOWNLOADED)
        intent.putExtra(INTENT_EXTRA_NODE_HANDLE, nodeHandle)
        intent.putExtra(Constants.EXTRA_RESULT_TRANSFER, result)
        intent.setPackage(applicationContext.packageName)
        sendBroadcast(intent)
    }

    private fun alterDocument(uri: Uri, fileName: String?) {
        Timber.d("alterUri")
        try {
            val tempFolder = getCacheFolder(CacheFolderManager.TEMPORARY_FOLDER)
            if (!FileUtil.isFileAvailable(tempFolder)) return
            val sourceLocation = tempFolder?.absolutePath + File.separator + fileName

            with(contentResolver.openFileDescriptor(uri, "w")) {
                val fileOutputStream = FileOutputStream(this?.fileDescriptor)
                val inputStream: InputStream = FileInputStream(sourceLocation)

                // Copy the bits from instream to outstream
                val buf = ByteArray(1024)
                var len: Int
                while (inputStream.read(buf).also { len = it } > 0) {
                    fileOutputStream.write(buf, 0, len)
                }
                inputStream.close()

                // Let the document provider know you're done by closing the stream.
                fileOutputStream.close()
            }

            val deleteTemp = File(sourceLocation)
            deleteTemp.delete()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private suspend fun doOnTransferUpdate(transfer: Transfer) = withContext(ioDispatcher) {
        if (canceled) {
            Timber.d("Transfer cancel: %s", transfer.nodeHandle)
            releaseLocks()
            runCatching { cancelTransferByTagUseCase(transfer.tag) }
                .onFailure { Timber.w("Exception canceling transfer: $it") }
            cancel()
            return@withContext
        }
        if (transfer.isStreamingTransfer || transfer.isVoiceClip()) return@withContext
        if (transfer.isBackgroundTransfer()) {
            backgroundTransfers.add(transfer.tag)
            return@withContext
        }
        transfersManagement.checkScanningTransfer(transfer, TransfersManagement.Check.ON_UPDATE)
        if (!transfer.isFolderTransfer) {
            updateProgressNotification()
        }
        if (!transfersManagement.isOnTransferOverQuota() && transfersManagement.hasNotToBeShowDueToTransferOverQuota()) {
            transfersManagement.setHasNotToBeShowDueToTransferOverQuota(false)
        }
    }

    private suspend fun doOnTransferTemporaryError(
        transfer: Transfer,
        e: MegaException?,
    ) = withContext(ioDispatcher) {
        Timber.w(
            "Download Temporary Error - Node Handle: ${transfer.nodeHandle} Error: ${e?.errorCode} ${e?.errorString}"
        )
        if (transfer.isStreamingTransfer || transfer.isBackgroundTransfer()) {
            return@withContext
        }
        if (e is QuotaExceededMegaException) {
            if (e.value != 0L) {
                Timber.w("TRANSFER OVERQUOTA ERROR: %s", e.errorCode)
                checkTransferOverQuota(true)
                downloadedBytesToOverQuota = getTotalDownloadedBytesUseCase()
                isOverQuota = true
            }
        }
    }

    /**
     * Checks if should show transfer over quota warning.
     * If so, sends a broadcast to show it in the current view.
     *
     * @param isCurrentOverQuota true if the overquota is currently received, false otherwise
     */
    private suspend fun checkTransferOverQuota(isCurrentOverQuota: Boolean) {
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            broadcastTransferOverQuotaUseCase(isCurrentOverQuota)
        } else if (!transfersManagement.isTransferOverQuotaNotificationShown) {
            transfersManagement.isTransferOverQuotaNotificationShown = true
            isForeground = false
            stopForeground(true)
            mNotificationManager.cancel(Constants.NOTIFICATION_DOWNLOAD)
            transfersManagement.isTransferOverQuotaBannerShown = true
            TransferOverQuotaNotification.show(applicationContext)
        }
    }

    private fun acquireLocks() {
        if (!wakeLock.isHeld) wakeLock.acquire()
        if (!wifiLock.isHeld) wifiLock.acquire()
    }

    private fun refreshOfflineFragment() {
        sendBroadcast(
            Intent(OfflineFragment.REFRESH_OFFLINE_FILE_LIST).setPackage(
                applicationContext.packageName
            )
        )
    }

    private fun refreshSettingsFragment() {
        val intent = Intent(Constants.BROADCAST_ACTION_INTENT_SETTINGS_UPDATED)
        intent.action = ACTION_REFRESH_CLEAR_OFFLINE_SETTING
        intent.setPackage(applicationContext.packageName)
        sendBroadcast(intent)
    }

    companion object {
        // Action to stop download
        const val ACTION_CANCEL = "CANCEL_DOWNLOAD"
        const val EXTRA_SIZE = "DOCUMENT_SIZE"
        const val EXTRA_HASH = "DOCUMENT_HASH"
        const val EXTRA_DOWNLOAD_TO_SDCARD = "download_to_sdcard"
        const val EXTRA_TARGET_PATH = "target_path"
        const val EXTRA_TARGET_URI = "target_uri"
        const val EXTRA_PATH = "SAVE_PATH"
        const val EXTRA_FOLDER_LINK = "FOLDER_LINK"
        const val EXTRA_FROM_MV = "fromMV"
        const val EXTRA_OPEN_FILE = "OPEN_FILE"
        const val EXTRA_CONTENT_URI = "CONTENT_URI"
        const val EXTRA_DOWNLOAD_FOR_PREVIEW = "EXTRA_DOWNLOAD_FOR_PREVIEW"
        const val EXTRA_DOWNLOAD_BY_OPEN_WITH = "EXTRA_DOWNLOAD_BY_OPEN_WITH"
        const val EXTRA_DOWNLOAD_FOR_OFFLINE = "EXTRA_DOWNLOAD_FOR_OFFLINE"
        private var errorEBlocked = 0
        private const val ON_TRANSFER_UPDATE_REFRESH_MILLIS = 1000
    }
}
