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
import mega.privacy.android.app.*
import mega.privacy.android.app.components.transferWidget.TransferWidget.NO_TYPE
import mega.privacy.android.app.constants.BroadcastConstants.*
import mega.privacy.android.app.constants.EventConstants.EVENT_SHOW_SCANNING_FOLDER_DIALOG
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.lollipop.megachat.ChatUploadService
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.SDCardUtils
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util.isOnline
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransfer.TYPE_DOWNLOAD
import nz.mega.sdk.MegaTransfer.TYPE_UPLOAD
import java.lang.Exception
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransfersManagement @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val dbH: DatabaseHandler
) {

    companion object {
        private const val WAIT_TIME_TO_SHOW_WARNING = 60000L
        private const val WAIT_TIME_TO_SHOW_NETWORK_WARNING = 30000L
        private const val WAIT_TIME_TO_RESTART_SERVICES = 5000L

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
         * Sends a broadcast to update the transfer widget where needed.
         *
         * @param transferType  the transfer type.
         */
        @JvmStatic
        fun launchTransferUpdateIntent(transferType: Int) {
            MegaApplication.getInstance().sendBroadcast(
                Intent(BROADCAST_ACTION_INTENT_TRANSFER_UPDATE)
                    .putExtra(TRANSFER_TYPE, transferType)
            )
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
            dbH: DatabaseHandler
        ) {
            val id = dbH.setCompletedTransfer(completedTransfer)
            completedTransfer.id = id

            MegaApplication.getInstance().sendBroadcast(
                Intent(BROADCAST_ACTION_TRANSFER_FINISH)
                    .putExtra(COMPLETED_TRANSFER, completedTransfer)
            )
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
            mBuilder: Notification.Builder
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

        /**
         * Checks if should update the scanning folder dialog in case the folder transfer stage
         * is STAGE_SCAN, STAGE_CREATE_TREE, STAGE_GEN_TRANSFERS, STAGE_PROCESS_TRANSFER_QUEUE or
         * STAGE_TRANSFERRING_FILES.
         *
         * @return True if should update it, false otherwise.
         */
        @JvmStatic
        fun shouldUpdateScanningFolderDialog(transfer: MegaTransfer): Boolean =
            transfer.stage.toInt() == MegaTransfer.STAGE_SCAN
                    || transfer.stage.toInt() == MegaTransfer.STAGE_CREATE_TREE
                    || transfer.stage.toInt() == MegaTransfer.STAGE_GEN_TRANSFERS
                    || transfer.stage.toInt() == MegaTransfer.STAGE_PROCESS_TRANSFER_QUEUE
                    || transfer.stage.toInt() == MegaTransfer.STAGE_TRANSFERRING_FILES
    }

    private var networkTimer: CountDownTimer? = null

    private var transferOverQuotaTimestamp: Long = 0
    private var hasNotToBeShowDueToTransferOverQuota = false
    private var isCurrentTransferOverQuota = false
    private var isOnTransfersSection = false
    private var failedTransfers = false
    private var transferOverQuotaNotificationShown = false
    private var isTransferOverQuotaBannerShown = false
    private var resumeTransfersWarningHasAlreadyBeenShown = false
    private var shouldShowNetworkWarning = false

    private val pausedTransfers = ArrayList<String>()

    private var cancelTransferToken: MegaCancelToken? = null
    private var scanningFolderTransfer: Pair<Int, Long>? = null

    init {
        resetTransferOverQuotaTimestamp()
    }

    fun resetDefaults() {
        networkTimer = null
        transferOverQuotaTimestamp = 0
        hasNotToBeShowDueToTransferOverQuota = false
        isCurrentTransferOverQuota = false
        isOnTransfersSection = false
        failedTransfers = false
        transferOverQuotaNotificationShown = false
        isTransferOverQuotaBannerShown = false
        resumeTransfersWarningHasAlreadyBeenShown = false
        shouldShowNetworkWarning = false
        pausedTransfers.clear()
        cancelTransferToken = null
        scanningFolderTransfer = null
    }

    /**
     * Checks if the queue of transfers is paused or all the current in-progress transfers are individually.
     *
     * @return True if the queue of transfers or all the current in-progress transfers are paused, false otherwise.
     */
    fun areTransfersPaused(): Boolean =
        megaApi.areTransfersPaused(TYPE_DOWNLOAD) || megaApi.areTransfersPaused(TYPE_UPLOAD)
                || megaApi.numPendingDownloads + megaApi.numPendingUploads == pausedTransfers.size

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
        if (transfer?.state == MegaTransfer.STATE_PAUSED) {
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
     * Sets if the widget has to be shown depending on if it is on transfer over quota
     * and the Transfers section has been opened from the transfers widget.
     * Also sets if the "transfer over quota" banner has to be shown due to the same reason.
     *
     * @param hasNotToBeShowDueToTransferOverQuota  true if it is on transfer over quota and the Transfers section
     * has been opened from the transfers widget, false otherwise
     */
    fun setHasNotToBeShowDueToTransferOverQuota(hasNotToBeShowDueToTransferOverQuota: Boolean) {
        this.hasNotToBeShowDueToTransferOverQuota = hasNotToBeShowDueToTransferOverQuota
        setTransferOverQuotaBannerShown(hasNotToBeShowDueToTransferOverQuota)
    }

    /**
     * Checks if the transfers widget has to be shown.
     * If the widget does not have to be shown means that:
     * the user is in transfer over quota, there is not any upload transfer in progress
     * and they already opened the transfers section by clicking the widget.
     *
     * @return True if the widget does not have to be shown, false otherwise
     */
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

                setShouldShowNetworkWarning(true)
                launchTransferUpdateIntent(NO_TYPE)
            }
        }.start()
    }

    /**
     * Cancels the CountDownTimer to show warnings related to no internet connection.
     */
    fun resetNetworkTimer() {
        if (networkTimer != null) {
            networkTimer?.cancel()
            setShouldShowNetworkWarning(false)
            launchTransferUpdateIntent(NO_TYPE)
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
                if (megaApi.numPendingDownloads > 0) {
                    val downloadServiceIntent =
                        Intent(app, DownloadService::class.java)
                            .setAction(Constants.ACTION_RESTART_SERVICE)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !app.isActivityVisible) {
                        app.startForegroundService(downloadServiceIntent)
                    } else {
                        app.startService(downloadServiceIntent)
                    }
                }

                if (megaApi.numPendingUploads > 0) {
                    val uploadServiceIntent = Intent(app, UploadService::class.java)
                        .setAction(Constants.ACTION_RESTART_SERVICE)

                    val chatUploadServiceIntent = Intent(app, ChatUploadService::class.java)
                        .setAction(Constants.ACTION_RESTART_SERVICE)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !app.isActivityVisible) {
                        app.startForegroundService(uploadServiceIntent)
                        app.startForegroundService(chatUploadServiceIntent)
                    } else {
                        app.startService(uploadServiceIntent)
                        app.startService(chatUploadServiceIntent)
                    }
                }
            } catch (e: Exception) {
                LogUtil.logWarning("Exception checking pending transfers", e)
            }
        }, WAIT_TIME_TO_RESTART_SERVICES)
    }

    /**
     * Creates a cancel token to cancel a folder transfer.
     *
     * @return The created cancel token.
     */
    fun createCancelTransferToken(): MegaCancelToken {
        val cancelToken = MegaCancelToken.createInstance()
        cancelTransferToken = cancelToken
        return cancelToken
    }

    /**
     * Cancels a folder transfer if exists.
     */
    fun cancelFolderTransfer() {
        cancelTransferToken?.cancel()
    }

    /**
     * Resets the cancel token when the folder transfer has been processed.
     */
    fun resetCancelTransferToken() {
        cancelTransferToken = null
    }

    fun checkFolderTransfer(transfer: MegaTransfer) {
        if (scanningFolderTransfer?.first == transfer.tag
            && scanningFolderTransfer?.second == transfer.stage) {
            return
        }

        scanningFolderTransfer = Pair(transfer.tag, transfer.stage)

        if (shouldUpdateScanningFolderDialog(transfer)) {
            LiveEventBus.get(EVENT_SHOW_SCANNING_FOLDER_DIALOG, MegaTransfer::class.java)
                .post(transfer)
        }
    }

    /**
     * Checks if the transfer over quota has occurred at this moment
     * or it occurred in other past moment.
     *
     * @return  True if the transfer over quota has occurred at this moment, false otherwise.
     */
    fun isCurrentTransferOverQuota(): Boolean =
        isCurrentTransferOverQuota

    fun setCurrentTransferOverQuota(currentTransferOverQuota: Boolean) {
        isCurrentTransferOverQuota = currentTransferOverQuota
    }

    fun setIsOnTransfersSection(isOnTransfersSection: Boolean) {
        this.isOnTransfersSection = isOnTransfersSection
    }

    fun isOnTransfersSection(): Boolean = isOnTransfersSection

    fun setFailedTransfers(failedTransfers: Boolean) {
        this.failedTransfers = failedTransfers
    }

    fun thereAreFailedTransfers(): Boolean = failedTransfers

    fun setTransferOverQuotaNotificationShown(transferOverQuotaNotificationShown: Boolean) {
        this.transferOverQuotaNotificationShown = transferOverQuotaNotificationShown
    }

    fun isTransferOverQuotaNotificationShown(): Boolean = transferOverQuotaNotificationShown

    fun setTransferOverQuotaBannerShown(transferOverQuotaBannerShown: Boolean) {
        isTransferOverQuotaBannerShown = transferOverQuotaBannerShown
    }

    fun isTransferOverQuotaBannerShown(): Boolean =
        isTransferOverQuotaBannerShown

    fun setResumeTransfersWarningHasAlreadyBeenShown(resumeTransfersWarningHasAlreadyBeenShown: Boolean) {
        this.resumeTransfersWarningHasAlreadyBeenShown = resumeTransfersWarningHasAlreadyBeenShown
    }

    fun isResumeTransfersWarningHasAlreadyBeenShown(): Boolean =
        resumeTransfersWarningHasAlreadyBeenShown

    fun setShouldShowNetworkWarning(shouldShowNetworkWarning: Boolean) {
        this.shouldShowNetworkWarning = shouldShowNetworkWarning
    }

    fun shouldShowNetWorkWarning(): Boolean = shouldShowNetworkWarning
}