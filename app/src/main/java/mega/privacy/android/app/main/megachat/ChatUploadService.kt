package mega.privacy.android.app.main.megachat

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mega.privacy.android.app.LegacyDatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.VideoDownSampling
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.CacheFolderManager.buildChatTempFile
import mega.privacy.android.app.utils.CacheFolderManager.buildVoiceClipFile
import mega.privacy.android.app.utils.CacheFolderManager.deleteCacheFolderIfEmpty
import mega.privacy.android.app.utils.CacheFolderManager.getCacheFolder
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.gateway.preferences.ChatPreferencesGateway
import mega.privacy.android.data.mapper.transfer.CompletedTransferMapper
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferFinishType
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.TransfersFinishedState
import mega.privacy.android.domain.entity.transfer.isVoiceClip
import mega.privacy.android.domain.entity.transfer.pendingMessageId
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.chat.AttachNodeUseCase
import mega.privacy.android.domain.usecase.chat.AttachVoiceMessageUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.transfers.BroadcastTransfersFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferDataUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.IsThereAnyChatUploadUseCase
import mega.privacy.android.domain.usecase.transfers.completed.AddCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.CancelAllUploadTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.ResetTotalUploadsUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.SetNodeAttributesAfterUploadUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Service which should be only used for chat uploads.
 */
@AndroidEntryPoint
class ChatUploadService : LifecycleService() {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var dbH: LegacyDatabaseHandler

    @Inject
    lateinit var transfersManagement: TransfersManagement

    @Inject
    lateinit var chatPreferencesGateway: ChatPreferencesGateway

    @Inject
    lateinit var monitorPausedTransfersUseCase: MonitorPausedTransfersUseCase

    @Inject
    lateinit var broadcastTransfersFinishedUseCase: BroadcastTransfersFinishedUseCase

    @Inject
    lateinit var addCompletedTransferUseCase: AddCompletedTransferUseCase

    @Inject
    lateinit var completedTransferMapper: CompletedTransferMapper

    @Inject
    lateinit var getTransferDataUseCase: GetTransferDataUseCase

    @Inject
    lateinit var getTransferByTagUseCase: GetTransferByTagUseCase

    @Inject
    lateinit var cancelTransferByTagUseCase: CancelTransferByTagUseCase

    @Inject
    lateinit var monitorTransferEventsUseCase: MonitorTransferEventsUseCase

    @Inject
    lateinit var isThereAnyChatUploadUseCase: IsThereAnyChatUploadUseCase

    @Inject
    lateinit var setNodeAttributesAfterUploadUseCase: SetNodeAttributesAfterUploadUseCase

    @Inject
    lateinit var cancelAllUploadTransfersUseCase: CancelAllUploadTransfersUseCase

    @Inject
    lateinit var attachNodeUseCase: AttachNodeUseCase

    @Inject
    lateinit var attachVoiceMessageUseCase: AttachVoiceMessageUseCase

    @Inject
    lateinit var getFingerprintUseCase: GetFingerprintUseCase

    @Inject
    lateinit var resetTotalUploadsUseCase: ResetTotalUploadsUseCase

    @Inject
    lateinit var broadcastBusinessAccountExpiredUseCase: BroadcastBusinessAccountExpiredUseCase

    private var isForeground = false
    private var canceled = false
    private var fileNames: HashMap<String, String>? = HashMap()
    var sendOriginalAttachments = false

    //0 - not over quota, not pre-over quota
    //1 - over quota
    //2 - pre-over quota
    private var isOverQuota = Constants.NOT_OVERQUOTA_STATE
    private var pendingMessages: ArrayList<PendingMessage>? = null
    private var mapVideoDownsampling: HashMap<String, Int>? = null
    private val mapProgressTransfers: HashMap<Int, Transfer> = HashMap()
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
    private var videoDownsampling: VideoDownSampling? = null
    private var mBuilder: Notification.Builder? = null
    private var mBuilderCompat: NotificationCompat.Builder? = null
    private var mNotificationManager: NotificationManager? = null
    private var fileExplorerUpload = false
    private var snackbarChatHandle = MegaChatApiJava.MEGACHAT_INVALID_HANDLE

