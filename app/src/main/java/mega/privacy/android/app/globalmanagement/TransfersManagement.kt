package mega.privacy.android.app.globalmanagement

import mega.privacy.android.icon.pack.R as iconPackR
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getColor
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.DownloadService
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.R
import mega.privacy.android.app.UploadService
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_SCANNING_TRANSFERS_DIALOG
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.megachat.ChatUploadService
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.SDCardOperator
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.extensions.toTransferStage
import mega.privacy.android.data.mapper.transfer.CompletedTransferMapper
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.getSDCardDownloadAppData
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.transfers.BroadcastFailedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.BroadcastStopTransfersWorkUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.completed.AddCompletedTransferIfNotExistUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseAllTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.sd.DeleteSdTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.sd.GetAllSdTransfersUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton class for transfers management.
 *
 * @property megaApi    MegaApiAndroid instance to check transfers status.
 * @property dbH        [DatabaseHandler] for getting and updating transfers' related info.
 */
@Singleton
class TransfersManagement @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val dbH: DatabaseHandler,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val broadcastFailedTransferUseCase: BroadcastFailedTransferUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase,
    private val broadcastStopTransfersWorkUseCase: BroadcastStopTransfersWorkUseCase,
    private val addCompletedTransferIfNotExistUseCase: AddCompletedTransferIfNotExistUseCase,
    private val deleteSdTransferByTagUseCase: DeleteSdTransferByTagUseCase,
    private val getAllSdTransfersUseCase: GetAllSdTransfersUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getTransferByTagUseCase: GetTransferByTagUseCase,
    private val completedTransferMapper: CompletedTransferMapper,
    private val pauseAllTransfersUseCase: PauseAllTransfersUseCase,
) {

    companion object {
        private const val WAIT_TIME_TO_SHOW_WARNING = 60000L
        private const val WAIT_TIME_TO_SHOW_NETWORK_WARNING = 30000L
        private const val WAIT_TIME_TO_RESTART_SERVICES = 5000L
        const val WAIT_TIME_BEFORE_UPDATE = 1000L

        /**
         * Checks if a service is already running.
         *
         * @param serviceClass Service it wants to know if its is already running.
         * @return True if the service is already running, false otherwise.
         */
        @JvmStatic
        @Suppress("DEPRECATION") // Deprecated for third party applications.
        fun isServiceRunning(serviceClass: Class<*>): Boolean {
            val manager = MegaApplication.getInstance()
                .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }

            return false
        }

        /**
         * Creates the initial notification when a service starts.
         *
         * @param mBuilder                Builder to create the notification.
         * @return The initial notification created.
         */
        @JvmStatic
        fun createInitialServiceNotification(
            mBuilder: Notification.Builder,
        ): Notification {
            mBuilder.apply {
                setSmallIcon(iconPackR.drawable.ic_stat_notify)
                setColor(getColor(MegaApplication.getInstance(), R.color.red_600_red_300))
                setContentTitle(
                    MegaApplication.getInstance().getString(R.string.download_preparing_files)
                )
                setAutoCancel(true)
            }

            return mBuilder.build()
        }

        /**
         * Creates the initial notification when a service starts.
         *
         * @param notificationChannelId   Identifier of the notification channel.
         * @param notificationChannelName Name of the notification channel.
         * @param mNotificationManager    NotificationManager to create the notification.
         * @return The initial notification created.
         */
        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.O)
        fun createInitialServiceNotification(
            notificationChannelId: String?,
            notificationChannelName: String?,
            mNotificationManager: NotificationManager,
            mBuilderCompat: NotificationCompat.Builder,
        ): Notification {
            val channel = NotificationChannel(
                notificationChannelId,
                notificationChannelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(true)
                setSound(null, null)
            }

            mNotificationManager.createNotificationChannel(channel)

            mBuilderCompat.apply {
                setSmallIcon(iconPackR.drawable.ic_stat_notify)
                color = getColor(MegaApplication.getInstance(), R.color.red_600_red_300)
                setContentTitle(
                    MegaApplication.getInstance().getString(R.string.download_preparing_files)
                )
                setAutoCancel(true)
            }

            return mBuilderCompat.build()
        }
    }

    private var networkTimer: CountDownTimer? = null

    private var transferOverQuotaTimestamp: Long = 0
    private var hasNotToBeShowDueToTransferOverQuota = false
    var isCurrentTransferOverQuota = false
    var isOnTransfersSection = false
    private var areFailedTransfers = false
    var isTransferOverQuotaNotificationShown = false
    var isTransferOverQuotaBannerShown = false
    var hasResumeTransfersWarningAlreadyBeenShown = false
    var shouldShowNetworkWarning = false

    private val scanningTransfers = mutableListOf<ScanningTransferData>()
    private var scanningTransfersToken: MegaCancelToken? = null
    var isProcessingFolders = false
    var isProcessingTransfers = false
    private var shouldBreakTransfersProcessing = false

    init {
        resetTransferOverQuotaTimestamp()
    }

    fun resetDefaults() {
        networkTimer = null
        transferOverQuotaTimestamp = 0
        hasNotToBeShowDueToTransferOverQuota = false
        isCurrentTransferOverQuota = false
        isOnTransfersSection = false
        areFailedTransfers = false
        isTransferOverQuotaNotificationShown = false
        isTransferOverQuotaBannerShown = false
        hasResumeTransfersWarningAlreadyBeenShown = false
        shouldShowNetworkWarning = false
        scanningTransfers.clear()
        scanningTransfersToken = null
        isProcessingFolders = false
        isProcessingTransfers = false
        shouldBreakTransfersProcessing = false
    }

    /**
     * Sets the current time as timestamp to avoid show duplicated transfer over quota warnings.
     */
    fun setTransferOverQuotaTimestamp() {
        transferOverQuotaTimestamp = System.currentTimeMillis()
    }

    /**
     * Sets the transfer over quota time stamp as invalid.
     */
    fun resetTransferOverQuotaTimestamp() {
        transferOverQuotaTimestamp = INVALID_VALUE.toLong()
    }

    /**
     * Checks if a transfer over quota warning has to be shown.
     * It will be shown if transferOverQuotaTimestamp has not been initialized yet
     * or if more than a minute has passed since the last time it was shown.
     *
     * @return  True if the warning has to be shown, false otherwise
     */
    fun shouldShowTransferOverQuotaWarning(): Boolean =
        transferOverQuotaTimestamp == INVALID_VALUE.toLong()
                || transferOverQuotaTimestamp - System.currentTimeMillis() > WAIT_TIME_TO_SHOW_WARNING

    /**
     * Checks if it is on transfer over quota.
     *
     * @return True if it is on transfer over quota, false otherwise.
     */
    fun isOnTransferOverQuota(): Boolean = megaApi.bandwidthOverquotaDelay > 0

    /**
     * Checks if it is on storage over quota.
     *
     * @return True if it is on storage over quota, false otherwise.
     */
    fun isStorageOverQuota(): Boolean =
        monitorStorageStateEventUseCase.getState() == StorageState.Red


    /**
     * Sets if the widget has to be shown depending on if it is on transfer over quota
     * and the Transfers section has been opened from the transfers widget.
     * Also sets if the "transfer over quota" banner has to be shown due to the same reason.
     *
     * @param hasNotToBeShowDueToTransferOverQuota  true if it is on transfer over quota and the Transfers section
     * has been opened from the transfers widget, false otherwise
     */
    fun setHasNotToBeShowDueToTransferOverQuota(hasNotToBeShowDueToTransferOverQuota: Boolean) {
        this.hasNotToBeShowDueToTransferOverQuota = hasNotToBeShowDueToTransferOverQuota
        isTransferOverQuotaBannerShown = hasNotToBeShowDueToTransferOverQuota
    }

    /**
     * Checks if the transfers widget has to be shown.
     * If the widget does not have to be shown means that:
     * the user is in transfer over quota, there is not any upload transfer in progress
     * and they already opened the transfers section by clicking the widget.
     *
     * @return True if the widget does not have to be shown, false otherwise
     */
    @Suppress("DEPRECATION")
    fun hasNotToBeShowDueToTransferOverQuota(): Boolean =
        hasNotToBeShowDueToTransferOverQuota && megaApi.numPendingUploads <= 0

    /**
     * Starts a CountDownTimer after show warnings related to no internet connection.
     * If the timer finishes, launches a Broadcast to update the widget.
     */
    fun startNetworkTimer() {
        networkTimer = object : CountDownTimer(
            WAIT_TIME_TO_SHOW_NETWORK_WARNING,
            WAIT_TIME_TO_SHOW_NETWORK_WARNING
        ) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                if (isOnline(MegaApplication.getInstance())) {
                    return
                }

                shouldShowNetworkWarning = true
            }
        }.start()
    }

    /**
     * Cancels the CountDownTimer to show warnings related to no internet connection.
     */
    fun resetNetworkTimer() {
        networkTimer?.let { timer ->
            timer.cancel()
            shouldShowNetworkWarning = false
        }
    }

    /**
     * Check if there are resumed pending transfers.
     * Before start to check if there are pending transfers, it has to wait a time
     * WAIT_TIME_TO_RESTART_SERVICES. This time is for the transfer resumption to be enabled
     * since there is no possibility to listen any response of the request to know when it finishes.
     *
     */
    fun checkResumedPendingTransfers() {
        if (megaApi.rootNode != null) {
            applicationScope.launch {
                val completedTransfers = checkSDCardCompletedTransfers()
                addCompletedTransferIfNotExistUseCase(completedTransfers)
            }
        }

        if (areTransfersPausedUseCase()) {
            //Queue of transfers should be paused.
            applicationScope.launch {
                runCatching {
                    pauseAllTransfersUseCase(true)
                }.onFailure {
                    Timber.e(it)
                }
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                applicationScope.launch {
                    if (!getFeatureFlagValueUseCase(AppFeatures.DownloadWorker)) {
                        @Suppress("DEPRECATION")
                        if (megaApi.numPendingDownloads > 0) {
                            val downloadServiceIntent =
                                Intent(context, DownloadService::class.java)
                                    .setAction(Constants.ACTION_RESTART_SERVICE)
                            tryToStartForegroundService(downloadServiceIntent)
                        }
                    }
                }

                @Suppress("DEPRECATION")
                if (megaApi.numPendingUploads > 0) {
                    val uploadServiceIntent = Intent(context, UploadService::class.java)
                        .setAction(Constants.ACTION_RESTART_SERVICE)

                    val chatUploadServiceIntent = Intent(context, ChatUploadService::class.java)
                        .setAction(Constants.ACTION_RESTART_SERVICE)
                    tryToStartForegroundService(uploadServiceIntent, chatUploadServiceIntent)
                }
            } catch (e: Exception) {
                Timber.w(e, "Exception checking pending transfers")
            }
        }, WAIT_TIME_TO_RESTART_SERVICES)
    }

    /**
     * Tries to start a foreground service for each [Intent] if the requirements are meet, if not it may start it with [startService]
     */
    private fun tryToStartForegroundService(vararg intents: Intent) {
        val active =
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        //starting with Android 12 only active apps can startForegroundService
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || active) {
            intents.forEach {
                context.startForegroundService(it)
            }
        }
    }


    /**
     * Checks if there are incomplete movements of SD card downloads and tries to complete them.
     */
    private suspend fun checkSDCardCompletedTransfers(): List<CompletedTransfer> {
        val sdTransfers = getAllSdTransfersUseCase()
        if (sdTransfers.isEmpty()) return emptyList()
        val completedTransfers = ArrayList<CompletedTransfer>()
        for (sdtransfer in sdTransfers) {
            val transfer = getTransferByTagUseCase(sdtransfer.tag)
            if (transfer != null && transfer.state < TransferState.STATE_COMPLETED) {
                continue
            }
            val originalDownload = File(sdtransfer.path)
            if (!FileUtil.isFileAvailable(originalDownload)) {
                deleteSdTransferByTagUseCase(sdtransfer.tag)
                continue
            }
            val sdCardDownload = sdtransfer.getSDCardDownloadAppData()
            if (isFinalDownloadFileExist(
                    sdtransfer,
                    originalDownload,
                    sdCardDownload?.targetPath
                )
            ) {
                continue
            }
            Timber.w("Movement incomplete")
            moveSdTransferToTargetPath(
                originalDownload,
                sdCardDownload?.targetPath,
                sdCardDownload?.targetUri,
                sdtransfer
            )
            transfer?.let { completedTransfers.add(completedTransferMapper(it, null)) }
        }
        return completedTransfers
    }

    private suspend fun isFinalDownloadFileExist(
        sdtransfer: SdTransfer,
        originalDownload: File,
        targetPath: String?,
    ): Boolean {
        val finalDownload = File(targetPath + File.separator + originalDownload.name)
        if (finalDownload.exists() && finalDownload.length() == originalDownload.length()) {
            originalDownload.delete()
            deleteSdTransferByTagUseCase(sdtransfer.tag)
            return true
        }
        return false
    }

    private suspend fun moveSdTransferToTargetPath(
        originalDownload: File,
        targetPath: String?,
        targetUri: String?,
        sdTransfer: SdTransfer,
    ) {
        try {
            val sdCardOperator = SDCardOperator(getInstance())
            val isSuccess = sdCardOperator.moveDownloadedFileToDestinationPath(
                originalDownload, targetPath,
                targetUri
            )
            if (isSuccess) {
                deleteSdTransferByTagUseCase(sdTransfer.tag)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error moving file to the sd card path")
        }
    }

    /**
     * Creates a ScanningTransferData object, containing all the required info to manage transfer
     * processing and other data to identify the transfer.
     *
     * @param type      TYPE_UPLOAD if an upload, TYPE_DOWNLOAD if a download.
     * @param localPath Path of the folder to upload if transferType is TYPE_UPLOAD.
     *                  Path where the folder will be download if transferType is TYPE_DOWNLOAD.
     * @param node      Parent MegaNode where the folder will be uploaded if transferType is TYPE_UPLOAD.
     *                  MegaNode to download if transferType is TYPE_DOWNLOAD.
     * @param isFolder  True if the transfer is a folder, false otherwise.
     * @return The cancel token if the transfer was included in scanning transfers
     * and can be processed, null otherwise.
     */
    fun addScanningTransfer(
        type: Int,
        localPath: String,
        node: MegaNode?,
        isFolder: Boolean,
    ): MegaCancelToken? {
        if (shouldBreakTransfersProcessing()) {
            return null
        }

        if (!isScanningTransfers()) {
            LiveEventBus.get(EVENT_SHOW_SCANNING_TRANSFERS_DIALOG, Boolean::class.java).post(true)
        }

        try {
            scanningTransfers.add(ScanningTransferData(type, localPath, node, isFolder))
        } catch (e: ArrayIndexOutOfBoundsException) {
            Timber.w(e)
            return null
        }

        return getScanningTransfersToken()
    }

    /**
     * Gets the current scanningTransfersToken if exists or a new one if not.
     */
    private fun getScanningTransfersToken(): MegaCancelToken? {
        if (scanningTransfersToken == null) {
            scanningTransfersToken = MegaCancelToken.createInstance()
        }

        return scanningTransfersToken
    }

    /**
     * Cancels all the scanning transfers.
     */
    fun cancelScanningTransfers() {
        if (isProcessingTransfers) {
            shouldBreakTransfersProcessing = true
        }
        isProcessingFolders = false
        scanningTransfersToken?.cancel()
        scanningTransfersToken = null
        scanningTransfers.clear()
    }

    /**
     * Checks scanning transfers.
     *
     * When Check is:
     * - ON_START:
     *  If the transfer is a file, removes it from scanningTransfers because is already processed.
     *  It the transfer is a folder, updates its scanningTransferData.
     *
     * - ON_UPDATE:
     *  If the transfer is a folder removes it from scanningTransfers if already processed,
     *  or updates its stage if not.
     *  If the folder transfer is already processed means its stage is >= STAGE_TRANSFERRING_FILES.
     *
     * - ON_FINISH:
     *  If the transfer is a folder removes it from scanningTransfers as is already processed.
     *
     * @param transfer  Transfer to check.
     */
    fun checkScanningTransfer(transfer: Transfer, check: Check) = synchronized(this) {
        val transfers = scanningTransfers.toList()
        when (check) {
            Check.ON_START -> {
                for (data in transfers) {
                    data.takeIf { it.isTheSameTransfer(transfer) }?.apply {
                        if (!isFolder || transfer.state == TransferState.STATE_COMPLETED) {
                            removeProcessedScanningTransfer()
                        } else {
                            transferTag = transfer.tag
                            transferStage = transfer.stage.toTransferStage()
                        }

                        return@synchronized
                    }
                }
            }

            Check.ON_UPDATE -> {
                for (data in transfers) {
                    data.takeIf { it.isTheSameTransfer(transfer) }?.apply {
                        if (transfer.stage == TransferStage.STAGE_TRANSFERRING_FILES
                            || transfer.state == TransferState.STATE_COMPLETED
                        ) {
                            removeProcessedScanningTransfer()
                        } else {
                            transferStage = transfer.stage.toTransferStage()
                        }

                        return@synchronized
                    }
                }
            }

            Check.ON_FINISH -> {
                for (data in transfers) {
                    data.takeIf { it.isTheSameTransfer(transfer) }?.apply {
                        removeProcessedScanningTransfer()
                        return@synchronized
                    }
                }
            }
        }
    }

    /**
     * Removes the a scanningTransferData which has been already processed.
     */
    private fun ScanningTransferData.removeProcessedScanningTransfer() {
        scanningTransfers.remove(this)

        if (scanningTransfers.isEmpty()) {
            scanningTransfersToken = null
            LiveEventBus.get(EVENT_SHOW_SCANNING_TRANSFERS_DIALOG, Boolean::class.java)
                .post(false)
        }
    }

    /**
     * Checks if is scanning transfers.
     *
     * @return True if scanningTransfers is not empty, which means is scanning transfers.
     *         False otherwise.
     */
    private fun isScanningTransfers(): Boolean = scanningTransfers.isNotEmpty()

    /**
     * Updates the flag isProcessingFolders if needed and launches and event to show
     * or hide the scanning transfers dialog if so.
     *
     * @param processing True if is processing folders, false otherwise.
     */
    fun setIsProcessingFolders(processing: Boolean) {
        when {
            !isScanningTransfers() && !isProcessingFolders && processing -> {
                isProcessingFolders = true
                LiveEventBus.get(EVENT_SHOW_SCANNING_TRANSFERS_DIALOG, Boolean::class.java)
                    .post(true)
            }

            !processing -> {
                isProcessingFolders = false
                LiveEventBus.get(EVENT_SHOW_SCANNING_TRANSFERS_DIALOG, Boolean::class.java)
                    .post(false)
            }
        }
    }

    /**
     * Checks if should show the scanning transfers dialog.
     *
     * @return True if should show the dialog, false otherwise.
     */
    fun shouldShowScanningTransfersDialog(): Boolean =
        isProcessingFolders || isScanningTransfers()

    /**
     * Sets shouldBreakTransfersProcessing to false after a second in order
     * to prevent the break processing fails at some point.
     */
    fun updateShouldBreakTransfersProcessing() {
        Handler(Looper.getMainLooper()).postDelayed({
            shouldBreakTransfersProcessing = false
            isProcessingTransfers = false
            applicationScope.launch { broadcastStopTransfersWorkUseCase() }
        }, WAIT_TIME_BEFORE_UPDATE)
    }

    /**
     * Checks if should stop processing transfers.
     * If so, updates the flag to false since is already checked and stopped.
     *
     * @return True if should stop processing transfers, false otherwise.
     */
    fun shouldBreakTransfersProcessing(): Boolean =
        if (shouldBreakTransfersProcessing) {
            updateShouldBreakTransfersProcessing()
            true
        } else {
            false
        }

    fun setAreFailedTransfers(failed: Boolean) {
        areFailedTransfers = failed
        applicationScope.launch {
            broadcastFailedTransferUseCase(failed)
        }
    }

    fun getAreFailedTransfers(): Boolean = areFailedTransfers

    /**
     * Enum class allowing to identify from where the check comes.
     */
    enum class Check {

        /**
         * Check from onTransferStart.
         */
        ON_START,

        /**
         * Check from onTransferUpdate.
         */
        ON_UPDATE,

        /**
         * Check from onTransferFinish.
         */
        ON_FINISH
    }
}
