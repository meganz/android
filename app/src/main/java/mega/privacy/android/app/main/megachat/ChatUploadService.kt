package mega.privacy.android.app.main.megachat

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
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.jeremyliao.liveeventbus.LiveEventBus
import com.shockwave.pdfium.PdfiumCore
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.AndroidCompletedTransfer
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.VideoDownsampling
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.data.extensions.isVoiceClipTransfer
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.globalmanagement.TransfersManagement.Companion.addCompletedTransfer
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.usecase.GetGlobalTransferUseCase
import mega.privacy.android.app.usecase.GetGlobalTransferUseCase.Result.OnTransferFinish
import mega.privacy.android.app.usecase.GetGlobalTransferUseCase.Result.OnTransferStart
import mega.privacy.android.app.usecase.GetGlobalTransferUseCase.Result.OnTransferTemporaryError
import mega.privacy.android.app.usecase.GetGlobalTransferUseCase.Result.OnTransferUpdate
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.CacheFolderManager.buildChatTempFile
import mega.privacy.android.app.utils.CacheFolderManager.buildVoiceClipFile
import mega.privacy.android.app.utils.CacheFolderManager.deleteCacheFolderIfEmpty
import mega.privacy.android.app.utils.CacheFolderManager.getCacheFolder
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.PreviewUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Service which should be only used for chat uploads.
 */
