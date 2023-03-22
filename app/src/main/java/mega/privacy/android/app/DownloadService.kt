package mega.privacy.android.app

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.components.saver.AutoPlayInfo
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_REFRESH_CLEAR_OFFLINE_SETTING
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_SHOWSNACKBAR_TRANSFERS_FINISHED
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_TAKEN_DOWN_FILES
import mega.privacy.android.app.constants.BroadcastConstants.DOWNLOAD_FILE_AND_OPEN_FOR_PREVIEW
import mega.privacy.android.app.constants.BroadcastConstants.DOWNLOAD_TRANSFER
import mega.privacy.android.app.constants.BroadcastConstants.DOWNLOAD_TRANSFER_OPEN
import mega.privacy.android.app.constants.BroadcastConstants.IS_OPEN_WITH
import mega.privacy.android.app.constants.BroadcastConstants.NODE_HANDLE
import mega.privacy.android.app.constants.BroadcastConstants.NODE_LOCAL_PATH
import mega.privacy.android.app.constants.BroadcastConstants.NODE_NAME
import mega.privacy.android.app.constants.BroadcastConstants.NUMBER_FILES
import mega.privacy.android.app.constants.BroadcastConstants.OFFLINE_AVAILABLE
import mega.privacy.android.app.constants.BroadcastConstants.TRANSFER_TYPE
import mega.privacy.android.app.constants.EventConstants.EVENT_FINISH_SERVICE_IF_NO_TRANSFERS
import mega.privacy.android.app.data.extensions.isBackgroundTransfer
import mega.privacy.android.app.data.extensions.isVoiceClipTransfer
import mega.privacy.android.app.fragments.offline.OfflineFragment
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.globalmanagement.TransfersManagement.Companion.addCompletedTransfer
import mega.privacy.android.app.globalmanagement.TransfersManagement.Companion.createInitialServiceNotification
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.notifications.TransferOverQuotaNotification
import mega.privacy.android.app.objects.SDTransfer
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.app.usecase.GetGlobalTransferUseCase
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.CacheFolderManager.buildVoiceClipFile
import mega.privacy.android.app.utils.CacheFolderManager.getCacheFolder
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaTransferUtils.getNumPendingDownloadsNonBackground
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.SDCardOperator
import mega.privacy.android.app.utils.SDCardUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.facade.INTENT_EXTRA_NODE_HANDLE
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.data.qualifier.MegaApiFolder
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetNumPendingDownloadsNonBackground
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.transfer.BroadcastTransferOverQuota
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

/**
 * Background service to download files
 */
@AndroidEntryPoint
internal class DownloadService : Service(), MegaRequestListenerInterface {

    @Inject
    lateinit var getGlobalTransferUseCase: GetGlobalTransferUseCase

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

    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    @IoDispatcher
    @Inject
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var getDownloadCount: GetNumPendingDownloadsNonBackground

    @Inject
    lateinit var rootNodeExistsUseCase: RootNodeExistsUseCase

    @Inject
    lateinit var broadcastTransferOverQuota: BroadcastTransferOverQuota

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    private var errorCount = 0
    private var alreadyDownloaded = 0
    private var isForeground = false
    private var canceled = false
    private var openFile = true
    private var downloadForPreview = false
    private var downloadByOpenWith = false
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
    private var offlineNode: MegaNode? = null
    private var isLoggingIn = false
    private var lastUpdated: Long = 0
    private var intent: Intent? = null

    /**
     * the receiver and manager for the broadcast to listen to the pause event
     */
    private var pauseBroadcastReceiver: BroadcastReceiver? = null
    private val rxSubscriptions = CompositeDisposable()
    private val uiHandler = Handler(Looper.getMainLooper())

    // the flag to determine the rating dialog is showed for this download action
    private var isRatingShowed = false
    private var isDownloadForOffline = false

    /**
     * Contains the info of a node that to be opened in-app.
     */
    private var autoPlayInfo: AutoPlayInfo? = null
    private val stopServiceObserver = Observer { finish: Boolean ->
        if (finish && megaApi.numPendingDownloads == 0) {
            stopForeground()
        }
    }

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
        megaApi.addRequestListener(this@DownloadService)
        initialiseWifiLock()
        initialiseWakeLock()
        setReceivers()
        setRxSubscription()
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