    private var monitorPausedTransfersJob: Job? = null
    private var monitorTransferEventsJob: Job? = null

    @SuppressLint("WrongConstant")
    override fun onCreate() {
        super.onCreate()
        app = application as MegaApplication
        pendingMessages = ArrayList()
        isForeground = false
        canceled = false
        isOverQuota = Constants.NOT_OVERQUOTA_STATE
        mapVideoDownsampling = HashMap()
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

        monitorPausedTransfersJob = lifecycleScope.launch {
            monitorPausedTransfersUseCase().collectLatest {
                // delay 1 second to refresh the pause notification to prevent update is missed
                delay(TransfersManagement.WAIT_TIME_BEFORE_UPDATE)
                updateProgressNotification(it)
            }
        }

        monitorTransferEventsJob = lifecycleScope.launch {
            monitorTransferEventsUseCase()
                .filter {
                    it.transfer.transferType == TransferType.CHAT_UPLOAD
                }
                .catch { Timber.e(it) }
                .collect { transferEvent ->
                    when (transferEvent) {
                        is TransferEvent.TransferFinishEvent -> onTransferFinish(
                            transferEvent.transfer,
                            transferEvent.error
                        )

                        is TransferEvent.TransferStartEvent -> onTransferStart(
                            transferEvent.transfer
                        )

                        is TransferEvent.TransferTemporaryErrorEvent -> onTransferTemporaryError(
                            transferEvent.transfer,
                            transferEvent.error
                        )

                        is TransferEvent.TransferUpdateEvent -> onTransferUpdate(
                            transferEvent.transfer
                        )

                        else -> {}
                    }
                }
        }
    }

    private fun startForeground() {
        isForeground = try {
            startForeground(
                Constants.NOTIFICATION_CHAT_UPLOAD,
                createInitialNotification()
            )
            true
        } catch (e: Exception) {
            Timber.w(e, "Error starting foreground.")
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun createInitialNotification() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TransfersManagement.createInitialServiceNotification(
                notificationChannelId = Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID,
                notificationChannelName = Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_NAME,
                mNotificationManager = mNotificationManager!!,
                mBuilderCompat = NotificationCompat.Builder(
                    this@ChatUploadService,
                    Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID
                )
            )

        } else {
            TransfersManagement.createInitialServiceNotification(
                Notification.Builder(this@ChatUploadService)
            )
        }

    private fun stopForeground() {
        isForeground = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        mNotificationManager?.cancel(Constants.NOTIFICATION_CHAT_UPLOAD)
        stopSelf()
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        releaseLocks()
        monitorPausedTransfersJob?.cancel()
        monitorTransferEventsJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.d("Flags: $flags, Start ID: $startId")
        canceled = false

        if (intent == null) {
            return START_NOT_STICKY
        }

        if (intent.action != null) {
            if (intent.action == ACTION_CANCEL) {
                Timber.d("Cancel intent")
                canceled = true
                lifecycleScope.launch { cancelAllUploadTransfersUseCase() }
                return START_NOT_STICKY
            }
        }

        isOverQuota = Constants.NOT_OVERQUOTA_STATE
        lifecycleScope.launch { onHandleIntent(intent) }
        return START_NOT_STICKY
    }

    private suspend fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.action != null && intent.action == Constants.ACTION_RESTART_SERVICE) {
            getTransferDataUseCase()?.let { transferData ->
                var voiceClipsInProgress = 0

                for (i in 0 until transferData.numUploads) {
                    getTransferByTagUseCase(transferData.uploadTags[i])?.takeIf { transfer ->
                        transfer.transferType == TransferType.CHAT_UPLOAD
                    }?.let { transfer ->
                        mapProgressTransfers[transfer.tag] = transfer

                        if (transfer.isVoiceClip()) voiceClipsInProgress++
                    }
                }

                totalUploads = mapProgressTransfers.size - voiceClipsInProgress
                transfersCount = totalUploads

                if (totalUploads > 0) {
                    updateProgressNotification()
                } else {
                    stopForeground()
                }
            } ?: startForeground()
            return
        } else if (Constants.ACTION_CHECK_COMPRESSING_MESSAGE == intent.action) {
            checkCompressingMessage(intent)
            return
        }