@AndroidEntryPoint
class ChatUploadService : Service(), MegaRequestListenerInterface,
    MegaChatRequestListenerInterface {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var megaChatApi: MegaChatApiAndroid

    @Inject
    lateinit var dbH: DatabaseHandler

    @ApplicationScope
    @Inject
    lateinit var sharingScope: CoroutineScope

    @Inject
    lateinit var transfersManagement: TransfersManagement

    @Inject
    lateinit var getGlobalTransferUseCase: GetGlobalTransferUseCase

    @Inject
    lateinit var chatPreferencesGateway: ChatPreferencesGateway

    private var isForeground = false
    private var canceled = false
    private var fileNames: HashMap<String, String>? = HashMap()
    var sendOriginalAttachments = false

    //0 - not over quota, not pre-over quota
    //1 - over quota
    //2 - pre-over quota
    private var isOverQuota = 0
    private var pendingMessages: ArrayList<PendingMessageSingle>? = null
    private var mapVideoDownsampling: HashMap<String, Int>? = null
    private var mapProgressTransfers: HashMap<Int, MegaTransfer>? = null
    private var app: MegaApplication? = null
    private var requestSent = 0
    private var lock: WifiLock? = null
    private var wl: PowerManager.WakeLock? = null
    private var transfersCount = 0
    private var numberVideosPending = 0
    private var totalVideos = 0
    private var totalUploadsCompleted = 0
    private var totalUploads = 0
    private var type: String? = ""
    private var parentNode: MegaNode? = null
    private var videoDownsampling: VideoDownsampling? = null
    private var mBuilder: Notification.Builder? = null
    private var mBuilderCompat: NotificationCompat.Builder? = null
    private var mNotificationManager: NotificationManager? = null
    private var fileExplorerUpload = false
    private var snackbarChatHandle = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
    private val rxSubscriptions = CompositeDisposable()

    /** the receiver and manager for the broadcast to listen to the pause event  */
    private var pauseBroadcastReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        app = application as MegaApplication
        pendingMessages = ArrayList()
        isForeground = false
        canceled = false
        isOverQuota = 0
        mapVideoDownsampling = HashMap()
        mapProgressTransfers = HashMap()
        val wifiLockMode = WifiManager.WIFI_MODE_FULL_HIGH_PERF
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        lock = wifiManager.createWifiLock(wifiLockMode, "MegaUploadServiceWifiLock")
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Mega:UploadServicePowerLock")
        @Suppress("DEPRECATION")
        mBuilder = Notification.Builder(this@ChatUploadService)
        mBuilderCompat = NotificationCompat.Builder(
            this@ChatUploadService,
            Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID
        )
        mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        startForeground()

        // delay 1 second to refresh the pause notification to prevent update is missed
        pauseBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Handler(Looper.getMainLooper()).postDelayed({ updateProgressNotification() }, 1000)
            }
        }
        registerReceiver(
            pauseBroadcastReceiver,
            IntentFilter(Constants.BROADCAST_ACTION_INTENT_UPDATE_PAUSE_NOTIFICATION)
        )

        getGlobalTransferUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .filter { it.transfer != null }
            .subscribeBy(
                onNext = { event ->
                    when (event) {
                        is OnTransferStart -> {
                            onTransferStart(event.transfer!!)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeBy(onError = { Timber.e(it) })
                                .addTo(rxSubscriptions)
                        }
                        is OnTransferUpdate -> {
                            onTransferUpdate(event.transfer!!)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeBy(onError = { Timber.e(it) })
                                .addTo(rxSubscriptions)
                        }
                        is OnTransferFinish -> {
                            onTransferFinish(event.transfer!!, event.error)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeBy(onError = { Timber.e(it) })
                                .addTo(rxSubscriptions)
                        }
                        is OnTransferTemporaryError -> {
                            onTransferTemporaryError(event.transfer!!, event.error)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribeBy(onError = { Timber.e(it) })
                                .addTo(rxSubscriptions)
                        }
                        is GetGlobalTransferUseCase.Result.OnTransferData -> {
                            // do nothing
                        }
                    }
                },
                onError = { Timber.e(it) }
            )
            .addTo(rxSubscriptions)
    }

    private fun startForeground() {
        @Suppress("DEPRECATION")
        if (megaApi.numPendingUploads <= 0) return

        isForeground = try {
            startForeground(
                Constants.NOTIFICATION_CHAT_UPLOAD,
                TransfersManagement.createInitialServiceNotification(
                    Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID,
                    Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_NAME, mNotificationManager!!,
                    NotificationCompat.Builder(
                        this@ChatUploadService,
                        Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID
                    ), mBuilder!!
                )
            )
            true
        } catch (e: Exception) {
            Timber.w(e, "Error starting foreground.")
            false
        }
    }

    private fun stopForeground() {
        isForeground = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        mNotificationManager?.cancel(Constants.NOTIFICATION_CHAT_UPLOAD)
        stopSelf()
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        if (lock != null && lock!!.isHeld) try {
            lock!!.release()
        } catch (ex: Exception) {
            Timber.e(ex)
        }
        if (wl != null && wl!!.isHeld) try {
            wl!!.release()
        } catch (ex: Exception) {
            Timber.e(ex)
        }
        megaApi.removeRequestListener(this)
        megaChatApi.saveCurrentState()
        unregisterReceiver(pauseBroadcastReceiver)
        rxSubscriptions.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("Flags: $flags, Start ID: $startId")
        canceled = false

        if (intent == null) {
            return START_NOT_STICKY
        }

        if (intent.action != null) {
            if (intent.action == ACTION_CANCEL) {
                Timber.d("Cancel intent")
                canceled = true
                megaApi.cancelTransfers(MegaTransfer.TYPE_UPLOAD, this)
                return START_NOT_STICKY
            }
        }

        isOverQuota = 0
        onHandleIntent(intent)
        return START_NOT_STICKY
    }

    private fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.action != null && intent.action == Constants.ACTION_RESTART_SERVICE) {
            val transferData = megaApi.getTransferData(null)
            if (transferData == null) {
                stopForeground()
                return
            }

            val uploadsInProgress = transferData.numUploads
            var voiceClipsInProgress = 0

            for (i in 0 until uploadsInProgress) {
                val transfer = megaApi.getTransferByTag(transferData.getUploadTag(i)) ?: continue
                val data = transfer.appData

                if (!TextUtil.isTextEmpty(data) && data.contains(Constants.APP_DATA_CHAT)) {
                    mapProgressTransfers!![transfer.tag] = transfer

                    if (transfer.isVoiceClipTransfer()) {
                        voiceClipsInProgress++
                    } else {
                        transfersManagement.checkIfTransferIsPaused(transfer)
                    }
                }
            }

            totalUploads = mapProgressTransfers!!.size - voiceClipsInProgress
            transfersCount = totalUploads

            if (totalUploads > 0) {
                updateProgressNotification()
            } else {
                stopForeground()
            }

            LiveEventBus.get(EventConstants.EVENT_TRANSFER_UPDATE, Int::class.java)
                .post(MegaTransfer.TYPE_UPLOAD)

            return
        } else if (Constants.ACTION_CHECK_COMPRESSING_MESSAGE == intent.action) {
            checkCompressingMessage(intent)
            return
        }

        val pendingMessageSingles = ArrayList<PendingMessageSingle>()
        parentNode = MegaNode.unserialize(intent.getStringExtra(EXTRA_PARENT_NODE))

        if (intent.hasExtra(EXTRA_NAME_EDITED)) {
            @Suppress("UNCHECKED_CAST")
            fileNames = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(EXTRA_NAME_EDITED, HashMap::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(EXTRA_NAME_EDITED)
            } as HashMap<String, String>?
        }

        if (intent.getBooleanExtra(EXTRA_COMES_FROM_FILE_EXPLORER, false)) {
            fileExplorerUpload = true
            @Suppress("UNCHECKED_CAST")
            val fileFingerprints = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(EXTRA_UPLOAD_FILES_FINGERPRINTS, HashMap::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(EXTRA_UPLOAD_FILES_FINGERPRINTS)
            } as HashMap<String, String>?

            val idPendMsgs = intent.getLongArrayExtra(EXTRA_PEND_MSG_IDS)
            val attachFiles = intent.getLongArrayExtra(EXTRA_ATTACH_FILES)
            val idChats = intent.getLongArrayExtra(EXTRA_ATTACH_CHAT_IDS)
            val validIdChats = idChats != null && idChats.isNotEmpty()
            var onlyOneChat = true

            if (attachFiles != null && attachFiles.isNotEmpty() && validIdChats) {
                for (attachFile in attachFiles) {
                    for (idChat in idChats!!) {
                        requestSent++
                        megaChatApi.attachNode(idChat, attachFile, this)
                    }
                }
            }

            if (validIdChats) {
                if (idChats!!.size == 1) {
                    snackbarChatHandle = idChats[0]
                } else {
                    onlyOneChat = false
                }
            }

            if (idPendMsgs != null && idPendMsgs.isNotEmpty() && fileFingerprints != null && fileFingerprints.isNotEmpty()) {
                for (entry in fileFingerprints.entries) {
                    val fingerprint = entry.key
                    val path = entry.value
                    totalUploads++

                    if (!wl!!.isHeld) {
                        wl!!.acquire()
                    }

                    if (!lock!!.isHeld) {
                        lock!!.acquire()
                    }

                    pendingMessageSingles.clear()

                    for (i in idPendMsgs.indices) {
                        var pendingMsg: PendingMessageSingle?

                        if (idPendMsgs[i] != -1L) {
                            pendingMsg = dbH.findPendingMessageById(idPendMsgs[i])
                            //									One transfer for file --> onTransferFinish() attach to all selected chats
                            if (pendingMsg != null && pendingMsg.getChatId() != -1L
                                && path == pendingMsg.getFilePath()
                                && fingerprint == pendingMsg.getFingerprint()
                            ) {
                                pendingMessageSingles.add(pendingMsg)

                                if (onlyOneChat) {
                                    if (snackbarChatHandle == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                                        snackbarChatHandle = pendingMsg.getChatId()
                                    } else if (snackbarChatHandle != pendingMsg.getChatId()) {
                                        onlyOneChat = false
                                    }
                                }
                            }
                        }
                    }

                    initUpload(pendingMessageSingles, null)
                }
            }
        } else {
            val chatId = intent.getLongExtra(EXTRA_CHAT_ID, -1)
            type = intent.getStringExtra(Constants.EXTRA_TRANSFER_TYPE)
            val idPendMsg = intent.getLongExtra(EXTRA_ID_PEND_MSG, -1)
            var pendingMsg: PendingMessageSingle? = null

            if (idPendMsg != -1L) {
                pendingMsg = dbH.findPendingMessageById(idPendMsg)
            }

            if (pendingMsg != null) {
                sendOriginalAttachments =
                    dbH.chatVideoQuality == VideoQuality.ORIGINAL.value
                Timber.d("sendOriginalAttachments is $sendOriginalAttachments")

                if (chatId != -1L) {
                    Timber.d("The chat ID is: $chatId")
                    if (type == null || type != Constants.APP_DATA_VOICE_CLIP) {
                        totalUploads++
                    }
                    if (!wl!!.isHeld) {
                        wl!!.acquire()
                    }
                    if (!lock!!.isHeld) {
                        lock!!.acquire()
                    }
                    pendingMessageSingles.clear()
                    pendingMessageSingles.add(pendingMsg)
                    initUpload(pendingMessageSingles, type)
                }
            } else {
                Timber.e("Error the chatId is not correct: $chatId")
            }
        }
    }

    private fun initUpload(pendingMsgs: ArrayList<PendingMessageSingle>, type: String?) {
        Timber.d("initUpload")
        val pendingMsg = pendingMsgs[0]
        val file = File(pendingMsg.getFilePath())

        sharingScope.launch {
            chatPreferencesGateway
                .getChatImageQualityPreference().collectLatest { imageQuality ->
                    val shouldCompressImage = MimeTypeList.typeForName(file.name).isImage
                            && !MimeTypeList.typeForName(file.name).isGIF
                            && (imageQuality == ChatImageQuality.Optimised
                            || (imageQuality == ChatImageQuality.Automatic
                            && Util.isOnMobileData(this@ChatUploadService)))

                    if (shouldCompressImage) {
                        val compressedFile = ChatUtil.checkImageBeforeUpload(file)
                        val uploadPath = if (FileUtil.isFileAvailable(compressedFile)) {
                            val fingerprint = megaApi.getFingerprint(compressedFile.absolutePath)
                            for (pendMsg in pendingMsgs) {
                                if (fingerprint != null) {
                                    pendMsg.setFingerprint(fingerprint)
                                }
                                pendingMessages!!.add(pendMsg)
                            }
                            compressedFile.absolutePath
                        } else {
                            pendingMessages!!.addAll(pendingMsgs)
                            pendingMsg.getFilePath()
                        }

                        startUpload(pendingMsg.id, type, fileNames!![pendingMsg.name], uploadPath)
                    } else if (MimeTypeList.typeForName(file.name).isMp4Video && !sendOriginalAttachments) {
                        Timber.d("DATA connection is Mp4Video")
                        try {
                            val chatTempFolder = getCacheFolder(
                                applicationContext,
                                CacheFolderManager.CHAT_TEMPORARY_FOLDER
                            )
                            var outFile = buildChatTempFile(applicationContext, file.name)
                            var index = 0

                            if (outFile != null) {
                                while (outFile!!.exists()) {
                                    if (index > 0) {
                                        outFile = File(chatTempFolder!!.absolutePath, file.name)
                                    }

                                    index++
                                    val outFilePath = outFile.absolutePath
                                    val splitByDot = outFilePath.split("\\.").toTypedArray()
                                    var ext = ""

                                    if (splitByDot.size > 1) {
                                        ext = splitByDot[splitByDot.size - 1]
                                    }

                                    var fileName = outFilePath
                                        .substring(outFilePath.lastIndexOf(File.separator) + 1)

                                    fileName =
                                        if (ext.isNotEmpty()) {
                                            fileName.replace(
                                                ".$ext",
                                                "_" + index + FileUtil.MP4_EXTENSION
                                            )
                                        } else {
                                            fileName + "_" + index + FileUtil.MP4_EXTENSION
                                        }

                                    outFile = File(chatTempFolder!!.absolutePath, fileName)
                                }

                                outFile.createNewFile()
                            }

                            if (outFile == null) {
                                addPendingMessagesAndStartUpload(
                                    pendingMsg.getId(),
                                    type,
                                    fileNames!![pendingMsg.getName()],
                                    pendingMsg.getFilePath(),
                                    pendingMsgs
                                )
                            } else {
                                totalVideos++
                                numberVideosPending++
                                for (pendMsg in pendingMsgs) {
                                    pendMsg.setVideoDownSampled(outFile.absolutePath)
                                    pendingMessages!!.add(pendMsg)
                                }
                                mapVideoDownsampling!![outFile.absolutePath] = 0
                                if (videoDownsampling == null) {
                                    videoDownsampling = VideoDownsampling(this@ChatUploadService)
                                }
                                videoDownsampling!!.changeResolution(
                                    file, outFile.absolutePath,
                                    pendingMsg.getId(), dbH.chatVideoQuality
                                )
                            }
                        } catch (throwable: Throwable) {
                            Timber.e("EXCEPTION: Video cannot be downsampled", throwable)
                            addPendingMessagesAndStartUpload(
                                pendingMsg.getId(),
                                type,
                                fileNames!![pendingMsg.getName()],
                                pendingMsg.getFilePath(),
                                pendingMsgs
                            )
                        }
                    } else {
                        addPendingMessagesAndStartUpload(
                            pendingMsg.getId(), type,
                            fileNames!![pendingMsg.getName()], pendingMsg.getFilePath(), pendingMsgs
                        )
                    }

                    if (megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)
                        && !transfersManagement.hasResumeTransfersWarningAlreadyBeenShown
                    ) {
                        sendBroadcast(Intent(BroadcastConstants.BROADCAST_ACTION_RESUME_TRANSFERS))
                    }

                    this.cancel()
                }
        }
    }

    /**
     * Adds pending messages to general list and starts the upload.
     *
     * @param idPendingMessage Identifier of pending message.
     * @param type             Type of upload file.
     * @param fileName         Name of the file if set, null otherwise.
     * @param localPath        Local path of the file to upload.
     * @param pendingMsgs       List of pending Messages.
     */
    private fun addPendingMessagesAndStartUpload(
        idPendingMessage: Long, type: String?, fileName: String?,
        localPath: String, pendingMsgs: ArrayList<PendingMessageSingle>,
    ) {
        pendingMessages!!.addAll(pendingMsgs)
        startUpload(idPendingMessage, type, fileName, localPath)
    }

    /**
     * Starts the upload.
     *
     * @param idPendingMessage Identifier of pending message.
     * @param type             Type of upload file.
     * @param fileName         Name of the file if set, null otherwise.
     * @param localPath        Local path of the file to upload.
     */
    private fun startUpload(
        idPendingMessage: Long,
        type: String?,
        fileName: String?,
        localPath: String,
    ) {
        var data = Constants.APP_DATA_CHAT + Constants.APP_DATA_INDICATOR + idPendingMessage

        if (type != null && type == Constants.APP_DATA_VOICE_CLIP) {
            data = Constants.APP_DATA_VOICE_CLIP + Constants.APP_DATA_SEPARATOR + data
        }

        megaApi.startUploadForChat(localPath, parentNode, data, false, fileName)
    }

    /**
     * Stop uploading service
     */
    private fun cancel() {
        Timber.d("cancel")
        canceled = true
        stopForeground()
    }

    /**
     * No more intents in the queue
     */
    private fun onQueueComplete() {
        Timber.d("onQueueComplete")
        //Review when is called
        if (lock != null && lock!!.isHeld) try {
            lock!!.release()
        } catch (ex: Exception) {
            Timber.e(ex)
        }
        if (wl != null && wl!!.isHeld) try {
            wl!!.release()
        } catch (ex: Exception) {
            Timber.e(ex)
        }

        if (isOverQuota != 0) {
            showStorageOverquotaNotification()
        }

        Timber.d("Reset figures of chatUploadService")
        numberVideosPending = 0
        totalVideos = 0
        totalUploads = 0
        totalUploadsCompleted = 0

        @Suppress("DEPRECATION")
        if (megaApi.numPendingUploads <= 0) {
            megaApi.resetTotalUploads()
        }

        if (fileExplorerUpload) {
            fileExplorerUpload = false
            sendBroadcast(
                Intent(BroadcastConstants.BROADCAST_ACTION_INTENT_SHOWSNACKBAR_TRANSFERS_FINISHED)
                    .putExtra(BroadcastConstants.FILE_EXPLORER_CHAT_UPLOAD, true)
                    .putExtra(Constants.CHAT_ID, snackbarChatHandle)
            )
            snackbarChatHandle = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
        }

        Timber.d("Stopping service!!")
        transfersManagement.hasResumeTransfersWarningAlreadyBeenShown = false
        stopForeground()
        Timber.d("After stopSelf")

        try {
            deleteCacheFolderIfEmpty(applicationContext, CacheFolderManager.TEMPORARY_FOLDER)
        } catch (e: Exception) {
            Timber.e("EXCEPTION: pathSelfie not deleted", e)
        }
    }

    fun updateProgressDownsampling(percentage: Int, key: String) {
        mapVideoDownsampling!![key] = percentage
        updateProgressNotification()
    }

    fun finishDownsampling(returnedFile: String, success: Boolean, idPendingMessage: Long) {
        Timber.d("success: $success, idPendingMessage: $idPendingMessage")
        numberVideosPending--
        var downFile: File? = null
        var fileName: String? = null

        if (success) {
            mapVideoDownsampling!![returnedFile] = 100
            downFile = File(returnedFile)

            for (i in pendingMessages!!.indices) {
                val pendMsg = pendingMessages!![i]

                if (idPendingMessage == pendMsg.id) {
                    fileName = fileNames!![pendMsg.name]
                }

                if (pendMsg.getVideoDownSampled() != null && pendMsg.getVideoDownSampled() == returnedFile) {
                    val fingerPrint = megaApi.getFingerprint(returnedFile)

                    if (fingerPrint != null) {
                        pendMsg.setFingerprint(fingerPrint)
                    }
                }
            }
        } else {
            mapVideoDownsampling!!.remove(returnedFile)

            for (i in pendingMessages!!.indices) {
                val pendMsg = pendingMessages!![i]

                if (idPendingMessage == pendMsg.id) {
                    fileName = fileNames!![pendMsg.name]
                }

                if (pendMsg.getVideoDownSampled() != null) {
                    if (pendMsg.getVideoDownSampled() == returnedFile) {
                        pendMsg.setVideoDownSampled(null)
                        downFile = File(pendMsg.getFilePath())
                        Timber.d("Found the downFile")
                    }
                } else {
                    Timber.e("Error message could not been downsampled")
                }
            }

            if (downFile != null) {
                mapVideoDownsampling!![downFile.absolutePath] = 100
            }
        }

        if (downFile != null) {
            startUpload(idPendingMessage, null, fileName, downFile.path)
        }
    }

    @SuppressLint("NewApi")
    private fun updateProgressNotification() {
        var progressPercent: Long = 0
        val transfers: Collection<MegaTransfer> = mapProgressTransfers!!.values

        if (sendOriginalAttachments) {
            var total: Long = 0
            var inProgress: Long = 0

            for (currentTransfer in transfers) {
                if (!currentTransfer.isVoiceClipTransfer()) {
                    if (currentTransfer.state == MegaTransfer.STATE_COMPLETED) {
                        total += currentTransfer.totalBytes
                        inProgress += currentTransfer.totalBytes
                    } else {
                        total += currentTransfer.totalBytes
                        inProgress += currentTransfer.transferredBytes
                    }
                }
            }

            if (total > 0) {
                progressPercent = inProgress * 100 / total
            }
        } else if (totalVideos > 0) {
            for (currentTransfer in transfers) {
                if (!currentTransfer.isVoiceClipTransfer()) {
                    val individualInProgress = currentTransfer.transferredBytes
                    val individualTotalBytes = currentTransfer.totalBytes
                    var individualProgressPercent: Long = 0

                    if (currentTransfer.state == MegaTransfer.STATE_COMPLETED) {
                        individualProgressPercent =
                            if (MimeTypeList.typeForName(currentTransfer.fileName).isMp4Video) {
                                50
                            } else {
                                100
                            }
                    } else if (individualTotalBytes > 0) {
                        individualProgressPercent =
                            if (MimeTypeList.typeForName(currentTransfer.fileName).isMp4Video) {
                                individualInProgress * 50 / individualTotalBytes
                            } else {
                                individualInProgress * 100 / individualTotalBytes
                            }
                    }

                    progressPercent += individualProgressPercent / totalUploads
                }
            }
            val values: Collection<Int> = mapVideoDownsampling!!.values
            val simplePercentage = 50 / totalUploads

            for (value in values) {
                val downsamplingPercent = simplePercentage * value / 100
                progressPercent += downsamplingPercent
            }
        } else {
            var total: Long = 0
            var inProgress: Long = 0

            for (currentTransfer in transfers) {
                if (!currentTransfer.isVoiceClipTransfer()) {
                    total += currentTransfer.totalBytes
                    inProgress += currentTransfer.transferredBytes
                }
            }

            inProgress *= 100
            progressPercent = if (total <= 0) 0 else inProgress / total
        }

        Timber.d("Progress: $progressPercent")
        val message: String = if (isOverQuota != 0) {
            getString(R.string.overquota_alert_title)
        } else {
            val inProgress =
                if (totalUploadsCompleted == totalUploads) totalUploadsCompleted
                else totalUploadsCompleted + 1

            val videosCompressed = videosCompressed

            if (megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)) {
                StringResourcesUtils.getString(
                    R.string.upload_service_notification_paused,
                    inProgress, totalUploads
                )
            } else if (thereAreChatUploads() || videosCompressed == mapVideoDownsampling!!.size) {
                StringResourcesUtils.getString(
                    R.string.upload_service_notification,
                    inProgress,
                    totalUploads
                )
            } else {
                StringResourcesUtils.getString(
                    R.string.title_compress_video,
                    videosCompressed + 1, mapVideoDownsampling!!.size
                )
            }
        }

        val intent = Intent(this@ChatUploadService, ManagerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

        when (isOverQuota) {
            0 -> {
                intent.action = Constants.ACTION_SHOW_TRANSFERS
                intent.putExtra(Constants.OPENED_FROM_CHAT, true)
            }
            1 -> intent.action = Constants.ACTION_OVERQUOTA_STORAGE
            2 -> intent.action = Constants.ACTION_PRE_OVERQUOTA_STORAGE
            else -> {
                intent.action = Constants.ACTION_SHOW_TRANSFERS
                intent.putExtra(Constants.OPENED_FROM_CHAT, true)
            }
        }

        val actionString =
            if (isOverQuota == 0) getString(R.string.chat_upload_title_notification)
            else getString(R.string.general_show_info)

        val pendingIntent = PendingIntent.getActivity(
            this@ChatUploadService,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID,
                Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setShowBadge(true)
            channel.setSound(null, null)
            mNotificationManager?.createNotificationChannel(channel)
            mBuilderCompat?.apply {
                setSmallIcon(R.drawable.ic_stat_notify)
                setProgress(100, progressPercent.toInt(), false)
                setContentIntent(pendingIntent)
                setOngoing(true).setContentTitle(message)
                setContentText(actionString)
                setOnlyAlertOnce(true).color =
                    ContextCompat.getColor(this@ChatUploadService, R.color.red_600_red_300)
            }
            notification = mBuilderCompat!!.build()
        } else {
            mBuilder?.apply {
                setSmallIcon(R.drawable.ic_stat_notify)
                setProgress(100, progressPercent.toInt(), false)
                setContentIntent(pendingIntent)
                setOngoing(true).setContentTitle(message)
                setContentText(actionString)
                setOnlyAlertOnce(true)
                setColor(ContextCompat.getColor(this@ChatUploadService, R.color.red_600_red_300))
            }

            notification = mBuilder!!.build()
        }

        if (!isForeground) {
            Timber.d("Starting foreground")

            isForeground = try {
                startForeground(Constants.NOTIFICATION_CHAT_UPLOAD, notification)
                true
            } catch (e: Exception) {
                Timber.e("startForeground EXCEPTION", e)
                false
            }
        } else {
            mNotificationManager!!.notify(Constants.NOTIFICATION_CHAT_UPLOAD, notification)
        }
    }

    private fun onTransferStart(transfer: MegaTransfer): Completable =
        Completable.fromAction {
            if (transfer.type == MegaTransfer.TYPE_UPLOAD) {
                Timber.d("onTransferStart: ${transfer.nodeHandle}")
                val appData = transfer.appData ?: return@fromAction

                if (appData.contains(Constants.APP_DATA_CHAT)) {
                    LiveEventBus.get(EventConstants.EVENT_TRANSFER_UPDATE, Int::class.java)
                        .post(MegaTransfer.TYPE_UPLOAD)

                    Timber.d("This is a chat upload: $appData")

                    if (!transfer.isVoiceClipTransfer()) {
                        transfersCount++
                    }

                    if (transfer.isStreamingTransfer) {
                        return@fromAction
                    }

                    val id = ChatUtil.getPendingMessageIdFromAppData(appData)
                    sendBroadcast(
                        Intent(BroadcastConstants.BROADCAST_ACTION_CHAT_TRANSFER_START)
                            .putExtra(BroadcastConstants.PENDING_MESSAGE_ID, id)
                    )

                    //Update status and tag on db
                    dbH.updatePendingMessageOnTransferStart(id, transfer.tag)
                    mapProgressTransfers!![transfer.tag] = transfer

                    if (!transfer.isFolderTransfer && !transfer.isVoiceClipTransfer()) {
                        updateProgressNotification()
                    }
                }
            }
        }

    private fun onTransferUpdate(transfer: MegaTransfer): Completable =
        Completable.fromAction {
            if (transfer.type == MegaTransfer.TYPE_UPLOAD) {
                LiveEventBus.get(EventConstants.EVENT_TRANSFER_UPDATE, Int::class.java)
                    .post(MegaTransfer.TYPE_UPLOAD)
                Timber.d("onTransferUpdate: ${transfer.nodeHandle}")
                val appData = transfer.appData

                if (appData?.contains(Constants.APP_DATA_CHAT) == false
                    || transfer.isStreamingTransfer
                    || transfer.isStreamingTransfer
                ) return@fromAction

                if (canceled) {
                    Timber.w("Transfer cancel: ${transfer.nodeHandle}")
                    if (lock != null && lock!!.isHeld) try {
                        lock!!.release()
                    } catch (ex: Exception) {
                        Timber.e(ex)
                    }
                    if (wl != null && wl!!.isHeld) try {
                        wl!!.release()
                    } catch (ex: Exception) {
                        Timber.e(ex)
                    }

                    megaApi.cancelTransfer(transfer)
                    cancel()
                    Timber.d("After cancel")
                    return@fromAction
                }

                LiveEventBus.get(EventConstants.EVENT_TRANSFER_UPDATE, Int::class.java)
                    .post(MegaTransfer.TYPE_UPLOAD)

                if (isOverQuota != 0) {
                    Timber.w("After overquota error")
                    isOverQuota = 0
                }

                mapProgressTransfers!![transfer.tag] = transfer

                if (!transfer.isVoiceClipTransfer()) {
                    updateProgressNotification()
                }
            }
        }

    private fun onTransferTemporaryError(transfer: MegaTransfer, e: MegaError): Completable =
        Completable.fromAction {
            Timber.w("Handle: ${transfer.nodeHandle}. Upload Temporary Error: ${e.errorString}__${e.errorCode}".trimIndent())

            if (transfer.type == MegaTransfer.TYPE_UPLOAD) {
                when (e.errorCode) {
                    MegaError.API_EOVERQUOTA, MegaError.API_EGOINGOVERQUOTA -> {
                        if (e.errorCode == MegaError.API_EOVERQUOTA) {
                            isOverQuota = 1
                        } else if (e.errorCode == MegaError.API_EGOINGOVERQUOTA) {
                            isOverQuota = 2
                        }

                        if (e.value != 0L) {
                            Timber.w("TRANSFER OVERQUOTA ERROR: ${e.errorCode}")
                        } else {
                            Timber.w("STORAGE OVERQUOTA ERROR: ${e.errorCode}")

                            if (!transfer.isVoiceClipTransfer()) {
                                updateProgressNotification()
                            }
                        }
                    }
                }
            }
        }

    private fun onTransferFinish(transfer: MegaTransfer, error: MegaError): Completable =
        Completable.fromAction {
            if (error.errorCode == MegaError.API_EBUSINESSPASTDUE) {
                sendBroadcast(Intent(Constants.BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED))
            }

            if (transfer.type == MegaTransfer.TYPE_UPLOAD) {
                Timber.d("onTransferFinish: ${transfer.nodeHandle}")
                val appData = transfer.appData

                if (appData != null && appData.contains(Constants.APP_DATA_CHAT)) {
                    if (transfer.isStreamingTransfer) {
                        return@fromAction
                    }

                    if (!transfer.isVoiceClipTransfer()) {
                        transfersCount--
                        totalUploadsCompleted++
                    }

                    addCompletedTransfer(AndroidCompletedTransfer(transfer, error), dbH)
                    mapProgressTransfers!![transfer.tag] = transfer

                    LiveEventBus.get(EventConstants.EVENT_TRANSFER_UPDATE, Int::class.java)
                        .post(MegaTransfer.TYPE_UPLOAD)

                    if (canceled) {
                        Timber.w("Upload cancelled: ${transfer.nodeHandle}")
                        if (lock != null && lock!!.isHeld) try {
                            lock!!.release()
                        } catch (ex: Exception) {
                            Timber.e(ex)
                        }
                        if (wl != null && wl!!.isHeld) try {
                            wl!!.release()
                        } catch (ex: Exception) {
                            Timber.e(ex)
                        }

                        cancel()
                        Timber.d("After cancel")

                        if (transfer.isVoiceClipTransfer()) {
                            val localFile = buildVoiceClipFile(this, transfer.fileName)

                            if (FileUtil.isFileAvailable(localFile) && localFile!!.name != transfer.fileName) {
                                localFile.delete()
                            }
                        } else {
                            //Delete recursively all files and folder-??????
                            deleteCacheFolderIfEmpty(
                                applicationContext,
                                CacheFolderManager.TEMPORARY_FOLDER
                            )
                        }
                    } else {
                        if (error.errorCode == MegaError.API_OK) {
                            Timber.d("Upload OK: ${transfer.nodeHandle}")

                            if (FileUtil.isVideoFile(transfer.path)) {
                                Timber.d("Is video!!!")
                                val previewDir = PreviewUtils.getPreviewFolder(this)
                                val preview = File(
                                    previewDir,
                                    MegaApiAndroid.handleToBase64(transfer.nodeHandle) + JPG_EXTENSION
                                )
                                val thumbDir = ThumbnailUtils.getThumbFolder(this)
                                val thumb = File(
                                    thumbDir,
                                    MegaApiAndroid.handleToBase64(transfer.nodeHandle) + JPG_EXTENSION
                                )
                                megaApi.createThumbnail(transfer.path, thumb.absolutePath)
                                megaApi.createPreview(transfer.path, preview.absolutePath)
                                attachNodes(transfer)
                            } else if (MimeTypeList.typeForName(transfer.path).isImage) {
                                Timber.d("Is image!!!")
                                val previewDir = PreviewUtils.getPreviewFolder(this)
                                val preview = File(
                                    previewDir,
                                    MegaApiAndroid.handleToBase64(transfer.nodeHandle) + JPG_EXTENSION
                                )
                                megaApi.createPreview(transfer.path, preview.absolutePath)
                                val thumbDir = ThumbnailUtils.getThumbFolder(this)
                                val thumb = File(
                                    thumbDir,
                                    MegaApiAndroid.handleToBase64(transfer.nodeHandle) + JPG_EXTENSION
                                )
                                megaApi.createThumbnail(transfer.path, thumb.absolutePath)
                                attachNodes(transfer)
                            } else if (MimeTypeList.typeForName(transfer.path).isPdf) {
                                Timber.d("Is pdf!!!")
                                try {
                                    ThumbnailUtils.createThumbnailPdf(
                                        this,
                                        transfer.path,
                                        megaApi,
                                        transfer.nodeHandle
                                    )
                                } catch (e: Exception) {
                                    Timber.e("Pdf thumbnail could not be created", e)
                                }

                                val pageNumber = 0
                                var out: FileOutputStream? = null
                                try {
                                    val pdfiumCore = PdfiumCore(this)
                                    val pdfNode = megaApi.getNodeByHandle(transfer.nodeHandle)

                                    if (pdfNode == null) {
                                        Timber.e("pdf is NULL")
                                        return@fromAction
                                    }

                                    val previewDir = PreviewUtils.getPreviewFolder(this)
                                    val preview = File(
                                        previewDir,
                                        MegaApiAndroid.handleToBase64(transfer.nodeHandle) + JPG_EXTENSION
                                    )
                                    val file = File(transfer.path)
                                    val pdfDocument = pdfiumCore.newDocument(
                                        ParcelFileDescriptor.open(
                                            file,
                                            ParcelFileDescriptor.MODE_READ_ONLY
                                        )
                                    )
                                    pdfiumCore.openPage(pdfDocument, pageNumber)
                                    val width =
                                        pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber)
                                    val height =
                                        pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber)
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
                                        val oldPreview =
                                            File(previewDir, transfer.fileName + JPG_EXTENSION)
                                        if (oldPreview.exists()) {
                                            oldPreview.delete()
                                        }
                                    } else {
                                        Timber.d("Not Compress")
                                    }

                                    //Attach node one the request finish
                                    requestSent++
                                    megaApi.setPreview(pdfNode, preview.absolutePath, this)
                                    pdfiumCore.closeDocument(pdfDocument)
                                    updatePdfAttachStatus(transfer)
                                } catch (e: Exception) {
                                    Timber.e("Pdf preview could not be created", e)
                                    attachNodes(transfer)
                                } finally {
                                    try {
                                        out?.close()
                                    } catch (e: Exception) {
                                        Timber.e(e)
                                    }
                                }
                            } else if (transfer.isVoiceClipTransfer()) {
                                Timber.d("Is voice clip")
                                attachVoiceClips(transfer)
                            } else {
                                Timber.d("NOT video, image or pdf!")
                                attachNodes(transfer)
                            }
                        } else {
                            Timber.e("Upload Error: ${transfer.nodeHandle}_${error.errorCode}___${error.errorString}")
                            if (error.errorCode == MegaError.API_EEXIST) {
                                Timber.w("Transfer API_EEXIST: ${transfer.nodeHandle}")
                            } else {
                                if (error.errorCode == MegaError.API_EOVERQUOTA) {
                                    isOverQuota = 1
                                } else if (error.errorCode == MegaError.API_EGOINGOVERQUOTA) {
                                    isOverQuota = 2
                                }

                                val id = ChatUtil.getPendingMessageIdFromAppData(appData)
                                //Update status and tag on db
                                dbH.updatePendingMessageOnTransferFinish(
                                    id,
                                    "-1",
                                    PendingMessageSingle.STATE_ERROR_UPLOADING
                                )

                                launchErrorToChat(id)

                                if (totalUploadsCompleted == totalUploads && transfersCount == 0 && numberVideosPending <= 0 && requestSent <= 0) {
                                    onQueueComplete()
                                    return@fromAction
                                }
                            }
                        }

                        val tempPic =
                            getCacheFolder(applicationContext, CacheFolderManager.TEMPORARY_FOLDER)

                        Timber.d("IN Finish: ${transfer.nodeHandle}")

                        if (FileUtil.isFileAvailable(tempPic) && transfer.path != null) {
                            if (transfer.path.startsWith(tempPic!!.absolutePath)) {
                                val f = File(transfer.path)
                                f.delete()
                            }
                        } else {
                            Timber.e("transfer.getPath() is NULL or temporal folder unavailable")
                        }
                    }

                    if (totalUploadsCompleted == totalUploads && transfersCount == 0 && numberVideosPending <= 0 && requestSent <= 0) {
                        onQueueComplete()
                    } else if (!transfer.isVoiceClipTransfer()) {
                        updateProgressNotification()
                    }
                }
            }
        }

    /**
     * Checks if pendingMessages list is empty.
     * If not, do nothing.
     * If so, means the service has been restarted after a transfers resumption or some error happened
     * and tries to get the PendingMessageSingle related to the current MegaTransfer from DB.
     * If the PendingMessageSingle exists, attaches it to the chat conversation.
     *
     * @param id       Identifier of PendingMessageSingle.
     * @param transfer Current MegaTransfer.
     * @return True if the list is empty, false otherwise.
     */
    private fun arePendingMessagesEmpty(id: Long, transfer: MegaTransfer): Boolean {
        if (pendingMessages != null && pendingMessages!!.isNotEmpty()) {
            return false
        }
        attachMessageFromDB(id, transfer)
        return true
    }

    /**
     * Attaches a message to a chat conversation getting it from DB.
     *
     * @param id       Identifier of PendingMessageSingle.
     * @param transfer Current MegaTransfer.
     */
    private fun attachMessageFromDB(id: Long, transfer: MegaTransfer) {
        val pendingMessage = dbH.findPendingMessageById(id)
        if (pendingMessage != null) {
            pendingMessages!!.add(pendingMessage)
            attach(pendingMessage, transfer)
        } else {
            Timber.e("Message not found and not attached.")
        }
    }

    fun attachNodes(transfer: MegaTransfer) {
        Timber.d("attachNodes()")
        //Find the pending message
        val appData = transfer.appData
        val id = ChatUtil.getPendingMessageIdFromAppData(appData)
        //Update status and nodeHandle on db
        dbH.updatePendingMessageOnTransferFinish(
            id,
            transfer.nodeHandle.toString() + "",
            PendingMessageSingle.STATE_ATTACHING
        )

        if (arePendingMessagesEmpty(id, transfer)) {
            return
        }

        val fingerprint = megaApi.getFingerprint(transfer.path)
        var msgNotFound = true

        for (pendMsg in pendingMessages!!) {
            if (pendMsg.getId() == id || pendMsg.getFingerprint() == fingerprint) {
                attach(pendMsg, transfer)
                msgNotFound = false
            }
        }

        if (msgNotFound) {
            //Message not found, try to attach from DB
            attachMessageFromDB(id, transfer)
        }
    }

    fun attach(pendMsg: PendingMessageSingle, transfer: MegaTransfer) {
        Timber.d("attach")
        requestSent++
        pendMsg.setNodeHandle(transfer.nodeHandle)
        pendMsg.setState(PendingMessageSingle.STATE_ATTACHING)
        megaChatApi.attachNode(pendMsg.getChatId(), transfer.nodeHandle, this)

        if (FileUtil.isVideoFile(transfer.path)) {
            val pathDownsampled = pendMsg.getVideoDownSampled()
            if (transfer.path == pathDownsampled) {
                //Delete the local temp video file
                val f = File(transfer.path)
                if (f.exists()) {
                    val deleted = f.delete()
                    if (!deleted) {
                        Timber.e("ERROR: Local file not deleted!")
                    }
                }
            }
        }
    }

    fun attachVoiceClips(transfer: MegaTransfer) {
        Timber.d("attachVoiceClips()")
        //Find the pending message
        val id = ChatUtil.getPendingMessageIdFromAppData(transfer.appData)
        //Update status and nodeHandle on db
        dbH.updatePendingMessageOnTransferFinish(
            id,
            transfer.nodeHandle.toString() + "",
            PendingMessageSingle.STATE_ATTACHING
        )

        if (arePendingMessagesEmpty(id, transfer)) {
            return
        }

        for (pendMsg in pendingMessages!!) {
            if (pendMsg.getId() == id) {
                pendMsg.setNodeHandle(transfer.nodeHandle)
                pendMsg.setState(PendingMessageSingle.STATE_ATTACHING)
                megaChatApi.attachVoiceMessage(pendMsg.getChatId(), transfer.nodeHandle, this)
                return
            }
        }

        //Message not found, try to attach from DB
        attachMessageFromDB(id, transfer)
    }

    fun updatePdfAttachStatus(transfer: MegaTransfer) {
        Timber.d("updatePdfAttachStatus")
        //Find the pending message
        for (i in pendingMessages!!.indices) {
            val pendMsg = pendingMessages!![i]

            if (pendMsg.getFilePath() == transfer.path) {
                if (pendMsg.getNodeHandle() == -1L) {
                    Timber.d("Set node handle to the pdf file: ${transfer.nodeHandle}")
                    pendMsg.setNodeHandle(transfer.nodeHandle)
                } else {
                    Timber.e("Set node handle error")
                }
            }
        }

        //Update node handle in db
        val id = ChatUtil.getPendingMessageIdFromAppData(transfer.appData)
        //Update status and nodeHandle on db
        dbH.updatePendingMessageOnTransferFinish(
            id,
            transfer.nodeHandle.toString() + "",
            PendingMessageSingle.STATE_ATTACHING
        )

        if (pendingMessages != null && pendingMessages!!.isNotEmpty()) {
            for (pendMsg in pendingMessages!!) {
                if (pendMsg.getId() == id) {
                    return
                }
            }
        }

        //Message not found, try to get it from DB.
        val pendingMessage = dbH.findPendingMessageById(id)

        if (pendingMessage != null) {
            pendingMessages!!.add(pendingMessage)
        } else {
            Timber.e("Message not found, not added")
        }
    }

    private fun attachPdfNode(nodeHandle: Long) {
        Timber.d("Node Handle: $nodeHandle")
        //Find the pending message
        for (i in pendingMessages!!.indices) {
            val pendMsg = pendingMessages!![i]

            if (pendMsg.getNodeHandle() == nodeHandle) {
                Timber.d("Send node: $nodeHandle to chat: ${pendMsg.getChatId()}")
                requestSent++
                val nodePdf = megaApi.getNodeByHandle(nodeHandle)

                if (nodePdf.hasPreview()) {
                    Timber.d("The pdf node has preview")
                }

                megaChatApi.attachNode(pendMsg.getChatId(), nodeHandle, this)
            } else {
                Timber.e("PDF attach error")
            }
        }
    }

    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestStart: ${request.name}")

        if (request.type == MegaRequest.TYPE_COPY) {
            updateProgressNotification()
        } else if (request.type == MegaRequest.TYPE_SET_ATTR_FILE) {
            Timber.d("TYPE_SET_ATTR_FILE")
        }
    }

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        Timber.d("UPLOAD: onRequestFinish ${request.requestString}")

        //Send the file without preview if the set attribute fails
        if (request.type == MegaRequest.TYPE_SET_ATTR_FILE && request.paramType == MegaApiJava.ATTR_TYPE_PREVIEW) {
            requestSent--
            val handle = request.nodeHandle
            val node = megaApi.getNodeByHandle(handle)

            if (node != null) {
                val nodeName = node.name

                if (MimeTypeList.typeForName(nodeName).isPdf) {
                    attachPdfNode(handle)
                }
            }
        }
        if (e.errorCode == MegaError.API_OK) {
            Timber.d("onRequestFinish OK")
        } else {
            Timber.e("onRequestFinish:ERROR: ${e.errorCode}")
            if (e.errorCode == MegaError.API_EOVERQUOTA) {
                Timber.w("OVERQUOTA ERROR: ${e.errorCode}")
                isOverQuota = 1
            } else if (e.errorCode == MegaError.API_EGOINGOVERQUOTA) {
                Timber.w("PRE-OVERQUOTA ERROR: ${e.errorCode}")
                isOverQuota = 2
            }
            onQueueComplete()
        }
    }

    override fun onRequestTemporaryError(
        api: MegaApiJava, request: MegaRequest,
        e: MegaError,
    ) {
        Timber.w("onRequestTemporaryError: ${request.name}")
    }

    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        Timber.d("onRequestUpdate: ${request.name}")
    }

    override fun onRequestStart(api: MegaChatApiJava, request: MegaChatRequest) {}

    override fun onRequestUpdate(api: MegaChatApiJava, request: MegaChatRequest) {}

    override fun onRequestFinish(api: MegaChatApiJava, request: MegaChatRequest, e: MegaChatError) {
        if (request.type == MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE) {
            requestSent--

            if (e.errorCode == MegaChatError.ERROR_OK) {
                Timber.d("Attachment sent correctly")
                val nodeList = request.megaNodeList

                //Find the pending message
                for (i in pendingMessages!!.indices) {
                    val pendMsg = pendingMessages!![i]

                    //Check node handles - if match add to DB the karere temp id of the message
                    val nodeHandle = pendMsg.getNodeHandle()
                    val node = nodeList[0]

                    if (node.handle == nodeHandle) {
                        Timber.d("The message MATCH!!")
                        val tempId = request.megaChatMessage.tempId
                        Timber.d("The tempId of the message is: $tempId")
                        dbH.updatePendingMessageOnAttach(
                            pendMsg.getId(),
                            tempId.toString() + "",
                            PendingMessageSingle.STATE_SENT
                        )
                        pendingMessages!!.removeAt(i)
                        break
                    }
                }
            } else {
                Timber.w("Attachment not correctly sent: ${e.errorCode} ${e.errorString}")
                val nodeList = request.megaNodeList

                //Find the pending message
                for (i in pendingMessages!!.indices) {
                    val pendMsg = pendingMessages!![i]
                    //Check node handles - if match add to DB the karere temp id of the message
                    val nodeHandle = pendMsg.getNodeHandle()
                    val node = nodeList[0]

                    if (node.handle == nodeHandle) {
                        MegaApplication.getChatManagement().removeMsgToDelete(pendMsg.getId())
                        Timber.d("The message MATCH!!")
                        dbH.updatePendingMessageOnAttach(
                            pendMsg.getId(),
                            (-1).toString(),
                            PendingMessageSingle.STATE_ERROR_ATTACHING
                        )
                        launchErrorToChat(pendMsg.getId())
                        break
                    }
                }
            }
        }

        if (totalUploadsCompleted == totalUploads && transfersCount == 0 && numberVideosPending <= 0 && requestSent <= 0) {
            onQueueComplete()
        }
    }

    fun launchErrorToChat(id: Long) {
        Timber.d("ID: $id")

        //Find the pending message
        for (i in pendingMessages!!.indices) {
            val pendMsg = pendingMessages!![i]

            if (pendMsg.getId() == id) {
                val openChatId = MegaApplication.openChatId

                if (pendMsg.getChatId() == openChatId) {
                    Timber.w("Error update activity")
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.action = Constants.ACTION_UPDATE_ATTACHMENT
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra("ID_MSG", pendMsg.getId())
                    intent.putExtra("IS_OVERQUOTA", isOverQuota)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onRequestTemporaryError(
        api: MegaChatApiJava,
        request: MegaChatRequest,
        e: MegaChatError,
    ) {
    }

    private fun showStorageOverquotaNotification() {
        Timber.d("showStorageOverquotaNotification")
        val contentText = getString(R.string.download_show_info)
        val message = getString(R.string.overquota_alert_title)
        val intent = Intent(this, ManagerActivity::class.java)
        intent.action =
            if (isOverQuota == Constants.OVERQUOTA_STORAGE_STATE) Constants.ACTION_OVERQUOTA_STORAGE else Constants.ACTION_PRE_OVERQUOTA_STORAGE
        val notification: Notification

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID,
                Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setShowBadge(true)
            channel.setSound(null, null)
            mNotificationManager!!.createNotificationChannel(channel)
            mBuilderCompat?.apply {
                setSmallIcon(R.drawable.ic_stat_notify)
                color = ContextCompat.getColor(this@ChatUploadService, R.color.red_600_red_300)
                setContentIntent(
                    PendingIntent.getActivity(
                        applicationContext,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                setAutoCancel(true).setTicker(contentText)
                setContentTitle(message).setContentText(contentText)
                setOngoing(false)
            }
            notification = mBuilderCompat!!.build()
        } else {
            mBuilder?.apply {
                setColor(ContextCompat.getColor(this@ChatUploadService, R.color.red_600_red_300))
                setSmallIcon(R.drawable.ic_stat_notify)
                setContentIntent(
                    PendingIntent.getActivity(
                        applicationContext,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                setAutoCancel(true).setTicker(contentText)
                setContentTitle(message).setContentText(contentText)
                setOngoing(false)
            }
            mBuilderCompat?.color = ContextCompat.getColor(this, R.color.red_600_red_300)
            notification = mBuilder!!.build()
        }

        mNotificationManager!!.notify(Constants.NOTIFICATION_STORAGE_OVERQUOTA, notification)
    }

    /**
     * Checks if there are chat uploads in progress, regardless of the voice clips.
     * @return True if there are chat uploads in progress, false otherwise.
     */
    private fun thereAreChatUploads(): Boolean {
        @Suppress("DEPRECATION")
        if (megaApi.numPendingUploads > 0) {
            val transferData = megaApi.getTransferData(null) ?: return false

            for (i in 0 until transferData.numUploads) {
                val transfer = megaApi.getTransferByTag(transferData.getUploadTag(i)) ?: continue
                val data = transfer.appData

                if (data?.contains(Constants.APP_DATA_CHAT) == true && !transfer.isVoiceClipTransfer()) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Gets the number of videos already compressed.
     *
     * @return Number of videos already compressed.
     */
    private val videosCompressed: Int
        get() {
            var videosCompressed = 0

            for (percentage in mapVideoDownsampling!!.values) {
                if (percentage == 100) {
                    videosCompressed++
                }
            }

            return videosCompressed
        }

    /**
     * Checks if a video pending message path is valid and is compressing:
     * - If the path is not valid or the video is already compressing, does nothing.
     * - If not, launches a broadcast to retry the upload.
     *
     * @param intent Intent containing the pending message with all the needed info.
     */
    private fun checkCompressingMessage(intent: Intent) {
        var fileName = intent.getStringExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME)

        if (TextUtil.isTextEmpty(fileName)) {
            Timber.w("fileName is not valid, no check is needed.")
            return
        }

        try {
            fileName = fileName!!.substring(0, fileName.lastIndexOf("."))
        } catch (e: Exception) {
            Timber.w(e, "Exception getting file name without extension.")
        }

        for (downSamplingPath in mapVideoDownsampling!!.keys) {
            if (downSamplingPath.contains(fileName!!)) {
                //Video message already compressing
                return
            }
        }

        //Video message not compressing, need to retry upload
        val chatId = intent.getLongExtra(Constants.CHAT_ID, MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
        val pendingMsgId = intent.getLongExtra(
            Constants.INTENT_EXTRA_PENDING_MESSAGE_ID,
            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
        )
        sendBroadcast(
            Intent(BroadcastConstants.BROADCAST_ACTION_RETRY_PENDING_MESSAGE)
                .putExtra(Constants.INTENT_EXTRA_PENDING_MESSAGE_ID, pendingMsgId)
                .putExtra(Constants.CHAT_ID, chatId)
        )
    }

    companion object {
        const val DOWNSCALE_IMAGES_PX = 2000000f
        const val ACTION_CANCEL = "CANCEL_UPLOAD"
        const val EXTRA_SIZE = "MEGA_SIZE"
        const val EXTRA_CHAT_ID = "CHAT_ID"
        const val EXTRA_ID_PEND_MSG = "ID_PEND_MSG"
        const val EXTRA_NAME_EDITED = "MEGA_FILE_NAME_EDITED"
        const val EXTRA_COMES_FROM_FILE_EXPLORER = "COMES_FROM_FILE_EXPLORER"
        const val EXTRA_ATTACH_FILES = "ATTACH_FILES"
        const val EXTRA_ATTACH_CHAT_IDS = "ATTACH_CHAT_IDS"
        const val EXTRA_UPLOAD_FILES_FINGERPRINTS = "UPLOAD_FILES_FINGERPRINTS"
        const val EXTRA_PEND_MSG_IDS = "PEND_MSG_IDS"
        const val EXTRA_PARENT_NODE = "EXTRA_PARENT_NODE"
    }
}