    private fun setReceivers() {
        // delay 1 second to refresh the pause notification to prevent update is missed
        pauseBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Handler().postDelayed({ updateProgressNotification() },
                    TransfersManagement.WAIT_TIME_BEFORE_UPDATE)
            }
        }
        registerReceiver(pauseBroadcastReceiver,
            IntentFilter(Constants.BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION))
        LiveEventBus.get(EVENT_FINISH_SERVICE_IF_NO_TRANSFERS, Boolean::class.java)
            .observeForever(stopServiceObserver)
    }

    private fun setRxSubscription() {
        val subscription = getGlobalTransferUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ event: GetGlobalTransferUseCase.Result? ->
                when (event) {
                    is GetGlobalTransferUseCase.Result.OnTransferStart -> {
                        val transfer = event.transfer
                        doOnTransferStart(transfer)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({}) { t: Throwable? -> Timber.e(t) }
                    }
                    is GetGlobalTransferUseCase.Result.OnTransferUpdate -> {
                        val transfer = event.transfer
                        doOnTransferUpdate(transfer)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({}) { t: Throwable? -> Timber.e(t) }
                    }
                    is GetGlobalTransferUseCase.Result.OnTransferFinish -> {
                        val transfer = event.transfer
                        val error = event.error
                        doOnTransferFinish(transfer, error)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({}) { t: Throwable? -> Timber.e(t) }
                    }
                    is GetGlobalTransferUseCase.Result.OnTransferTemporaryError -> {
                        val transfer = event.transfer
                        val error = event.error
                        doOnTransferTemporaryError(transfer, error)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({}) { t: Throwable? -> Timber.e(t) }
                    }
                    is GetGlobalTransferUseCase.Result.OnTransferData -> {}
                    null -> {}
                }
            }) { t: Throwable? -> Timber.e(t) }
        rxSubscriptions.add(subscription)
    }

    private fun startForeground() {
        CoroutineScope(ioDispatcher).launch {
            if (getDownloadCount() > 0) {
                isForeground = kotlin.runCatching {
                    val notification = createInitialNotification()
                    startForeground(Constants.NOTIFICATION_DOWNLOAD,
                        notification)
                }.fold(
                    onSuccess = { true },
                    onFailure = {
                        Timber.w(it)
                        false
                    }
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun createInitialNotification() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createInitialServiceNotification(
                notificationChannelId = Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                notificationChannelName = Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                mNotificationManager = mNotificationManager,
                mBuilderCompat = NotificationCompat.Builder(this@DownloadService,
                    Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
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
        megaApi.removeRequestListener(this)
        megaChatApi.saveCurrentState()
        // remove all the generated folders in cache folder on SD card.
        val fs = externalCacheDirs
        if (fs.size > 1 && fs[1] != null) {
            FileUtil.purgeDirectory(fs[1])
        }
        unregisterReceiver(pauseBroadcastReceiver)
        rxSubscriptions.clear()
        stopForeground()
        LiveEventBus.get(EVENT_FINISH_SERVICE_IF_NO_TRANSFERS, Boolean::class.java)
            .removeObserver(stopServiceObserver)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand")
        canceled = false
        if (intent.action == ACTION_CANCEL) {
            Timber.d("Cancel intent")
            canceled = true
            megaApi.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD)
            return START_NOT_STICKY
        }
        rxSubscriptions.add(Single.just(intent)
            .observeOn(Schedulers.single())
            .subscribe({ intent: Intent -> onHandleIntent(intent) }) { t: Throwable? -> Timber.e(t) })
        return START_NOT_STICKY
    }

    private fun onHandleIntent(intent: Intent) {
        Timber.d("onHandleIntent")
        this.intent = intent
        if (intent.action != null && intent.action == Constants.ACTION_RESTART_SERVICE) {
            val transferData = megaApi.getTransferData(null)
            if (transferData == null) {
                stopForeground()
                return
            }
            val uploadsInProgress = transferData.numDownloads
            for (i in 0 until uploadsInProgress) {
                val transfer =
                    megaApi.getTransferByTag(transferData.getDownloadTag(i)) ?: continue
                if (!transfer.isVoiceClipTransfer() && !transfer.isBackgroundTransfer()) {
                    transfersCount++
                }
            }
            if (transfersCount > 0) {
                updateProgressNotification()
            } else {
                stopForeground()
            }
            return
        }

        isDownloadForOffline = intent.getBooleanExtra(EXTRA_DOWNLOAD_FOR_OFFLINE, false)

        openFile = intent.getBooleanExtra(EXTRA_OPEN_FILE, true)
        downloadForPreview = intent.getBooleanExtra(EXTRA_DOWNLOAD_FOR_PREVIEW, false)
        downloadByOpenWith = intent.getBooleanExtra(EXTRA_DOWNLOAD_BY_OPEN_WITH, false)
        type = intent.getStringExtra(Constants.EXTRA_TRANSFER_TYPE)

        // we don't need to create ioDispatcher here, in already run in Background Thread by rx java setup
        runBlocking {
            processIntent(intent)
        }
    }

    private suspend fun processIntent(
        intent: Intent,
    ) {

        if (addPendingIntentIfNotLoggedIn(intent)) return
        if (handlePublicNode(intent)) return

        val isFolderLink = intent.getBooleanExtra(EXTRA_FOLDER_LINK, false)
        val fromMV = intent.getBooleanExtra(EXTRA_FROM_MV, false)
        Timber.d("fromMV: %s", fromMV)
        val contentUri = intent.getStringExtra(EXTRA_CONTENT_URI)?.let { Uri.parse(it) }
        val highPriority = intent.getBooleanExtra(Constants.HIGH_PRIORITY_TRANSFER, false)
        val node: MegaNode = getNodeForIntent(intent, isFolderLink) ?: return

        fromMediaViewers[node.handle] = fromMV
        currentDir = getDir(intent)
        currentDir?.mkdirs()
        currentFile = if (currentDir?.isDirectory == true) {
            File(currentDir,
                megaApi.escapeFsIncompatible(node.name,
                    currentDir?.absolutePath + Constants.SEPARATOR))
        } else {
            currentDir
        }
        var appData = getSDCardAppData(intent)
        if (currentDocument?.let { checkCurrentFile(it) } != true) {
            Timber.d("checkCurrentFile == false")
            alreadyDownloaded++
            if (megaApi.getNumPendingDownloadsNonBackground() == 0) {
                onQueueComplete(node.handle)
            }
            return
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
                val localFingerprint = megaApi.getFingerprint(currentFile?.absolutePath)
                val megaFingerprint = node.fingerprint
                if (!TextUtil.isTextEmpty(localFingerprint)
                    && !TextUtil.isTextEmpty(megaFingerprint)
                    && localFingerprint == megaFingerprint
                ) {
                    Timber.d("Delete the old version")
                    currentFile?.delete()
                }
            }
            if (currentDir?.absolutePath?.contains(OfflineUtils.OFFLINE_DIR) == true) {
                //			Save for offline: do not open when finishes
                openFile = false
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
            val token = transfersManagement.addScanningTransfer(MegaTransfer.TYPE_DOWNLOAD,
                localPath, node, node.isFolder)
            megaApi.startDownload(currentDocument,
                localPath,
                node.name,
                appData,
                highPriority,
                token)
        } else {
            Timber.w("currentDir is not a directory")
        }
    }

    private fun handlePublicNode(intent: Intent): Boolean {
        val url = intent.getStringExtra(EXTRA_URL) ?: return false
        Timber.d("Public node")
        val path = intent.getStringExtra(EXTRA_PATH) ?: return false
        currentDir = File(path)
        currentDir?.mkdirs()
        megaApi.getPublicNode(url)
        return true
    }

    private suspend fun addPendingIntentIfNotLoggedIn(intent: Intent): Boolean {
        val credentials = dbH.credentials
        if (credentials != null) {
            val gSession = credentials.session
            if (!rootNodeExistsUseCase()) {
                isLoggingIn = MegaApplication.isLoggingIn
                if (!isLoggingIn) {
                    isLoggingIn = true
                    MegaApplication.isLoggingIn = isLoggingIn
                    ChatUtil.initMegaChatApi(gSession)
                    pendingIntents.add(intent)
                    if (type?.contains(Constants.APP_DATA_VOICE_CLIP) == true && type?.contains(
                            Constants.APP_DATA_BACKGROUND_TRANSFER) == true
                    ) {
                        updateProgressNotification()
                    }
                    megaApi.fastLogin(gSession)
                    return true
                } else {
                    Timber.w("Another login is processing")
                }
                pendingIntents.add(intent)
                return true
            }
        }
        return false
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

    private fun onQueueComplete(handle: Long) {
        Timber.d("onQueueComplete")
        releaseLocks()
        showCompleteNotification(handle)
        stopForeground()
        val pendingDownloads = megaApi.getNumPendingDownloadsNonBackground()
        Timber.d("onQueueComplete: total of files before reset %s", pendingDownloads)
        if (pendingDownloads <= 0) {
            Timber.d("onQueueComplete: reset total downloads")
            // When download a single file by tapping it, and auto play is enabled.
            val totalDownloads = megaApi.totalDownloads - backgroundTransfers.size
            if (totalDownloads == 1 && autoPlayInfo != null && downloadForPreview) {
                // If the file is Microsoft file, send the corresponding broadcast
                sendBroadcast(Intent(BROADCAST_ACTION_INTENT_SHOWSNACKBAR_TRANSFERS_FINISHED)
                    .putExtra(TRANSFER_TYPE, DOWNLOAD_FILE_AND_OPEN_FOR_PREVIEW)
                    .putExtra(NODE_NAME, autoPlayInfo?.nodeName)
                    .putExtra(NODE_HANDLE, autoPlayInfo?.nodeHandle)
                    .putExtra(NUMBER_FILES, 1)
                    .putExtra(NODE_LOCAL_PATH, autoPlayInfo?.localPath)
                    .putExtra(IS_OPEN_WITH, downloadByOpenWith))
            } else if (totalDownloads == 1 && java.lang.Boolean.parseBoolean(dbH.autoPlayEnabled) && autoPlayInfo != null) {
                sendBroadcast(Intent(BROADCAST_ACTION_INTENT_SHOWSNACKBAR_TRANSFERS_FINISHED)
                    .putExtra(TRANSFER_TYPE, DOWNLOAD_TRANSFER_OPEN)
                    .putExtra(NODE_NAME, autoPlayInfo?.nodeName)
                    .putExtra(NODE_HANDLE, autoPlayInfo?.nodeHandle)
                    .putExtra(NUMBER_FILES, 1)
                    .putExtra(NODE_LOCAL_PATH, autoPlayInfo?.localPath))
            } else if (totalDownloads > 0) {
                val intent: Intent = Intent(BROADCAST_ACTION_INTENT_SHOWSNACKBAR_TRANSFERS_FINISHED)
                    .putExtra(TRANSFER_TYPE, DOWNLOAD_TRANSFER)
                    .putExtra(NUMBER_FILES, totalDownloads)
                if (isDownloadForOffline) {
                    intent.putExtra(OFFLINE_AVAILABLE, true)
                }
                sendBroadcast(intent)
            }
            megaApi.resetTotalDownloads()
            backgroundTransfers.clear()
            errorEBlocked = 0
            errorCount = 0
            alreadyDownloaded = 0
        }
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
                document)
        ) {
            current.setReadable(true, false)
            return false
        }
        if (document.size > 1024L * 1024 * 1024 * 4) {
            Timber.d("Show size alert: %s", document.size)
            uiHandler.post {
                Toast.makeText(applicationContext,
                    getString(R.string.error_file_size_greater_than_4gb),
                    Toast.LENGTH_LONG).show()
            }
        }
        return true
    }

    /*
     * Show download success notification
     */
    private fun showCompleteNotification(handle: Long) {
        Timber.d("showCompleteNotification")
        val notificationTitle: String
        val size: String
        val totalDownloads = megaApi.totalDownloads - backgroundTransfers.size
        if (alreadyDownloaded > 0 && errorCount > 0) {
            val totalNumber = totalDownloads + errorCount + alreadyDownloaded
            notificationTitle =
                resources.getQuantityString(R.plurals.download_service_final_notification_with_details,
                    totalNumber,
                    totalDownloads,
                    totalNumber)
            val copiedString = resources.getQuantityString(R.plurals.already_downloaded_service,
                alreadyDownloaded,
                alreadyDownloaded)
            val errorString =
                resources.getQuantityString(R.plurals.upload_service_failed, errorCount, errorCount)
            size = "$copiedString, $errorString"
        } else if (alreadyDownloaded > 0) {
            val totalNumber = totalDownloads + alreadyDownloaded
            notificationTitle =
                resources.getQuantityString(R.plurals.download_service_final_notification_with_details,
                    totalNumber,
                    totalDownloads,
                    totalNumber)
            size = resources.getQuantityString(R.plurals.already_downloaded_service,
                alreadyDownloaded,
                alreadyDownloaded)
        } else if (errorCount > 0) {
            sendTakenDownAlert()
            val totalNumber = totalDownloads + errorCount
            notificationTitle =
                resources.getQuantityString(R.plurals.download_service_final_notification_with_details,
                    totalNumber,
                    totalDownloads,
                    totalNumber)
            size = resources.getQuantityString(R.plurals.download_service_failed,
                errorCount,
                errorCount)
        } else {
            notificationTitle =
                resources.getQuantityString(R.plurals.download_service_final_notification,
                    totalDownloads,
                    totalDownloads)
            val totalBytes = Util.getSizeString(
                megaApi.totalDownloadedBytes)
            size = getString(R.string.general_total_size, totalBytes)
        }
        val intent = Intent(applicationContext, ManagerActivity::class.java)
        intent.action = Constants.ACTION_SHOW_TRANSFERS
        intent.putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.COMPLETED_TAB)
        val pendingIntent = PendingIntent.getActivity(applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (totalDownloads != 1) {
            Timber.d("Show notification")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                    Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT)
                channel.setShowBadge(true)
                channel.setSound(null, null)
                mNotificationManager.createNotificationChannel(channel)
                val mBuilderCompatO = NotificationCompat.Builder(
                    applicationContext, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
                mBuilderCompatO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true).setTicker(notificationTitle)
                    .setContentTitle(notificationTitle).setContentText(size)
                    .setOngoing(false)
                mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINAL,
                    mBuilderCompatO.build())
            } else {
                val builder = NotificationCompat.Builder(this)
                builder
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true).setTicker(notificationTitle)
                    .setContentTitle(notificationTitle).setContentText(size)
                    .setOngoing(false)
                mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINAL,
                    builder.build())
            }
        } else {
            try {
                val autoPlayEnabled = java.lang.Boolean.parseBoolean(dbH.autoPlayEnabled)
                if (downloadForPreview || openFile && autoPlayEnabled) {

                    val path = FileUtil.getLocalFile(
                        megaApi.getNodeByHandle(handle))
                    currentFile?.let { file ->
                        val fileLocalPath: String = path ?: file.absolutePath
                        currentDocument?.let {
                            autoPlayInfo = AutoPlayInfo(it.name,
                                it.handle,
                                fileLocalPath,
                                true)
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
                                    NotificationChannel(Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                                        Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                                        NotificationManager.IMPORTANCE_DEFAULT)
                                channel.setShowBadge(true)
                                channel.setSound(null, null)
                                mNotificationManager.createNotificationChannel(channel)
                                val mBuilderCompatO = NotificationCompat.Builder(
                                    applicationContext, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
                                mBuilderCompatO
                                    .setSmallIcon(R.drawable.ic_stat_notify)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true).setTicker(notificationTitle)
                                    .setContentTitle(notificationTitle).setContentText(size)
                                    .setOngoing(false)
                                mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                    mBuilderCompatO.build())
                            } else {
                                val builder = NotificationCompat.Builder(this)
                                builder
                                    .setSmallIcon(R.drawable.ic_stat_notify)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true).setTicker(notificationTitle)
                                    .setContentTitle(notificationTitle).setContentText(size)
                                    .setOngoing(false)
                                mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                    builder.build())
                            }
                        }
                    } else if (MimeTypeList.typeForName(currentFile?.name).isVideoMimeType || MimeTypeList.typeForName(
                            currentFile?.name).isAudio
                    ) {
                        Timber.d("Video/Audio file")
                        if (fromMV) {
                            Timber.d("Show notification")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val channel =
                                    NotificationChannel(Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                                        Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                                        NotificationManager.IMPORTANCE_DEFAULT)
                                channel.setShowBadge(true)
                                channel.setSound(null, null)
                                mNotificationManager.createNotificationChannel(channel)
                                val mBuilderCompatO = NotificationCompat.Builder(
                                    applicationContext, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
                                mBuilderCompatO
                                    .setSmallIcon(R.drawable.ic_stat_notify)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true).setTicker(notificationTitle)
                                    .setContentTitle(notificationTitle).setContentText(size)
                                    .setOngoing(false)
                                mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                    mBuilderCompatO.build())
                            } else {
                                val builder = NotificationCompat.Builder(this)
                                builder
                                    .setSmallIcon(R.drawable.ic_stat_notify)
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true).setTicker(notificationTitle)
                                    .setContentTitle(notificationTitle).setContentText(size)
                                    .setOngoing(false)
                                mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                    builder.build())
                            }
                        }
                    } else if (MimeTypeList.typeForName(currentFile?.name).isImage) {
                        Timber.d("Download is IMAGE")
                        if (fromMV) {
                            Timber.d("Show notification")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val channel =
                                    NotificationChannel(Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                                        Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                                        NotificationManager.IMPORTANCE_DEFAULT)
                                channel.setShowBadge(true)
                                channel.setSound(null, null)
                                mNotificationManager.createNotificationChannel(channel)
                                val mBuilderCompatO = NotificationCompat.Builder(
                                    applicationContext, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
                                mBuilderCompatO
                                    .setSmallIcon(R.drawable.ic_stat_notify)
                                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true).setTicker(notificationTitle)
                                    .setContentTitle(notificationTitle).setContentText(size)
                                    .setOngoing(false)
                                mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                    mBuilderCompatO.build())
                            } else {
                                val builder = NotificationCompat.Builder(this)
                                builder
                                    .setSmallIcon(R.drawable.ic_stat_notify)
                                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                                    .setContentIntent(pendingIntent)
                                    .setAutoCancel(true).setTicker(notificationTitle)
                                    .setContentTitle(notificationTitle).setContentText(size)
                                    .setOngoing(false)
                                mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                    builder.build())
                            }
                        }
                    } else {
                        Timber.d("Show notification")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val channel =
                                NotificationChannel(Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                                    Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                                    NotificationManager.IMPORTANCE_DEFAULT)
                            channel.setShowBadge(true)
                            channel.setSound(null, null)
                            mNotificationManager.createNotificationChannel(channel)
                            val mBuilderCompatO = NotificationCompat.Builder(
                                applicationContext, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
                            mBuilderCompatO
                                .setSmallIcon(R.drawable.ic_stat_notify)
                                .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true).setTicker(notificationTitle)
                                .setContentTitle(notificationTitle).setContentText(size)
                                .setOngoing(false)
                            mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                mBuilderCompatO.build())
                        } else {
                            val builder = NotificationCompat.Builder(this)
                            builder
                                .setSmallIcon(R.drawable.ic_stat_notify)
                                .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true).setTicker(notificationTitle)
                                .setContentTitle(notificationTitle).setContentText(size)
                                .setOngoing(false)
                            mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINAL,
                                builder.build())
                        }
                    }
                } else {
                    openFile = true //Set the openFile to the default
                    Timber.d("Show notification")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channel =
                            NotificationChannel(Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                                Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                                NotificationManager.IMPORTANCE_DEFAULT)
                        channel.setShowBadge(true)
                        channel.setSound(null, null)
                        mNotificationManager.createNotificationChannel(channel)
                        val mBuilderCompatO = NotificationCompat.Builder(
                            applicationContext, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
                        mBuilderCompatO
                            .setSmallIcon(R.drawable.ic_stat_notify)
                            .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true).setTicker(notificationTitle)
                            .setContentTitle(notificationTitle).setContentText(size)
                            .setOngoing(false)
                        mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINAL,
                            mBuilderCompatO.build())
                    } else {
                        val builder = NotificationCompat.Builder(this)
                        builder
                            .setSmallIcon(R.drawable.ic_stat_notify)
                            .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true).setTicker(notificationTitle)
                            .setContentTitle(notificationTitle).setContentText(size)
                            .setOngoing(false)
                        mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINAL,
                            builder.build())
                    }
                }
            } catch (e: Exception) {
                openFile = true //Set the openFile to the default
                Timber.e(e)
                Timber.d("Show notification")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                        Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT)
                    channel.setShowBadge(true)
                    channel.setSound(null, null)
                    mNotificationManager.createNotificationChannel(channel)
                    val mBuilderCompatO = NotificationCompat.Builder(
                        applicationContext, Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
                    mBuilderCompatO
                        .setSmallIcon(R.drawable.ic_stat_notify)
                        .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true).setTicker(notificationTitle)
                        .setContentTitle(notificationTitle).setContentText(size)
                        .setOngoing(false)
                    mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINAL,
                        mBuilderCompatO.build())
                } else {
                    val builder = NotificationCompat.Builder(this)
                    builder
                        .setSmallIcon(R.drawable.ic_stat_notify)
                        .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true).setTicker(notificationTitle)
                        .setContentTitle(notificationTitle).setContentText(size)
                        .setOngoing(false)
                    mNotificationManager.notify(Constants.NOTIFICATION_DOWNLOAD_FINAL,
                        builder.build())
                }
            }
        }
    }

    /*
     * Update notification download progress
     */
    @SuppressLint("NewApi")
    private fun updateProgressNotification() {
        val pendingTransfers = megaApi.getNumPendingDownloadsNonBackground()
        val totalTransfers = megaApi.totalDownloads - backgroundTransfers.size
        val totalSizePendingTransfer = megaApi.totalDownloadBytes
        val totalSizeTransferred = megaApi.totalDownloadedBytes
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
                    if (now - lastUpdated > Util.ONTRANSFERUPDATE_REFRESH_MILLIS || megaApi.areTransfersPaused(
                            MegaTransfer.TYPE_DOWNLOAD)
                    ) {
                        now
                    } else {
                        return
                    }
            }
            val progressPercent =
                Math.round(totalSizeTransferred.toDouble() / totalSizePendingTransfer * 100).toInt()
            Timber.d("Progress: $progressPercent%")
            showRating(totalSizePendingTransfer, megaApi.currentDownloadSpeed)
            var message: String? = ""
            message = if (totalTransfers == 0) {
                getString(R.string.download_preparing_files)
            } else {
                val inProgress =
                    if (pendingTransfers == 0) totalTransfers else totalTransfers - pendingTransfers + 1
                if (megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD)) {
                    StringResourcesUtils.getString(R.string.download_service_notification_paused,
                        inProgress,
                        totalTransfers)
                } else {
                    StringResourcesUtils.getString(R.string.download_service_notification,
                        inProgress,
                        totalTransfers)
                }
            }
            val info = Util.getProgressSize(this@DownloadService,
                totalSizeTransferred,
                totalSizePendingTransfer)
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
                val channel = NotificationChannel(Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                    Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT)
                channel.setShowBadge(true)
                channel.setSound(null, null)
                mNotificationManager.createNotificationChannel(channel)
                val mBuilderCompat = NotificationCompat.Builder(applicationContext,
                    Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
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
                .showRatingBaseOnSpeedAndSize(total,
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
        return null
    }

    private fun doOnTransferStart(transfer: MegaTransfer?): Completable {
        return Completable.fromCallable {
            Timber.d("Download start: %d, totalDownloads: %d",
                transfer?.nodeHandle,
                megaApi.totalDownloads)
            if (transfer?.isStreamingTransfer == true || transfer?.isVoiceClipTransfer() == true) return@fromCallable null
            if (transfer?.isBackgroundTransfer() == true) {
                backgroundTransfers.add(transfer.tag)
                return@fromCallable null
            }
            if (transfer?.type == MegaTransfer.TYPE_DOWNLOAD) {
                val appData = transfer.appData
                if (!TextUtil.isTextEmpty(appData) && appData.contains(Constants.APP_DATA_SD_CARD)) {
                    dbH.addSDTransfer(SDTransfer(
                        transfer.tag,
                        transfer.fileName,
                        Util.getSizeString(transfer.totalBytes),
                        java.lang.Long.toString(transfer.nodeHandle),
                        transfer.path,
                        appData))
                }
                transfersManagement.checkScanningTransferOnStart(transfer)
                transfersCount++
                updateProgressNotification()
            }
            null
        }
    }

    private fun doOnTransferFinish(transfer: MegaTransfer?, error: MegaError): Completable {
        return Completable.fromCallable {
            Timber.d("Node handle: " + transfer?.nodeHandle + ", Type = " + transfer?.type)
            if (transfer?.isStreamingTransfer == true) {
                return@fromCallable null
            }
            if (error.errorCode == MegaError.API_EBUSINESSPASTDUE) {
                sendBroadcast(Intent(Constants.BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED))
            }
            if (transfer?.type == MegaTransfer.TYPE_DOWNLOAD) {
                transfersManagement.checkScanningTransferOnFinish(transfer)
                val isVoiceClip = transfer.isVoiceClipTransfer()
                val isBackgroundTransfer = transfer.isBackgroundTransfer()
                if (!isVoiceClip && !isBackgroundTransfer) transfersCount--
                val path = transfer.path
                val targetPath = SDCardUtils.getSDCardTargetPath(transfer.appData)
                if (!transfer.isFolderTransfer) {
                    if (!isVoiceClip && !isBackgroundTransfer) {
                        val completedTransfer = AndroidCompletedTransfer(transfer, error)
                        if (!TextUtil.isTextEmpty(targetPath)) {
                            completedTransfer.path = targetPath
                        }
                        addCompletedTransfer(completedTransfer, dbH)
                    }
                    if (transfer.state == MegaTransfer.STATE_FAILED) {
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
                        resultTransfersVoiceClip(transfer.nodeHandle,
                            Constants.ERROR_VOICE_CLIP_TRANSFER)
                        val localFile = buildVoiceClipFile(this, transfer.fileName)
                        if (FileUtil.isFileAvailable(localFile)) {
                            Timber.d("Delete own voiceclip : exists")
                            localFile?.delete()
                        }
                    } else {
                        val file = File(transfer.path)
                        file.delete()
                    }
                    cancel()
                } else {
                    if (error.errorCode == MegaError.API_OK) {
                        Timber.d("Download OK - Node handle: %s", transfer.nodeHandle)
                        if (isVoiceClip) {
                            resultTransfersVoiceClip(transfer.nodeHandle,
                                Constants.SUCCESSFUL_VOICE_CLIP_TRANSFER)
                        }

                        //need to move downloaded file to a location on sd card.
                        if (targetPath != null) {
                            val source = File(path)
                            try {
                                val sdCardOperator = SDCardOperator(this)
                                sdCardOperator.moveDownloadedFileToDestinationPath(source,
                                    targetPath,
                                    SDCardUtils.getSDCardTargetUri(transfer.appData),
                                    transfer.tag)
                            } catch (e: Exception) {
                                Timber.e(e, "Error moving file to the sd card path.")
                            }
                        }
                        //To update thumbnails for videos
                        if (FileUtil.isVideoFile(transfer.path)) {
                            Timber.d("Is video!")
                            val videoNode = megaApi.getNodeByHandle(transfer.nodeHandle)
                            if (videoNode != null) {
                                if (!videoNode.hasThumbnail()) {
                                    Timber.d("The video has not thumb")
                                    ThumbnailUtils.createThumbnailVideo(this,
                                        path,
                                        megaApi,
                                        transfer.nodeHandle)
                                }
                            } else {
                                Timber.w("videoNode is NULL")
                            }
                        } else {
                            Timber.d("NOT video!")
                        }
                        if (!TextUtil.isTextEmpty(path)) {
                            FileUtil.sendBroadcastToUpdateGallery(this, File(path))
                        }
                        storeToAdvancedDevices[transfer.nodeHandle]?.let { transfersUri ->
                            Timber.d("Now copy the file to the SD Card")
                            openFile = false
                            val node = megaApi.getNodeByHandle(transfer.nodeHandle)
                            alterDocument(transfersUri, node.name)
                        }
                        if (!TextUtil.isTextEmpty(path) && path.contains(OfflineUtils.OFFLINE_DIR)) {
                            Timber.d("It is Offline file")
                            offlineNode = megaApi.getNodeByHandle(transfer.nodeHandle)
                            if (offlineNode != null) {
                                OfflineUtils.saveOffline(this,
                                    megaApi,
                                    dbH,
                                    offlineNode,
                                    transfer.path)
                            } else {
                                OfflineUtils.saveOfflineChatFile(dbH, transfer)
                            }
                            refreshOfflineFragment()
                            refreshSettingsFragment()
                        }
                    } else {
                        Timber.e("Download ERROR: %s", transfer.nodeHandle)
                        if (isVoiceClip) {
                            resultTransfersVoiceClip(transfer.nodeHandle,
                                Constants.ERROR_VOICE_CLIP_TRANSFER)
                            val localFile = buildVoiceClipFile(this, transfer.fileName)
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
                            if (!TextUtil.isTextEmpty(transfer.path)) {
                                val file = File(transfer.path)
                                file.delete()
                            }
                        }
                    }
                }
                if (isVoiceClip || isBackgroundTransfer) return@fromCallable null
                if (megaApi.getNumPendingDownloadsNonBackground() == 0 && transfersCount == 0) {
                    onQueueComplete(transfer.nodeHandle)
                }
            }
            null
        }
    }

    private fun resultTransfersVoiceClip(nodeHandle: Long, result: Int) {
        Timber.d("nodeHandle =  $nodeHandle, the result is $result")
        val intent = Intent(Constants.BROADCAST_ACTION_INTENT_VOICE_CLIP_DOWNLOADED)
        intent.putExtra(INTENT_EXTRA_NODE_HANDLE, nodeHandle)
        intent.putExtra(Constants.EXTRA_RESULT_TRANSFER, result)
        sendBroadcast(intent)
    }

    private fun alterDocument(uri: Uri, fileName: String) {
        Timber.d("alterUri")
        try {
            val tempFolder = getCacheFolder(applicationContext, CacheFolderManager.TEMPORARY_FOLDER)
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

    private fun doOnTransferUpdate(transfer: MegaTransfer?): Completable {
        return Completable.fromCallable {
            if (transfer?.type == MegaTransfer.TYPE_DOWNLOAD) {
                if (canceled) {
                    Timber.d("Transfer cancel: %s", transfer.nodeHandle)
                    releaseLocks()
                    megaApi.cancelTransfer(transfer)
                    cancel()
                    return@fromCallable null
                }
                if (transfer.isStreamingTransfer || transfer.isVoiceClipTransfer()) return@fromCallable null
                if (transfer.isBackgroundTransfer()) {
                    backgroundTransfers.add(transfer.tag)
                    return@fromCallable null
                }
                transfersManagement.checkScanningTransferOnUpdate(transfer)
                if (!transfer.isFolderTransfer) {
                    updateProgressNotification()
                }
                if (!transfersManagement.isOnTransferOverQuota() && transfersManagement.hasNotToBeShowDueToTransferOverQuota()) {
                    transfersManagement.setHasNotToBeShowDueToTransferOverQuota(false)
                }
            }
            null
        }
    }

    private fun doOnTransferTemporaryError(transfer: MegaTransfer?, e: MegaError): Completable {
        return Completable.fromCallable {
            Timber.w("""Download Temporary Error - Node Handle: ${transfer?.nodeHandle}
Error: ${e.errorCode} ${e.errorString}""")
            if (transfer?.isStreamingTransfer == true || transfer?.isBackgroundTransfer() == true) {
                return@fromCallable null
            }
            if (transfer?.type == MegaTransfer.TYPE_DOWNLOAD) {
                if (e.errorCode == MegaError.API_EOVERQUOTA) {
                    if (e.value != 0L) {
                        Timber.w("TRANSFER OVERQUOTA ERROR: %s", e.errorCode)
                        checkTransferOverQuota(true)
                        downloadedBytesToOverQuota = megaApi.totalDownloadedBytes
                        isOverQuota = true
                    }
                }
            }
            null
        }
    }

    /**
     * Checks if should show transfer over quota warning.
     * If so, sends a broadcast to show it in the current view.
     *
     * @param isCurrentOverQuota true if the overquota is currently received, false otherwise
     */
    private fun checkTransferOverQuota(isCurrentOverQuota: Boolean) {
        if (activityLifecycleHandler.isActivityVisible) {
            if (transfersManagement.shouldShowTransferOverQuotaWarning()) {
                transfersManagement.isCurrentTransferOverQuota = isCurrentOverQuota
                transfersManagement.setTransferOverQuotaTimestamp()
                applicationScope.launch {
                    broadcastTransferOverQuota()
                }
            }
        } else if (!transfersManagement.isTransferOverQuotaNotificationShown) {
            transfersManagement.isTransferOverQuotaNotificationShown = true
            isForeground = false
            stopForeground(true)
            mNotificationManager.cancel(Constants.NOTIFICATION_DOWNLOAD)
            TransferOverQuotaNotification(transfersManagement).show()
        }
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart: %s", request.requestString)
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("onRequestFinish")
        if (request.type == MegaRequest.TYPE_CANCEL_TRANSFERS) {
            Timber.d("TYPE_CANCEL_TRANSFERS finished")
            if (e.errorCode == MegaError.API_OK) {
                cancel()
            }
        } else if (request.type == MegaRequest.TYPE_LOGIN) {
            if (e.errorCode == MegaError.API_OK) {
                Timber.d("Logged in. Setting account auth token for folder links.")
                megaApiFolder.accountAuth = megaApi.accountAuth
                Timber.d("Fast login OK, Calling fetchNodes from CameraSyncService")
                megaApi.fetchNodes()

                // Get cookies settings after login.
                getInstance().checkEnabledCookies()
            } else {
                Timber.e("ERROR: %s", e.errorString)
                isLoggingIn = false
                MegaApplication.isLoggingIn = isLoggingIn
            }
        } else if (request.type == MegaRequest.TYPE_FETCH_NODES) {
            if (e.errorCode == MegaError.API_OK) {
                isLoggingIn = false
                MegaApplication.isLoggingIn = isLoggingIn
                for (i in pendingIntents.indices) {
                    onHandleIntent(pendingIntents[i])
                }
                pendingIntents.clear()
            } else {
                Timber.e("ERROR: " + e.errorString)
                isLoggingIn = false
                MegaApplication.isLoggingIn = isLoggingIn
            }
        } else {
            Timber.d("Public node received")
            if (e.errorCode != MegaError.API_OK) {
                Timber.e("Public node error")
                return
            }
            val node = request.publicMegaNode
            if (node == null) {
                Timber.e("Public node is null")
                return
            }
            if (currentDir == null) {
                Timber.e("currentDir is null")
                return
            }
            currentFile = if (currentDir?.isDirectory == true) {
                File(currentDir,
                    megaApi.escapeFsIncompatible(node.name,
                        currentDir?.absolutePath + Constants.SEPARATOR))
            } else {
                currentDir
            }
            val appData = getSDCardAppData(intent)
            Timber.d("Public node download launched")
            acquireLocks()
            if (currentDir?.isDirectory == true) {
                Timber.d("To downloadPublic(dir)")
                val localPath = currentDir?.absolutePath + "/"
                currentDocument?.let {
                    val token = transfersManagement.addScanningTransfer(MegaTransfer.TYPE_DOWNLOAD,
                        localPath, it, it.isFolder)
                    megaApi.startDownload(currentDocument,
                        localPath,
                        it.name,
                        appData,
                        false,
                        token)
                }
            }
        }
    }

    private fun acquireLocks() {
        if (!wakeLock.isHeld) wakeLock.acquire()
        if (!wifiLock.isHeld) wifiLock.acquire()
    }

    override fun onRequestTemporaryError(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.w("Node handle: %s", request.nodeHandle)
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestUpdate")
    }

    private fun refreshOfflineFragment() {
        sendBroadcast(Intent(OfflineFragment.REFRESH_OFFLINE_FILE_LIST))
    }

    private fun refreshSettingsFragment() {
        val intent = Intent(Constants.BROADCAST_ACTION_INTENT_SETTINGS_UPDATED)
        intent.action = ACTION_REFRESH_CLEAR_OFFLINE_SETTING
        sendBroadcast(intent)
    }

    companion object {
        // Action to stop download
        const val ACTION_CANCEL = "CANCEL_DOWNLOAD"
        const val EXTRA_SIZE = "DOCUMENT_SIZE"
        const val EXTRA_HASH = "DOCUMENT_HASH"
        const val EXTRA_URL = "DOCUMENT_URL"
        const val EXTRA_DOWNLOAD_TO_SDCARD = "download_to_sdcard"
        const val EXTRA_TARGET_PATH = "target_path"
        const val EXTRA_TARGET_URI = "target_uri"
        const val EXTRA_PATH = "SAVE_PATH"
        const val EXTRA_FOLDER_LINK = "FOLDER_LINK"
        const val EXTRA_FROM_MV = "fromMV"
        const val EXTRA_CONTACT_ACTIVITY = "CONTACT_ACTIVITY"
        const val EXTRA_OPEN_FILE = "OPEN_FILE"
        const val EXTRA_CONTENT_URI = "CONTENT_URI"
        const val EXTRA_DOWNLOAD_FOR_PREVIEW = "EXTRA_DOWNLOAD_FOR_PREVIEW"
        const val EXTRA_DOWNLOAD_BY_OPEN_WITH = "EXTRA_DOWNLOAD_BY_OPEN_WITH"
        const val EXTRA_DOWNLOAD_FOR_OFFLINE = "EXTRA_DOWNLOAD_FOR_OFFLINE"
        private var errorEBlocked = 0
    }
}