package mega.privacy.android.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.Observer
import com.jeremyliao.liveeventbus.LiveEventBus
import com.shockwave.pdfium.PdfiumCore
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_SHOW_SNACKBAR
import mega.privacy.android.app.constants.BroadcastConstants.SNACKBAR_TEXT
import mega.privacy.android.app.constants.EventConstants.EVENT_FINISH_SERVICE_IF_NO_TRANSFERS
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.globalmanagement.TransfersManagement.Companion.createInitialServiceNotification
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.qrcode.mycode.MyCodeFragment
import mega.privacy.android.app.presentation.transfers.model.mapper.CompletedTransferMapper
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.app.textEditor.TextEditorUtil.getCreationOrEditorText
import mega.privacy.android.app.usecase.GetGlobalTransferUseCase
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.CacheFolderManager.buildQrFile
import mega.privacy.android.app.utils.CacheFolderManager.deleteCacheFolderIfEmpty
import mega.privacy.android.app.utils.CacheFolderManager.getCacheFile
import mega.privacy.android.app.utils.CacheFolderManager.getCacheFolder
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.PreviewUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.transfer.TransferFinishType
import mega.privacy.android.domain.entity.transfer.TransfersFinishedState
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.transfer.AddCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfer.BroadcastTransfersFinishedUseCase
import mega.privacy.android.domain.usecase.transfer.MonitorPausedTransfers
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Service to Upload files
 */
@AndroidEntryPoint
class UploadService : Service() {

    /**
     * Use case to get glboal transfer
     */
    @Inject
    lateinit var getGlobalTransferUseCase: GetGlobalTransferUseCase

    /**
     * Transfers Management
     */
    @Inject
    lateinit var transfersManagement: TransfersManagement

    /**
     * database handler
     */
    @Inject
    lateinit var dbH: LegacyDatabaseHandler

    /**
     * MegaApi
     */
    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    /**
     * MEGAchat API
     */
    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    /**
     * Monitor paused transfers.
     */
    @Inject
    lateinit var monitorPausedTransfers: MonitorPausedTransfers

    @Inject
    lateinit var broadcastTransfersFinishedUseCase: BroadcastTransfersFinishedUseCase

    @Inject
    lateinit var addCompletedTransferUseCase: AddCompletedTransferUseCase

    @Inject
    lateinit var completedTransferMapper: CompletedTransferMapper

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    private var isForeground = false
    private var canceled = false

    private var lock: WifiManager.WifiLock? = null
    private var wl: PowerManager.WakeLock? = null
    private var notificationBuilder: Notification.Builder? = null
    private var notificationBuilderCompat: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private val mapProgressFileTransfers: HashMap<Int, MegaTransfer> = HashMap()
    private var pendingToAddInQueue = 0
    private var completed = 0
    private var completedSuccessfully = 0
    private var alreadyUploaded = 0
    private var uploadCount = 0

    //NOT_OVERQUOTA_STATE           = 0 - not over quota, not pre-over quota
    //OVERQUOTA_STORAGE_STATE       = 1 - over quota
    //PRE_OVERQUOTA_STORAGE_STATE   = 2 - pre-over quota
    private var isOverQuota = Constants.NOT_OVERQUOTA_STATE

    private val rxSubscriptions = CompositeDisposable()

    // the flag to determine the rating dialog is showed for this upload action
    private var isRatingShowed = false
    private val stopServiceObserver = Observer { finish: Boolean ->
        @Suppress("DEPRECATION")
        if (finish && megaApi.numPendingUploads == 0) {
            stopForeground()
        }
    }

    private var monitorPausedTransfersJob: Job? = null

    @SuppressLint("NewApi", "CheckResult", "WrongConstant")
    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")
        notificationBuilder = Notification.Builder(this@UploadService)
        notificationBuilderCompat =
            NotificationCompat.Builder(this@UploadService, Constants.NOTIFICATION_CHANNEL_UPLOAD_ID)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        startForeground()
        isForeground = false
        canceled = false
        isOverQuota = Constants.NOT_OVERQUOTA_STATE