        val pendingMessages = ArrayList<PendingMessage>()
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
                        runCatching { attachNodeUseCase(idChat, attachFile) }
                            .onSuccess { handleSuccessAttachment(attachFile, it) }
                            .onFailure { handleFailureAttachment(it, attachFile) }
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

            if (idPendMsgs != null && idPendMsgs.isNotEmpty() && !fileFingerprints.isNullOrEmpty()) {
                for (entry in fileFingerprints.entries) {
                    val fingerprint = entry.key
                    val path = entry.value
                    totalUploads++
                    acquireLock()
                    pendingMessages.clear()

                    for (i in idPendMsgs.indices) {
                        var pendingMsg: PendingMessage?

                        if (idPendMsgs[i] != -1L) {
                            pendingMsg = dbH.findPendingMessageById(idPendMsgs[i])
                            //									One transfer for file --> onTransferFinish() attach to all selected chats
                            if (pendingMsg != null && pendingMsg.chatId != -1L
                                && path == pendingMsg.filePath
                                && fingerprint == pendingMsg.fingerprint
                            ) {
                                if (!pendingMessages.contains(pendingMsg)) {
                                    pendingMessages.add(pendingMsg)
                                }

                                if (onlyOneChat) {
                                    if (snackbarChatHandle == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                                        snackbarChatHandle = pendingMsg.chatId
                                    } else if (snackbarChatHandle != pendingMsg.chatId) {
                                        onlyOneChat = false
                                    }
                                }
                            }
                        }
                    }

                    initUpload(pendingMessages, null)
                }
            }
        } else {
            val chatId = intent.getLongExtra(EXTRA_CHAT_ID, -1)
            type = intent.getStringExtra(Constants.EXTRA_TRANSFER_TYPE)
            val idPendMsg = intent.getLongExtra(EXTRA_ID_PEND_MSG, -1)
            var pendingMsg: PendingMessage? = null

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
                    acquireLock()
                    pendingMessages.clear()
                    pendingMessages.add(pendingMsg)
                    initUpload(pendingMessages, type)
                }
            } else {
                Timber.e("Error the chatId is not correct: $chatId")
            }
        }
    }

    private suspend fun initUpload(pendingMsgs: ArrayList<PendingMessage>, type: String?) {
        Timber.d("initUpload")
        val pendingMsg = pendingMsgs[0]
        val file = File(pendingMsg.filePath)

        val imageQuality = chatPreferencesGateway.getChatImageQualityPreference().first()
        Timber.d("Image quality is $imageQuality")
        val shouldCompressImage = MimeTypeList.typeForName(file.name).isImage
                && !MimeTypeList.typeForName(file.name).isGIF
                && (imageQuality == ChatImageQuality.Optimised
                || (imageQuality == ChatImageQuality.Automatic
                && Util.isOnMobileData(this@ChatUploadService)))

        if (shouldCompressImage) {
            val compressedFile = ChatUtil.checkImageBeforeUpload(file)
            val uploadPath = if (FileUtil.isFileAvailable(compressedFile)) {
                val fingerprint = getFingerprintUseCase(compressedFile.absolutePath)
                for (pendMsg in pendingMsgs) {
                    if (fingerprint != null) {
                        pendMsg.fingerprint = fingerprint
                    }
                    pendingMessages!!.add(pendMsg)
                }
                compressedFile.absolutePath
            } else {
                pendingMessages!!.addAll(pendingMsgs)
                pendingMsg.filePath
            }

            startUpload(pendingMsg.id, type, fileNames!![pendingMsg.name], uploadPath)
        } else if (MimeTypeList.typeForName(file.name).isMp4Video && !sendOriginalAttachments) {
            Timber.d("DATA connection is Mp4Video")
            try {
                val chatTempFolder = getCacheFolder(
                    CacheFolderManager.CHAT_TEMPORARY_FOLDER
                )
                var outFile = buildChatTempFile(file.name)
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
                        pendingMsg.id,
                        type,
                        fileNames!![pendingMsg.name],
                        pendingMsg.filePath,
                        pendingMsgs
                    )
                } else {
                    totalVideos++
                    numberVideosPending++
                    for (pendMsg in pendingMsgs) {
                        pendMsg.videoDownSampled = outFile.absolutePath
                        pendingMessages!!.add(pendMsg)
                    }
                    mapVideoDownsampling!![outFile.absolutePath] = 0
                    if (videoDownsampling == null) {
                        videoDownsampling = VideoDownSampling(this@ChatUploadService)
                    }
                    videoDownsampling!!.changeResolution(
                        file, outFile.absolutePath,
                        pendingMsg.id, dbH.chatVideoQuality
                    )
                }
            } catch (throwable: Throwable) {
                Timber.e("EXCEPTION: Video cannot be downsampled", throwable)
                addPendingMessagesAndStartUpload(
                    pendingMsg.id,
                    type,
                    fileNames!![pendingMsg.name],
                    pendingMsg.filePath,
                    pendingMsgs
                )
            }
        } else {
            addPendingMessagesAndStartUpload(
                pendingMsg.id, type,
                fileNames!![pendingMsg.name], pendingMsg.filePath, pendingMsgs
            )
        }

        if (dbH.transferQueueStatus
            && !transfersManagement.hasResumeTransfersWarningAlreadyBeenShown
        ) {
            sendBroadcast(
                Intent(BroadcastConstants.BROADCAST_ACTION_RESUME_TRANSFERS).setPackage(
                    applicationContext.packageName
                )
            )
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
        localPath: String, pendingMsgs: ArrayList<PendingMessage>,
    ) {
        for (msg in pendingMsgs) {
            if (!(pendingMessages ?: return).contains(msg)) {
                pendingMessages?.add(msg)
            }
        }
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
    private suspend fun onQueueComplete() {
        Timber.d("onQueueComplete")
        //Review when is called
        releaseLocks()

        if (isOverQuota != Constants.NOT_OVERQUOTA_STATE) {
            showStorageOverquotaNotification()
        }

        Timber.d("Reset figures of chatUploadService")
        numberVideosPending = 0
        totalVideos = 0
        totalUploads = 0
        totalUploadsCompleted = 0

        resetTotalUploadsUseCase()

        if (fileExplorerUpload) {
            fileExplorerUpload = false
            broadcastTransfersFinishedUseCase(
                TransfersFinishedState(
                    type = TransferFinishType.FILE_EXPLORER_CHAT_UPLOAD,
                    chatId = snackbarChatHandle
                )
            )

            snackbarChatHandle = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
        }

        Timber.d("Stopping service!!")
        transfersManagement.hasResumeTransfersWarningAlreadyBeenShown = false
        stopForeground()
        Timber.d("After stopSelf")

        try {
            deleteCacheFolderIfEmpty(CacheFolderManager.TEMPORARY_FOLDER)
        } catch (e: Exception) {
            Timber.e("EXCEPTION: pathSelfie not deleted", e)
        }
    }

    fun updateProgressDownsampling(percentage: Int, key: String) {
        mapVideoDownsampling!![key] = percentage
        lifecycleScope.launch { updateProgressNotification() }
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

                if (pendMsg.videoDownSampled != null && pendMsg.videoDownSampled == returnedFile) {
                    lifecycleScope.launch {
                        getFingerprintUseCase(returnedFile)?.let { pendMsg.fingerprint = it }
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

                if (pendMsg.videoDownSampled != null) {
                    if (pendMsg.videoDownSampled == returnedFile) {
                        pendMsg.videoDownSampled = null
                        downFile = File(pendMsg.filePath)
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
    private suspend fun updateProgressNotification(pausedTransfers: Boolean = false) {
        var progressPercent: Long = 0
        val transfers: Collection<Transfer> = mapProgressTransfers.values

        if (sendOriginalAttachments) {
            var total: Long = 0
            var inProgress: Long = 0

            for (currentTransfer in transfers) {
                if (!currentTransfer.isVoiceClip()) {
                    if (currentTransfer.state == TransferState.STATE_COMPLETED) {
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
                if (!currentTransfer.isVoiceClip()) {
                    val individualInProgress = currentTransfer.transferredBytes
                    val individualTotalBytes = currentTransfer.totalBytes
                    var individualProgressPercent: Long = 0

                    if (currentTransfer.state == TransferState.STATE_COMPLETED) {
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
                if (!currentTransfer.isVoiceClip()) {
                    total += currentTransfer.totalBytes
                    inProgress += currentTransfer.transferredBytes
                }
            }

            inProgress *= 100
            progressPercent = if (total <= 0) 0 else inProgress / total
        }

        Timber.d("Progress: $progressPercent")
        val message: String = if (isOverQuota != Constants.NOT_OVERQUOTA_STATE) {
            getString(R.string.overquota_alert_title)
        } else {
            val inProgress =
                if (totalUploadsCompleted == totalUploads) totalUploadsCompleted
                else totalUploadsCompleted + 1

            val videosCompressed = videosCompressed

            if (pausedTransfers) {
                getString(
                    R.string.upload_service_notification_paused,
                    inProgress, totalUploads
                )
            } else if (isThereAnyChatUploadUseCase() || videosCompressed == mapVideoDownsampling!!.size) {
                getString(
                    R.string.upload_service_notification,
                    inProgress,
                    totalUploads
                )
            } else {
                getString(
                    R.string.title_compress_video,
                    videosCompressed + 1, mapVideoDownsampling!!.size
                )
            }
        }

        val intent = Intent(this@ChatUploadService, ManagerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

        when (isOverQuota) {
            Constants.NOT_OVERQUOTA_STATE -> {
                intent.action = Constants.ACTION_SHOW_TRANSFERS
                intent.putExtra(Constants.OPENED_FROM_CHAT, true)
            }

            Constants.OVERQUOTA_STORAGE_STATE -> {
                intent.action = Constants.ACTION_OVERQUOTA_STORAGE
            }

            Constants.PRE_OVERQUOTA_STORAGE_STATE -> {
                intent.action = Constants.ACTION_PRE_OVERQUOTA_STORAGE
            }

            else -> {
                intent.action = Constants.ACTION_SHOW_TRANSFERS
                intent.putExtra(Constants.OPENED_FROM_CHAT, true)
            }
        }

        val actionString =
            if (isOverQuota == Constants.NOT_OVERQUOTA_STATE) getString(R.string.chat_upload_title_notification)
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

    private suspend fun onTransferStart(transfer: Transfer) = with(transfer) {
        Timber.d("onTransferStart: $nodeHandle appData: $appData")

        if (!isVoiceClip()) {
            transfersCount++
        }

        if (isStreamingTransfer) {
            return
        }

        transfer.pendingMessageId()?.let { id ->
            sendBroadcast(
                Intent(BroadcastConstants.BROADCAST_ACTION_CHAT_TRANSFER_START)
                    .putExtra(BroadcastConstants.PENDING_MESSAGE_ID, id)
                    .setPackage(applicationContext.packageName)
            )

            //Update status and tag on db
            dbH.updatePendingMessageOnTransferStart(id, tag)
        }

        mapProgressTransfers[tag] = transfer

        if (!isFolderTransfer && !isVoiceClip()) {
            updateProgressNotification()
        }
    }

    private suspend fun onTransferUpdate(transfer: Transfer) = with(transfer) {
        Timber.d("onTransferUpdate: $nodeHandle")

        if (canceled) {
            Timber.w("Transfer cancel: $nodeHandle")
            releaseLocks()
            runCatching { cancelTransferByTagUseCase(tag) }
                .onFailure { Timber.w("Exception canceling transfer: $it") }
            cancel()
            Timber.d("After cancel")
            return
        }

        if (isOverQuota != Constants.NOT_OVERQUOTA_STATE) {
            Timber.w("After overquota error")
            isOverQuota = Constants.NOT_OVERQUOTA_STATE
        }

        mapProgressTransfers[tag] = transfer

        if (!isVoiceClip()) {
            updateProgressNotification()
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

    private suspend fun onTransferTemporaryError(transfer: Transfer, e: MegaException?) {
        Timber.w("Handle: ${transfer.nodeHandle}. Upload Temporary Error: ${e?.errorString}__${e?.errorCode}".trimIndent())

        when (e?.errorCode) {
            MegaError.API_EOVERQUOTA, MegaError.API_EGOINGOVERQUOTA -> {
                isOverQuota = if (e.errorCode == MegaError.API_EOVERQUOTA) {
                    Constants.OVERQUOTA_STORAGE_STATE
                } else {
                    Constants.PRE_OVERQUOTA_STORAGE_STATE
                }

                if (e.value != 0L) {
                    Timber.w("TRANSFER OVERQUOTA ERROR: ${e.errorCode}")
                } else {
                    Timber.w("STORAGE OVERQUOTA ERROR: ${e.errorCode}")

                    if (!transfer.isVoiceClip()) {
                        updateProgressNotification()
                    }
                }
            }
        }
    }

    private suspend fun onTransferFinish(
        transfer: Transfer,
        error: MegaException?,
    ) = with(transfer) {
        if (error?.errorCode == MegaError.API_EBUSINESSPASTDUE) {
            broadcastBusinessAccountExpiredUseCase()
        }

        Timber.d("onTransferFinish: $nodeHandle")

        if (isStreamingTransfer) {
            return
        }

        if (!isVoiceClip()) {
            transfersCount--
            totalUploadsCompleted++
        }
        addCompletedTransferUseCase(completedTransferMapper(transfer, error))
        mapProgressTransfers[tag] = transfer

        if (canceled) {
            Timber.w("Upload cancelled: $nodeHandle")
            releaseLocks()
            cancel()
            Timber.d("After cancel")

            if (isVoiceClip()) {
                val localFile = buildVoiceClipFile(fileName)

                if (FileUtil.isFileAvailable(localFile) && localFile!!.name != fileName) {
                    localFile.delete()
                }
            } else {
                //Delete recursively all files and folder-??????
                deleteCacheFolderIfEmpty(
                    CacheFolderManager.TEMPORARY_FOLDER
                )
            }
        } else {
            if (error == null) {
                Timber.d("Upload OK: $nodeHandle")

                runCatching { setNodeAttributesAfterUploadUseCase(nodeHandle, File(localPath)) }
                    .onFailure { error -> Timber.e("Set Node Attributes error: $error") }

                if (isVoiceClip()) {
                    Timber.d("Is voice clip")
                    attachVoiceClips(transfer)
                } else {
                    Timber.d("NOT video, image or pdf!")
                    attachNodes(transfer)
                }
            } else {
                Timber.e("Upload Error: ${nodeHandle}_${error.errorCode}___${error.errorString}")
                if (error.errorCode == MegaError.API_EEXIST) {
                    Timber.w("Transfer API_EEXIST: $nodeHandle")
                } else {
                    if (error.errorCode == MegaError.API_EOVERQUOTA) {
                        isOverQuota = Constants.OVERQUOTA_STORAGE_STATE
                    } else if (error.errorCode == MegaError.API_EGOINGOVERQUOTA) {
                        isOverQuota = Constants.PRE_OVERQUOTA_STORAGE_STATE
                    }

                    transfer.pendingMessageId()?.let { id ->
                        //Update status and tag on db
                        dbH.updatePendingMessageOnTransferFinish(
                            id,
                            "-1",
                            PendingMessageState.ERROR_UPLOADING.value
                        )

                        launchErrorToChat(id)
                    }

                    if (totalUploadsCompleted == totalUploads && transfersCount == 0 && numberVideosPending <= 0 && requestSent <= 0) {
                        onQueueComplete()
                        return
                    }
                }
            }

            val tempPic =
                getCacheFolder(CacheFolderManager.TEMPORARY_FOLDER)

            Timber.d("IN Finish: $nodeHandle")

            if (FileUtil.isFileAvailable(tempPic) && localPath.isNotEmpty()) {
                if (localPath.startsWith(tempPic!!.absolutePath)) {
                    val f = File(localPath)
                    f.delete()
                }
            } else {
                Timber.e("transfer.getPath() is NULL or temporal folder unavailable")
            }
        }

        if (totalUploadsCompleted == totalUploads && transfersCount == 0 && numberVideosPending <= 0 && requestSent <= 0) {
            onQueueComplete()
        } else if (!isVoiceClip()) {
            updateProgressNotification()
        }
    }

    /**
     * Checks if pendingMessages list is empty.
     * If not, do nothing.
     * If so, means the service has been restarted after a transfers resumption or some error happened
     * and tries to get the PendingMessage related to the current MegaTransfer from DB.
     * If the PendingMessage exists, attaches it to the chat conversation.
     *
     * @param id       Identifier of PendingMessage.
     * @param transfer Current MegaTransfer.
     * @return True if the list is empty, false otherwise.
     */
    private suspend fun arePendingMessagesEmpty(id: Long, transfer: Transfer): Boolean {
        if (pendingMessages != null && pendingMessages!!.isNotEmpty()) {
            return false
        }
        attachMessageFromDB(id, transfer)
        return true
    }

    /**
     * Attaches a message to a chat conversation getting it from DB.
     *
     * @param id       Identifier of PendingMessage.
     * @param transfer Current MegaTransfer.
     */
    private suspend fun attachMessageFromDB(id: Long, transfer: Transfer) {
        val pendingMessage = dbH.findPendingMessageById(id)
        if (pendingMessage != null) {
            pendingMessages!!.add(pendingMessage)
            attach(pendingMessage, transfer)
        } else {
            Timber.e("Message not found and not attached.")
        }
    }

    private suspend fun attachNodes(transfer: Transfer) = with(transfer) {
        Timber.d("attachNodes()")

        transfer.pendingMessageId()?.let { id ->
            //Update status and nodeHandle on db
            dbH.updatePendingMessageOnTransferFinish(
                id,
                nodeHandle.toString() + "",
                PendingMessageState.ATTACHING.value
            )

            if (arePendingMessagesEmpty(id, transfer)) {
                return@with
            }

            val fingerprint = getFingerprintUseCase(localPath)
            var msgNotFound = true

            //Use an internal temp list in order to avoid `ConcurrentModificationException`
            pendingMessages?.toList()?.let { pendingMessagesTempList ->
                for (pendMsg in pendingMessagesTempList) {
                    if (pendMsg.id == id || pendMsg.fingerprint == fingerprint) {
                        attach(pendMsg, transfer)
                        msgNotFound = false
                    }
                }
            }

            if (msgNotFound) {
                //Message not found, try to attach from DB
                attachMessageFromDB(id, transfer)
            }
        }
    }

    private suspend fun attach(pendMsg: PendingMessage, transfer: Transfer) = with(transfer) {
        Timber.d("attach")
        requestSent++
        pendMsg.nodeHandle = nodeHandle
        pendMsg.state = PendingMessageState.ATTACHING.value
        runCatching { attachNodeUseCase(pendMsg.chatId, nodeHandle) }
            .onSuccess { handleSuccessAttachment(nodeHandle, it) }
            .onFailure { handleFailureAttachment(it, nodeHandle) }

        if (FileUtil.isVideoFile(localPath)) {
            val pathDownsampled = pendMsg.videoDownSampled
            if (localPath == pathDownsampled) {
                //Delete the local temp video file
                val f = File(localPath)
                if (f.exists()) {
                    val deleted = f.delete()
                    if (!deleted) {
                        Timber.e("ERROR: Local file not deleted!")
                    }
                }
            }
        }
    }

    private suspend fun attachVoiceClips(transfer: Transfer) = with(transfer) {
        Timber.d("attachVoiceClips()")
        transfer.pendingMessageId()?.let { id ->
            //Update status and nodeHandle on db
            dbH.updatePendingMessageOnTransferFinish(
                id,
                nodeHandle.toString() + "",
                PendingMessageState.ATTACHING.value
            )

            if (arePendingMessagesEmpty(id, transfer)) {
                return@with
            }

            for (pendMsg in pendingMessages!!) {
                if (pendMsg.id == id) {
                    pendMsg.nodeHandle = nodeHandle
                    pendMsg.state = PendingMessageState.ATTACHING.value
                    runCatching { attachVoiceMessageUseCase(pendMsg.chatId, nodeHandle) }
                        .onSuccess { handleSuccessAttachment(nodeHandle, it) }
                        .onFailure { handleFailureAttachment(it, nodeHandle) }
                    return@with
                }
            }

            //Message not found, try to attach from DB
            attachMessageFromDB(id, transfer)
        }
    }

    private suspend fun handleSuccessAttachment(nodeHandle: Long, tempId: Long) {
        Timber.d("The tempId of the message is: $tempId")

        requestSent--

        pendingMessages?.first { pendMsg -> pendMsg.nodeHandle == nodeHandle }?.let {
            dbH.updatePendingMessageOnAttach(
                it.id,
                tempId.toString(),
                PendingMessageState.SENT.value
            )
            pendingMessages?.remove(it)
        }

        checkTotals()
    }

    private suspend fun handleFailureAttachment(exception: Throwable, nodeHandle: Long) {
        Timber.e("Exception attaching voice message", exception)

        requestSent--

        pendingMessages?.first { pendMsg -> pendMsg.nodeHandle == nodeHandle }?.let {
            MegaApplication.getChatManagement().removeMsgToDelete(it.id)
            Timber.d("The message MATCH!!")

            dbH.updatePendingMessageOnAttach(
                it.id,
                (-1).toString(),
                PendingMessageState.ERROR_ATTACHING.value
            )
            launchErrorToChat(it.id)
        }

        checkTotals()
    }

    private suspend fun checkTotals() {
        if (totalUploadsCompleted == totalUploads && transfersCount == 0 && numberVideosPending <= 0 && requestSent <= 0) {
            onQueueComplete()
        }
    }

    fun launchErrorToChat(id: Long) {
        Timber.d("ID: $id")

        //Find the pending message
        for (i in pendingMessages!!.indices) {
            val pendMsg = pendingMessages!![i]

            if (pendMsg.id == id) {
                val openChatId = MegaApplication.openChatId

                if (pendMsg.chatId == openChatId) {
                    Timber.w("Error update activity")
                    val intent = Intent(this, ChatActivity::class.java)
                    intent.action = Constants.ACTION_UPDATE_ATTACHMENT
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.putExtra("ID_MSG", pendMsg.id)
                    intent.putExtra("IS_OVERQUOTA", isOverQuota)
                    startActivity(intent)
                }
            }
        }
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
                .setPackage(applicationContext.packageName)
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
