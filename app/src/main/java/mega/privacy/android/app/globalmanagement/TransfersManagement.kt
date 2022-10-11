package mega.privacy.android.app.globalmanagement

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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getColor
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.AndroidCompletedTransfer
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.DownloadService
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.UploadService
import mega.privacy.android.app.components.transferWidget.TransfersWidget.Companion.NO_TYPE
import mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_TRANSFER_FINISH
import mega.privacy.android.app.constants.BroadcastConstants.COMPLETED_TRANSFER
import mega.privacy.android.app.constants.EventConstants.EVENT_FAILED_TRANSFERS
import mega.privacy.android.app.constants.EventConstants.EVENT_FINISH_SERVICE_IF_NO_TRANSFERS
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_SCANNING_TRANSFERS_DIALOG
import mega.privacy.android.app.constants.EventConstants.EVENT_TRANSFER_UPDATE
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.main.megachat.ChatUploadService
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.SDCardUtils
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransfer.STAGE_TRANSFERRING_FILES
import nz.mega.sdk.MegaTransfer.STATE_COMPLETED
import nz.mega.sdk.MegaTransfer.STATE_PAUSED
import timber.log.Timber
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
    @MegaApi private val megaApi: MegaApiAndroid,
    private val activityLifecycleHandler: ActivityLifecycleHandler,
    private val dbH: DatabaseHandler,
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
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
         * Adds the completed transfer to the DB and
         * sends a broadcast to update the completed transfers tab.
         *
         * @param completedTransfer AndroidCompletedTransfer to add.
         * @param dbH               DatabaseHandle to add the transfer.
         */
        @JvmStatic
        fun addCompletedTransfer(
            completedTransfer: AndroidCompletedTransfer,
            dbH: DatabaseHandler,
        ) {
            Completable.create { emitter ->
                completedTransfer.id = dbH.setCompletedTransfer(completedTransfer)
                emitter.onComplete()
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = {
                        MegaApplication.getInstance().sendBroadcast(
                            Intent(BROADCAST_ACTION_TRANSFER_FINISH)
                                .putExtra(COMPLETED_TRANSFER, completedTransfer)
                        )
                    },
                    onError = Timber::e
                )
                .addTo(CompositeDisposable())
        }

        /**
         * Creates the initial notification when a service starts.
         *
         * @param notificationChannelId   Identifier of the notification channel.
         * @param notificationChannelName Name of the notification channel.
         * @param mNotificationManager    NotificationManager to create the notification.
         * @param mBuilder                Builder to create the notification.
         * @return The initial notification created.
         */
        @JvmStatic
        fun createInitialServiceNotification(
            notificationChannelId: String?,
            notificationChannelName: String?,
            mNotificationManager: NotificationManager,
            mBuilderCompat: NotificationCompat.Builder,
            mBuilder: Notification.Builder,
        ): Notification =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    notificationChannelId,
                    notificationChannelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    setShowBadge(true)
                    setSound(null, null)
                }

                mNotificationManager.createNotificationChannel(channel)

                mBuilderCompat.apply {
                    setSmallIcon(R.drawable.ic_stat_notify)
                    color = getColor(MegaApplication.getInstance(), R.color.red_600_red_300)
                    setContentTitle(getString(R.string.download_preparing_files))
                    setAutoCancel(true)
                }

                mBuilderCompat.build()
            } else {
                mBuilder.apply {
                    setSmallIcon(R.drawable.ic_stat_notify)
                    setColor(getColor(MegaApplication.getInstance(), R.color.red_600_red_300))
                    setContentTitle(getString(R.string.download_preparing_files))
                    setAutoCancel(true)
                }

                mBuilder.build()
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

    private val pausedTransfers = ArrayList<String>()

    private val scanningTransfers = ArrayList<ScanningTransferData>()
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
        pausedTransfers.clear()
        scanningTransfers.clear()
        scanningTransfersToken = null
        isProcessingFolders = false
        isProcessingTransfers = false
        shouldBreakTransfersProcessing = false
    }

    /**
     * Removes a resumed transfer.
     *
     * @param transferTag   tag of the resumed transfer
     */
    fun removePausedTransfers(transferTag: Int) {
        pausedTransfers.remove(transferTag.toString())
    }

    /**
     * Adds a paused transfer.
     *
     * @param transferTag   tag of the paused transfer
     */
    fun addPausedTransfers(transferTag: Int) {
        val tag = transferTag.toString()

        if (!pausedTransfers.contains(tag)) {
            pausedTransfers.add(tag)
        }
    }

    /**
     * Checks if a transfer is paused.
     * If so, adds it to the paused transfers list.
     * If not, do nothing.
     *
     * @param transferTag Identifier of the MegaTransfer to check.
     */
    fun checkIfTransferIsPaused(transferTag: Int) {
        checkIfTransferIsPaused(megaApi.getTransferByTag(transferTag))
    }

    /**
     * Checks if a transfer is paused.
     * If so, adds it to the paused transfers list.
     * If not, do nothing.
     *
     * @param transfer MegaTransfer to check.
     */
    fun checkIfTransferIsPaused(transfer: MegaTransfer?) {
        if (transfer?.state == STATE_PAUSED) {
            addPausedTransfers(transfer.tag)
        }
    }

    /**
     * Clears the paused transfers list.
     */
    fun resetPausedTransfers() {
        pausedTransfers.clear()
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
        monitorStorageStateEvent.getState() == StorageState.Red


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
                LiveEventBus.get(EVENT_TRANSFER_UPDATE, Int::class.java).post(NO_TYPE)
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
            LiveEventBus.get(EVENT_TRANSFER_UPDATE, Int::class.java).post(NO_TYPE)
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
        val app = MegaApplication.getInstance()

        if (megaApi.rootNode != null) {
            SDCardUtils.checkSDCardCompletedTransfers()
        }

        if (dbH.transferQueueStatus) {
            //Queue of transfers should be paused.
            megaApi.pauseTransfers(true)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                @Suppress("DEPRECATION")
                if (megaApi.numPendingDownloads > 0) {
                    val downloadServiceIntent =
                        Intent(app, DownloadService::class.java)
                            .setAction(Constants.ACTION_RESTART_SERVICE)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activityLifecycleHandler.isActivityVisible) {
                        app.startForegroundService(downloadServiceIntent)
                    } else {
                        app.startService(downloadServiceIntent)
                    }
                }

                @Suppress("DEPRECATION")
                if (megaApi.numPendingUploads > 0) {
                    val uploadServiceIntent = Intent(app, UploadService::class.java)
                        .setAction(Constants.ACTION_RESTART_SERVICE)

                    val chatUploadServiceIntent = Intent(app, ChatUploadService::class.java)
                        .setAction(Constants.ACTION_RESTART_SERVICE)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activityLifecycleHandler.isActivityVisible) {
                        app.startForegroundService(uploadServiceIntent)
                        app.startForegroundService(chatUploadServiceIntent)
                    } else {
                        app.startService(uploadServiceIntent)
                        app.startService(chatUploadServiceIntent)
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Exception checking pending transfers")
            }
        }, WAIT_TIME_TO_RESTART_SERVICES)
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
        node: MegaNode,
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
    fun getScanningTransfersToken(): MegaCancelToken? {
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
     * If the transfer is a file, removes it from scanningTransfers because is already processed.
     * It the transfer is a folder, updates its scanningTransferData.
     *
     * @param transfer  Transfer to check.
     */
    fun checkScanningTransferOnStart(transfer: MegaTransfer) {
        for (data in scanningTransfers) {
            if (data.isTheSameTransfer(transfer)) {
                data.apply {
                    val updatedTransfer = megaApi.getTransferByTag(transfer.tag)
                    if (!isFolder || updatedTransfer == null
                        || updatedTransfer.state == STATE_COMPLETED
                    ) {
                        removeProcessedScanningTransfer()
                    } else {
                        transferTag = transfer.tag
                        transferStage = transfer.stage
                    }
                }

                break
            }
        }
    }

    /**
     * If the transfer is a folder removes it from scanningTransfers if already processed,
     * or updates its stage if not.
     * If the folder transfer is already processed means its stage is >= STAGE_TRANSFERRING_FILES.
     *
     * @param transfer  Transfer to check.
     */
    fun checkScanningTransferOnUpdate(transfer: MegaTransfer) {
        for (data in scanningTransfers) {
            if (data.isTheSameTransfer(transfer)) {
                val updatedTransfer = megaApi.getTransferByTag(transfer.tag)
                if (transfer.stage >= STAGE_TRANSFERRING_FILES
                    || updatedTransfer == null || updatedTransfer.state == STATE_COMPLETED
                ) {
                    data.removeProcessedScanningTransfer()
                } else {
                    data.transferStage = transfer.stage
                }

                break
            }
        }
    }

    /**
     * If the transfer is a folder removes it from scanningTransfers as is already processed.
     *
     * @param transfer  Transfer to check.
     */
    fun checkScanningTransferOnFinish(transfer: MegaTransfer) {
        for (data in scanningTransfers) {
            if (data.isTheSameTransfer(transfer)) {
                data.removeProcessedScanningTransfer()
                break
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
     * Removes a scanning transfer after has been processed.
     *
     * @param transfer  Transfer to remove.
     */
    fun scanningTransferProcessed(transfer: MegaTransfer) {
        for (data in scanningTransfers) {
            if (data.isTheSameTransfer(transfer)) {
                scanningTransfers.remove(data)
                break
            }
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
            LiveEventBus.get(EVENT_FINISH_SERVICE_IF_NO_TRANSFERS, Boolean::class.java).post(true)
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
        LiveEventBus.get(EVENT_FAILED_TRANSFERS, Boolean::class.java).post(false)
    }

    fun getAreFailedTransfers(): Boolean = areFailedTransfers
}