        (applicationContext.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager?)?.let { wifiManager ->
            lock =
                wifiManager.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                    "MegaUploadServiceWifiLock"
                )
        }
        (getSystemService(POWER_SERVICE) as PowerManager?)?.let { powerManager ->
            wl = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MegaUploadServicePowerLock:"
            )
        }

        monitorPausedTransfersJob = applicationScope.launch {
            monitorPausedTransfers().collectLatest {
                // delay 1 second to refresh the pause notification to prevent update is missed
                Handler(Looper.getMainLooper()).postDelayed(
                    { updateProgressNotification(true) },
                    TransfersManagement.WAIT_TIME_BEFORE_UPDATE
                )
            }
        }

        LiveEventBus.get(EVENT_FINISH_SERVICE_IF_NO_TRANSFERS, Boolean::class.java)
            .observeForever(stopServiceObserver)
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

                    else -> {}
                }
            }
            ) { t: Throwable? -> Timber.e(t) }
        rxSubscriptions.add(subscription)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand")
        canceled = false
        if (intent == null) {
            canceled = true
            stopForeground()
            return START_NOT_STICKY
        }
        intent.action?.takeIf { it == ACTION_CANCEL }?.let {
            Timber.d("Cancel intent")
            canceled = true
            megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD)
            stopForeground()
            return START_NOT_STICKY
        }

        Timber.d("action = ${intent.action}")
        startForeground()
        onHandleIntent(intent)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun startForeground() {
        Timber.d("StartForeground")
        isForeground = try {
            startForeground(
                Constants.NOTIFICATION_UPLOAD,
                createInitialNotification()
            )
            true
        } catch (e: Exception) {
            Timber.w(e, "Error starting foreground.")
            false
        }
    }

    private fun createInitialNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createInitialServiceNotification(
                Constants.NOTIFICATION_CHANNEL_UPLOAD_ID,
                Constants.NOTIFICATION_CHANNEL_UPLOAD_NAME,
                notificationManager!!,
                NotificationCompat.Builder(
                    this,
                    Constants.NOTIFICATION_CHANNEL_UPLOAD_ID
                )
            )
        } else {
            createInitialServiceNotification(Notification.Builder(this))
        }
    }

    private fun stopForeground() {
        isForeground = false
        stopForeground(true)
        notificationManager?.cancel(Constants.NOTIFICATION_UPLOAD)
        notificationManager?.cancel(Constants.NOTIFICATION_UPLOAD_FOLDER)
        stopSelf()
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        releaseLocks()
        megaChatApi.saveCurrentState()
        rxSubscriptions.clear()
        LiveEventBus.get(EVENT_FINISH_SERVICE_IF_NO_TRANSFERS, Boolean::class.java)
            .removeObserver(stopServiceObserver)
        monitorPausedTransfersJob?.cancel()
        super.onDestroy()
    }

    @Synchronized
    private fun onHandleIntent(intent: Intent) {
        Timber.d("onHandleIntent")
        val action = intent.action
        Timber.d("Action is $action")
        action?.let {
            when (it) {
                Constants.ACTION_OVERQUOTA_STORAGE -> isOverQuota =
                    Constants.OVERQUOTA_STORAGE_STATE
                Constants.ACTION_STORAGE_STATE_CHANGED -> isOverQuota =
                    Constants.NOT_OVERQUOTA_STATE
                Constants.ACTION_RESTART_SERVICE -> {
                    val transferData = megaApi.getTransferData(null)
                    if (transferData == null) {
                        stopForeground()
                        return
                    }
                    val uploadsInProgress = transferData.numUploads
                    var i = 0
                    while (i < uploadsInProgress) {
                        val transfer = megaApi.getTransferByTag(transferData.getUploadTag(i))
                        if (transfer == null || isCUOrChatTransfer(transfer)) {
                            i++
                            continue
                        }
                        if (!transfer.isFolderTransfer && transfer.appData == null) {
                            mapProgressFileTransfers[transfer.tag] = transfer
                        }
                        i++
                    }
                    uploadCount = mapProgressFileTransfers.size
                    if (uploadCount > 0) {
                        isForeground = false
                        stopForeground(true)
                        notificationManager?.cancel(Constants.NOTIFICATION_UPLOAD)
                    }
                }
            }
            if (uploadCount == 0) {
                stopForeground()
            } else {
                updateProgressNotification()
            }
            return
        } ?: run {
            isOverQuota = Constants.NOT_OVERQUOTA_STATE
        }
        intent.getStringExtra(EXTRA_FILE_PATH)?.takeIf { it.isNotEmpty() }?.let { filePath ->
            acquireLock()
            doHandleIntent(intent, filePath)
        } ?: run {
            Timber.w("Error: File path is NULL or EMPTY")
        }
    }

    private fun doHandleIntent(intent: Intent, filePath: String) {
        val file = File(filePath)
        Timber.d("File to manage: ${file.absolutePath}")
        val textFileMode = intent.getStringExtra(EXTRA_UPLOAD_TXT)
        val parentHandle = intent.getLongExtra(EXTRA_PARENT_HASH, MegaApiJava.INVALID_HANDLE)
        val fileName = intent.getStringExtra(EXTRA_NAME)
        var lastModified = intent.getLongExtra(EXTRA_LAST_MODIFIED, 0)
        if (lastModified <= 0) {
            lastModified = file.lastModified() / 1000
        }
        val parentNode =
            if (parentHandle == MegaApiJava.INVALID_HANDLE)
                megaApi.rootNode
            else
                megaApi.getNodeByHandle(parentHandle)
        val mTime = if (lastModified == 0L) Constants.INVALID_VALUE.toLong() else lastModified
        pendingToAddInQueue++
        if (!TextUtil.isTextEmpty(textFileMode)) {
            val appData = (Constants.APP_DATA_TXT_FILE
                    + Constants.APP_DATA_INDICATOR
                    + textFileMode
                    + Constants.APP_DATA_INDICATOR
                    + intent.getBooleanExtra(Constants.FROM_HOME_PAGE, false)
                    )

            megaApi.startUpload(
                file.absolutePath, parentNode, fileName, mTime, appData,
                true, true, null
            )
        } else {
            val cancelToken = transfersManagement
                .addScanningTransfer(
                    MegaTransfer.TYPE_UPLOAD,
                    file.absolutePath,
                    parentNode,
                    file.isDirectory
                )
            megaApi.startUpload(
                file.absolutePath, parentNode, fileName, mTime, null,
                false, false, cancelToken
            )
        }
    }

    /*
     * Stop uploading service
     */
    private fun cancel() {
        Timber.d("cancel")
        canceled = true
        stopForeground()
    }

    /**
     * No more intents in the queue, reset and finish service.
     *
     * @param showSnackbar True if should show finish snackbar, false otherwise.
     */
    private fun onQueueComplete(showSnackbar: Boolean) {
        Timber.d("onQueueComplete")
        releaseLocks()
        if (isOverQuota != Constants.NOT_OVERQUOTA_STATE) {
            showStorageOverQuotaNotification()
        } else {
            showUploadCompleteNotification()
            if (showSnackbar && uploadCount > 0) {
                sendUploadFinishBroadcast()
            }
        }
        @Suppress("DEPRECATION")
        if (megaApi.numPendingUploads <= 0) {
            Timber.d("Reset total uploads")
            megaApi.resetTotalUploads()
        }
        resetUploadNumbers()
        Timber.d("Stopping service!")
        stopForeground()
        Timber.d("After stopSelf")
        if (hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            deleteCacheFolderIfEmpty(applicationContext, CacheFolderManager.TEMPORARY_FOLDER)
        }
    }

    private fun notifyNotification(
        notificationTitle: String,
        size: String,
    ) {
        val notificationId = Constants.NOTIFICATION_UPLOAD_FINAL
        val channelId = Constants.NOTIFICATION_CHANNEL_UPLOAD_ID
        val channelName = Constants.NOTIFICATION_CHANNEL_UPLOAD_NAME
        val intent = Intent(this@UploadService, ManagerActivity::class.java).apply {
            action = Constants.ACTION_SHOW_TRANSFERS
            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.COMPLETED_TAB)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setShowBadge(true)
                setSound(null, null)
            }

            notificationManager?.createNotificationChannel(channel)
            val builderCompatOreo =
                NotificationCompat.Builder(applicationContext, channelId).also {
                    it.setSmallIcon(R.drawable.ic_stat_notify)
                        .setColor(ContextCompat.getColor(getInstance(), R.color.red_600_red_300))
                        .setContentIntent(
                            PendingIntent.getActivity(
                                applicationContext,
                                0,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        )
                        .setAutoCancel(true).setTicker(notificationTitle)
                        .setContentTitle(notificationTitle).setContentText(size)
                        .setOngoing(false)
                }

            notificationManager?.notify(notificationId, builderCompatOreo.build())
        } else {
            notificationBuilderCompat?.let { builder ->
                builder
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(getInstance(), R.color.red_600_red_300))
                    .setContentIntent(
                        PendingIntent.getActivity(
                            applicationContext,
                            0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    .setAutoCancel(true).setTicker(notificationTitle)
                    .setContentTitle(notificationTitle).setContentText(size)
                    .setOngoing(false)
                notificationManager?.notify(notificationId, builder.build())
            }
        }
    }

    private fun getTransferredByte(map: HashMap<Int, MegaTransfer>?): Long =
        map?.values?.sumOf { it.transferredBytes } ?: 0

    private fun sendUploadFinishBroadcast() = applicationScope.launch {
        broadcastTransfersFinishedUseCase(
            TransfersFinishedState(
                type = TransferFinishType.UPLOAD,
                numberFiles = uploadCount
            )
        )
    }

    /**
     * Show complete success notification.
     */
    private fun showUploadCompleteNotification() {
        Timber.d("showUploadCompleteNotification")
        if (isOverQuota == Constants.NOT_OVERQUOTA_STATE) {
            var notificationTitle = ""
            val errorCount = completed - completedSuccessfully - alreadyUploaded
            if (completedSuccessfully > 0) {
                notificationTitle = resources.getQuantityString(
                    R.plurals.upload_service_final_notification,
                    completedSuccessfully, completedSuccessfully
                )
            }
            if (alreadyUploaded > 0) {
                notificationTitle = TextUtil.addStringSeparator(notificationTitle)
                notificationTitle += resources.getQuantityString(
                    R.plurals.upload_service_notification_already_uploaded,
                    alreadyUploaded, alreadyUploaded
                )
            }
            if (errorCount > 0) {
                notificationTitle = TextUtil.addStringSeparator(notificationTitle)
                notificationTitle += resources.getQuantityString(
                    R.plurals.upload_service_failed,
                    errorCount, errorCount
                )
            }
            val transferredBytes = getTransferredByte(mapProgressFileTransfers)
            val totalBytes = Util.getSizeString(transferredBytes, this)
            val size = getString(R.string.general_total_size, totalBytes)
            notifyNotification(
                notificationTitle,
                size,
            )
        }
    }

    private fun notifyProgressNotification(
        progressPercent: Int,
        message: String,
        info: String,
        actionString: String,
    ) {
        val intent = Intent(this@UploadService, ManagerActivity::class.java).apply {
            when (isOverQuota) {
                Constants.OVERQUOTA_STORAGE_STATE -> action = Constants.ACTION_OVERQUOTA_STORAGE
                Constants.PRE_OVERQUOTA_STORAGE_STATE -> action =
                    Constants.ACTION_PRE_OVERQUOTA_STORAGE

                Constants.NOT_OVERQUOTA_STATE -> {
                    action = Constants.ACTION_SHOW_TRANSFERS
                    putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.PENDING_TAB)
                }

                else -> {
                    action = Constants.ACTION_SHOW_TRANSFERS
                    putExtra(ManagerActivity.TRANSFERS_TAB, TransfersTab.PENDING_TAB)
                }
            }
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this@UploadService,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_UPLOAD_ID,
                Constants.NOTIFICATION_CHANNEL_UPLOAD_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setShowBadge(true)
            channel.setSound(null, null)
            notificationManager?.createNotificationChannel(channel)
            NotificationCompat.Builder(
                applicationContext,
                Constants.NOTIFICATION_CHANNEL_UPLOAD_ID
            ).setSmallIcon(R.drawable.ic_stat_notify)
                .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                .setProgress(100, progressPercent, false)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setContentTitle(message)
                .setSubText(info)
                .setContentText(actionString)
                .setOnlyAlertOnce(true)
                .build()
        } else {
            notificationBuilder?.let { builder ->
                builder
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setColor(ContextCompat.getColor(this, R.color.red_600_red_300))
                    .setProgress(100, progressPercent, false)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setContentTitle(message)
                    .setSubText(info)
                    .setContentText(actionString)
                    .setOnlyAlertOnce(true)
                builder.build()
            }
        }
        if (!isForeground) {
            Timber.d("Starting foreground")
            isForeground = try {
                startForeground(Constants.NOTIFICATION_UPLOAD, notification)
                true
            } catch (e: Exception) {
                Timber.e(e, "Start foreground exception")
                false
            }
        } else {
            notificationManager?.notify(Constants.NOTIFICATION_UPLOAD, notification)
        }
    }

    private fun updateProgressNotification(pausedTransfers: Boolean = false) {
        val transfers: Collection<MegaTransfer> = ArrayList(
            mapProgressFileTransfers.values
        )
        val up = getInProgressNotification(transfers)
        val total = up.total
        val inProgress = up.inProgress
        var progressPercent = 0
        val inProgressTemp: Long
        if (total > 0) {
            inProgressTemp = inProgress * 100
            progressPercent = (inProgressTemp / total).toInt()
            showRating(total, megaApi.currentUploadSpeed)
        }
        val message = getMessageForProgressNotification(inProgress, pausedTransfers)
        Timber.d("updateProgressNotification $progressPercent $message")
        val actionString =
            if (isOverQuota == Constants.NOT_OVERQUOTA_STATE) getString(R.string.download_touch_to_show) else getString(
                R.string.general_show_info
            )
        val info = Util.getProgressSize(this@UploadService, inProgress, total)
        notifyProgressNotification(progressPercent, message, info, actionString)
    }

    /**
     * Determine if should show the rating page to users
     *
     * @param total              the total size of uploading file
     * @param currentUploadSpeed current uploading speed
     */
    private fun showRating(total: Long, currentUploadSpeed: Int) {
        if (!isRatingShowed) {
            RatingHandlerImpl(this)
                .showRatingBaseOnSpeedAndSize(total, currentUploadSpeed.toLong()) {
                    isRatingShowed = true
                }
        }
    }

    private fun getMessageForProgressNotification(
        inProgress: Long,
        pausedTransfers: Boolean = false,
    ): String {
        Timber.d("inProgress: $inProgress")
        return when {
            isOverQuota != Constants.NOT_OVERQUOTA_STATE -> getString(R.string.overquota_alert_title)
            inProgress == 0L -> getString(R.string.download_preparing_files)
            else -> {
                val stringId =
                    if (pausedTransfers)
                        R.string.upload_service_notification_paused
                    else
                        R.string.upload_service_notification

                getString(
                    stringId,
                    completed + 1,
                    uploadCount
                )
            }
        }
    }

    private fun getInProgressNotification(transfers: Collection<MegaTransfer>): UploadProgress {
        Timber.d("getInProgressNotification")
        val progress = UploadProgress()
        var total: Long = 0
        var inProgress: Long = 0

        transfers.forEach { currTransfer ->
            if (currTransfer.state == MegaTransfer.STATE_COMPLETED) {
                total += currTransfer.totalBytes
                inProgress += currTransfer.totalBytes
            } else {
                total += currTransfer.totalBytes
                inProgress += currTransfer.transferredBytes
            }
        }
        progress.total = total
        progress.inProgress = inProgress
        return progress
    }

    private fun doOnTransferStart(transfer: MegaTransfer?): Completable {
        return Completable.fromCallable {
            transfer?.let { t ->
                Timber.d("Upload start: ${t.fileName}")
                if (t.type == MegaTransfer.TYPE_UPLOAD) {
                    if (isCUOrChatTransfer(t)) return@fromCallable null
                    if (t.appData != null) {
                        return@fromCallable null
                    }
                    pendingToAddInQueue--
                    transfersManagement.checkScanningTransferOnStart(t)
                    if (!t.isFolderTransfer) {
                        uploadCount++
                        mapProgressFileTransfers[t.tag] = t
                        updateProgressNotification()
                    }
                }
                null
            }
        }
    }

    private fun doOnTransferFinish(transfer: MegaTransfer?, error: MegaError): Completable {
        return Completable.fromCallable {
            if (transfer == null) return@fromCallable null
            Timber.d("Path: ${transfer.path}, Size: ${transfer.transferredBytes}")
            if (isCUOrChatTransfer(transfer)) return@fromCallable null
            if (error.errorCode == MegaError.API_EBUSINESSPASTDUE) {
                sendBroadcast(Intent(Constants.BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED))
            }
            if (transfer.type == MegaTransfer.TYPE_UPLOAD) {
                transfersManagement.checkScanningTransferOnFinish(transfer)
                if (!transfer.isFolderTransfer) {
                    val completedTransfer = AndroidCompletedTransfer(transfer, error, this)
                    runBlocking {
                        addCompletedTransferUseCase(completedTransferMapper(completedTransfer))
                    }
                    val appData = transfer.appData
                    if (!TextUtil.isTextEmpty(appData) && appData.contains(Constants.APP_DATA_TXT_FILE)) {
                        val message =
                            getCreationOrEditorText(
                                appData = appData,
                                isSuccess = error.errorCode == MegaError.API_OK,
                                context = this
                            )
                        sendBroadcast(
                            Intent(BROADCAST_ACTION_SHOW_SNACKBAR)
                                .putExtra(SNACKBAR_TEXT, message)
                        )
                    }
                    if (transfer.state == MegaTransfer.STATE_FAILED) {
                        transfersManagement.setAreFailedTransfers(true)
                    }
                }
                transfer.appData?.let {
                    @Suppress("Deprecation")
                    if (megaApi.numPendingUploads == 0) {
                        onQueueComplete(false)
                    }
                    return@fromCallable null
                }
                if (!transfer.isFolderTransfer) {
                    completed++
                    mapProgressFileTransfers[transfer.tag] = transfer
                }
                if (canceled) {
                    Timber.d("Upload canceled: transfer.fileName")
                    releaseLocks()
                    cancel()
                    Timber.d("After cancel")
                    deleteCacheFolderIfEmpty(
                        applicationContext,
                        CacheFolderManager.TEMPORARY_FOLDER
                    )
                } else {
                    if (error.errorCode == MegaError.API_OK) {
                        if (!transfer.isFolderTransfer) {
                            if (transfer.transferredBytes == 0L) {
                                alreadyUploaded++
                            } else {
                                completedSuccessfully++
                            }
                        }
                        if (FileUtil.isVideoFile(transfer.path)) {
                            Timber.d("Is video!!!")
                            val previewDir = PreviewUtils.getPreviewFolder(this)
                            val preview = File(
                                previewDir,
                                MegaApiAndroid.handleToBase64(transfer.nodeHandle) + ".jpg"
                            )
                            val thumbDir = ThumbnailUtils.getThumbFolder(this)
                            val thumb = File(
                                thumbDir,
                                MegaApiAndroid.handleToBase64(transfer.nodeHandle) + ".jpg"
                            )
                            megaApi.createThumbnail(transfer.path, thumb.absolutePath)
                            megaApi.createPreview(transfer.path, preview.absolutePath)
                            val node = megaApi.getNodeByHandle(transfer.nodeHandle)
                            node?.let {
                                val retriever = MediaMetadataRetriever()
                                var location: String? = null
                                try {
                                    retriever.setDataSource(transfer.path)
                                    location =
                                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
                                } catch (ex: Exception) {
                                    Timber.e(ex, "Exception is thrown")
                                }
                                location?.let {
                                    Timber.d("Location: $location")
                                    var secondTry = false
                                    try {
                                        val mid = location.length / 2 //get the middle of the String
                                        val parts = arrayOf(
                                            location.substring(0, mid),
                                            location.substring(mid)
                                        )
                                        val lat = parts[0].toDouble()
                                        val lon = parts[1].toDouble()
                                        Timber.d("Lat: $lat") //first part
                                        Timber.d("Long: $lon") //second part
                                        megaApi.setNodeCoordinates(node, lat, lon, null)
                                    } catch (e: Exception) {
                                        secondTry = true
                                        Timber.e(e, "Exception, second try to set GPS coordinates")
                                    }
                                    if (secondTry) {
                                        try {
                                            val lat = location.substring(0, 7).toDouble()
                                            val lon = location.substring(8, 17).toDouble()
                                            Timber.d("Lat: $lat") //first part
                                            Timber.d("Long: $lon") //second part
                                            megaApi.setNodeCoordinates(node, lat, lon, null)
                                        } catch (e: Exception) {
                                            Timber.e(
                                                e,
                                                "Exception again, no chance to set coordinates of video"
                                            )
                                        }
                                    }
                                } ?: run {
                                    Timber.d("No location info")
                                }
                            }
                        } else if (typeForName(transfer.path).isImage) {
                            Timber.d("Is image!!!")
                            val previewDir = PreviewUtils.getPreviewFolder(this)
                            val preview = File(
                                previewDir,
                                MegaApiAndroid.handleToBase64(transfer.nodeHandle) + ".jpg"
                            )
                            val thumbDir = ThumbnailUtils.getThumbFolder(this)
                            val thumb = File(
                                thumbDir,
                                MegaApiAndroid.handleToBase64(transfer.nodeHandle) + ".jpg"
                            )
                            megaApi.createThumbnail(transfer.path, thumb.absolutePath)
                            megaApi.createPreview(transfer.path, preview.absolutePath)
                            megaApi.getNodeByHandle(transfer.nodeHandle)?.let { node ->
                                try {
                                    ExifInterface(transfer.path).latLong?.let { latLong ->
                                        megaApi.setNodeCoordinates(
                                            node,
                                            latLong[0],
                                            latLong[1],
                                            null
                                        )
                                    }
                                } catch (e: Exception) {
                                    Timber.w(e, "Couldn't read exif info: transfer.path")
                                }
                            }
                        } else if (typeForName(transfer.path).isPdf) {
                            Timber.d("Is pdf!!!")
                            try {
                                ThumbnailUtils.createThumbnailPdf(
                                    this,
                                    transfer.path,
                                    megaApi,
                                    transfer.nodeHandle
                                )
                            } catch (e: Exception) {
                                Timber.w(e, "Pdf thumbnail could not be created")
                            }
                            val pageNumber = 0
                            var out: FileOutputStream? = null
                            try {
                                val pdfiumCore = PdfiumCore(this)
                                val pdfNode = megaApi.getNodeByHandle(transfer.nodeHandle)
                                    ?: run {
                                        Timber.e("pdf is NULL")
                                        return@fromCallable null
                                    }
                                val previewDir = PreviewUtils.getPreviewFolder(this)
                                val preview = File(
                                    previewDir,
                                    MegaApiAndroid.handleToBase64(transfer.nodeHandle) + ".jpg"
                                )
                                val file = File(transfer.path)
                                val pdfDocument = pdfiumCore.newDocument(
                                    ParcelFileDescriptor.open(
                                        file,
                                        ParcelFileDescriptor.MODE_READ_ONLY
                                    )
                                )
                                pdfiumCore.openPage(pdfDocument, pageNumber)
                                val width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber)
                                val height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber)
                                val bmp =
                                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                pdfiumCore.renderPageBitmap(
                                    pdfDocument,
                                    bmp,
                                    pageNumber,
                                    0,
                                    0,
                                    width,
                                    height
                                )
                                val resizedBitmap =
                                    PreviewUtils.resizeBitmapUpload(bmp, width, height)
                                out = FileOutputStream(preview)
                                val result = resizedBitmap.compress(
                                    Bitmap.CompressFormat.JPEG,
                                    100,
                                    out
                                ) // bmp is your Bitmap instance
                                if (result) {
                                    Timber.d("Compress OK!")
                                    megaApi.setPreview(pdfNode, preview.absolutePath)
                                } else {
                                    Timber.w("Not Compress")
                                }
                                pdfiumCore.closeDocument(pdfDocument)
                            } catch (e: Exception) {
                                Timber.w(e, "Pdf preview could not be created")
                            } finally {
                                try {
                                    out?.close()
                                } catch (_: Exception) {
                                }
                            }
                        } else {
                            Timber.d("NOT video, image or pdf!")
                        }
                    } else {
                        Timber.e(
                            "Upload Error: ${transfer.fileName}_${error.errorCode}___${error.errorString}"
                        )
                        if (error.errorCode == MegaError.API_EOVERQUOTA && !transfer.isForeignOverquota) {
                            isOverQuota = Constants.OVERQUOTA_STORAGE_STATE
                        } else if (error.errorCode == MegaError.API_EGOINGOVERQUOTA) {
                            isOverQuota = Constants.PRE_OVERQUOTA_STORAGE_STATE
                        }
                    }
                    val qrFileName = megaApi.myEmail + MyCodeFragment.QR_IMAGE_FILE_NAME
                    buildQrFile(applicationContext, transfer.fileName)
                        ?.takeIf { it.exists() && it.name != qrFileName }
                        ?.let {
                            Timber.d("Delete file!: ${it.absolutePath}")
                            it.delete()
                        }
                    if (error.errorCode == MegaError.API_OK) {
                        // Get the uploaded file from cache root directory.
                        getCacheFile(applicationContext, "", transfer.fileName)
                            ?.takeIf { it.exists() }
                            ?.let { uploadedFile ->
                                Timber.d("Delete file!: ${uploadedFile.absolutePath}")
                                uploadedFile.delete()
                            }
                    }

                    Timber.d("IN Finish: ${transfer.fileName} path: ${transfer.path}")
                    getCacheFolder(applicationContext, CacheFolderManager.TEMPORARY_FOLDER)
                        ?.takeIf { it.exists() && transfer.path != null }
                        ?.let { tmpPic ->
                            if (transfer.path.startsWith(tmpPic.absolutePath)) {
                                File(transfer.path).delete()
                            }
                        }
                        ?: run {
                            Timber.e("transfer.getPath() is NULL or temporal folder unavailable")
                        }

                    if (completed == uploadCount && pendingToAddInQueue == 0) {
                        onQueueComplete(true)
                    } else {
                        updateProgressNotification()
                    }
                }
            }
            null
        }
    }

    private fun doOnTransferUpdate(transfer: MegaTransfer?): Completable {
        return Completable.fromCallable {
            Timber.d("onTransferUpdate")
            transfer
                ?.takeIf { it.type == MegaTransfer.TYPE_UPLOAD }
                ?.let { _transfer ->
                    if (isCUOrChatTransfer(_transfer)) return@fromCallable null
                    if (_transfer.appData != null) {
                        return@fromCallable null
                    }
                    if (canceled) {
                        Timber.d("Transfer cancel: ${_transfer.fileName}")
                        releaseLocks()
                        megaApi.cancelTransfer(_transfer)
                        cancel()
                        Timber.d("After cancel")
                        return@fromCallable null
                    }
                    transfersManagement.checkScanningTransferOnUpdate(_transfer)
                    if (!_transfer.isFolderTransfer) {
                        mapProgressFileTransfers[_transfer.tag] = _transfer
                        updateProgressNotification()
                    }
                }
            null
        }
    }

    private fun doOnTransferTemporaryError(transfer: MegaTransfer?, e: MegaError): Completable {
        return Completable.fromCallable {
            Timber.w("onTransferTemporaryError: ${e.errorString}__${e.errorCode}")
            transfer
                ?.takeIf { it.type == MegaTransfer.TYPE_UPLOAD }
                ?.let { _transfer ->
                    when (e.errorCode) {
                        MegaError.API_EOVERQUOTA, MegaError.API_EGOINGOVERQUOTA -> {
                            if (!_transfer.isForeignOverquota) {
                                if (e.errorCode == MegaError.API_EOVERQUOTA) {
                                    isOverQuota = Constants.OVERQUOTA_STORAGE_STATE
                                } else if (e.errorCode == MegaError.API_EGOINGOVERQUOTA) {
                                    isOverQuota = Constants.PRE_OVERQUOTA_STORAGE_STATE
                                }
                                if (e.value != 0L) {
                                    Timber.w("TRANSFER OVER QUOTA ERROR: ${e.errorCode}")
                                } else {
                                    Timber.w("STORAGE OVER QUOTA ERROR: ${e.errorCode}")
                                    updateProgressNotification()
                                }
                            }
                        }
                    }
                }
            null
        }
    }

    private fun showStorageOverQuotaNotification() {
        Timber.d("showStorageOverQuotaNotification")
        val contentText = getString(R.string.download_show_info)
        val message = getString(R.string.overquota_alert_title)
        val intent = Intent(this, ManagerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            action = if (isOverQuota == Constants.OVERQUOTA_STORAGE_STATE) {
                Constants.ACTION_OVERQUOTA_STORAGE
            } else {
                Constants.ACTION_PRE_OVERQUOTA_STORAGE
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_UPLOAD_ID,
                Constants.NOTIFICATION_CHANNEL_UPLOAD_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setShowBadge(true)
                setSound(null, null)
            }
            notificationManager?.createNotificationChannel(channel)
            NotificationCompat.Builder(
                applicationContext,
                Constants.NOTIFICATION_CHANNEL_UPLOAD_ID
            ).also { mBuilderCompatO ->
                mBuilderCompatO
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentIntent(
                        PendingIntent.getActivity(
                            applicationContext,
                            0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    .setAutoCancel(true).setTicker(contentText)
                    .setContentTitle(message).setContentText(contentText)
                    .setOngoing(false)

                notificationManager?.notify(
                    Constants.NOTIFICATION_STORAGE_OVERQUOTA,
                    mBuilderCompatO.build()
                )
            }
        } else {
            notificationBuilderCompat?.let { builder ->
                builder
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentIntent(
                        PendingIntent.getActivity(
                            applicationContext,
                            0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                    .setAutoCancel(true).setTicker(contentText)
                    .setContentTitle(message).setContentText(contentText)
                    .setOngoing(false)

                notificationManager?.notify(
                    Constants.NOTIFICATION_STORAGE_OVERQUOTA,
                    builder.build()
                )
            }

        }
    }

    private fun acquireLock() {
        Timber.d("acquireLock")
        wl?.takeIf { !it.isHeld }?.acquire()
        lock?.takeIf { !it.isHeld }?.acquire()
    }

    private fun releaseLocks() {
        Timber.d("releaseLocks")

        lock?.takeIf { it.isHeld }
            ?.runCatching { release() }
            ?.onFailure { err -> Timber.e(err, "EXCEPTION") }

        wl?.takeIf { it.isHeld }
            ?.runCatching { release() }
            ?.onFailure { err -> Timber.e(err, "EXCEPTION") }
    }

    private fun resetUploadNumbers() {
        Timber.d("resetUploadNumbers")
        pendingToAddInQueue = 0
        completed = 0
        completedSuccessfully = 0
        alreadyUploaded = 0
        uploadCount = 0
    }

    /**
     * Checks if a transfer is a CU or Chat transfer.
     *
     * @param transfer MegaTransfer to check
     * @return True if the transfer is a CU or Chat transfer, false otherwise.
     */
    private fun isCUOrChatTransfer(transfer: MegaTransfer): Boolean {
        val appData = transfer.appData
        return (!TextUtil.isTextEmpty(appData)
                && (appData.contains(Constants.APP_DATA_CU)
                || appData.contains(Constants.APP_DATA_CHAT)))
    }

    internal class UploadProgress {
        var total: Long = 0
        var inProgress: Long = 0
    }

    companion object {
        var ACTION_CANCEL = "CANCEL_UPLOAD"
        var EXTRA_FILE_PATH = "MEGA_FILE_PATH"
        var EXTRA_NAME = "MEGA_FILE_NAME"
        var EXTRA_LAST_MODIFIED = "MEGA_FILE_LAST_MODIFIED"
        var EXTRA_PARENT_HASH = "MEGA_PARENT_HASH"
        var EXTRA_UPLOAD_TXT = "EXTRA_UPLOAD_TXT"
    }
}
