package mega.privacy.android.app.globalmanagement

import android.os.CountDownTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.SDCardOperator
import mega.privacy.android.app.utils.Util.isOnline
import mega.privacy.android.data.mapper.transfer.CompletedTransferMapper
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.getSDCardDownloadAppData
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.transfers.BroadcastFailedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.completed.AddCompletedTransferIfNotExistUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransfersQueueUseCase
import mega.privacy.android.domain.usecase.transfers.sd.DeleteSdTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.sd.GetAllSdTransfersUseCase
import nz.mega.sdk.MegaApiAndroid
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton class for transfers management.
 *
 * @property megaApi    MegaApiAndroid instance to check transfers status.
 */
@Singleton
class TransfersManagement @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val broadcastFailedTransferUseCase: BroadcastFailedTransferUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase,
    private val addCompletedTransferIfNotExistUseCase: AddCompletedTransferIfNotExistUseCase,
    private val deleteSdTransferByTagUseCase: DeleteSdTransferByTagUseCase,
    private val getAllSdTransfersUseCase: GetAllSdTransfersUseCase,
    private val getTransferByTagUseCase: GetTransferByTagUseCase,
    private val completedTransferMapper: CompletedTransferMapper,
    private val pauseTransfersQueueUseCase: PauseTransfersQueueUseCase,
) {

    companion object {
        private const val WAIT_TIME_TO_SHOW_WARNING = 60000L
        private const val WAIT_TIME_TO_SHOW_NETWORK_WARNING = 30000L
    }

    private var networkTimer: CountDownTimer? = null

    private var transferOverQuotaTimestamp: Long = 0
    var isCurrentTransferOverQuota = false
    var isOnTransfersSection = false
    private var areFailedTransfers = false
    var isTransferOverQuotaNotificationShown = false
    var isTransferOverQuotaBannerShown = false
    var hasResumeTransfersWarningAlreadyBeenShown = false
    var shouldShowNetworkWarning = false

    init {
        resetTransferOverQuotaTimestamp()
    }

    fun resetDefaults() {
        networkTimer = null
        transferOverQuotaTimestamp = 0
        isCurrentTransferOverQuota = false
        isOnTransfersSection = false
        areFailedTransfers = false
        isTransferOverQuotaNotificationShown = false
        isTransferOverQuotaBannerShown = false
        hasResumeTransfersWarningAlreadyBeenShown = false
        shouldShowNetworkWarning = false
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
    @Deprecated(
        message = "There's a use case to get the transfer over quota that: MonitorTransferOverQuotaUseCase",
        replaceWith = ReplaceWith("MonitorTransferOverQuotaUseCase().first()")
    )
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
        isTransferOverQuotaBannerShown = hasNotToBeShowDueToTransferOverQuota
    }

    /**
     * Starts a CountDownTimer after show warnings related to no internet connection.
     * If the timer finishes, launches a Broadcast to update the widget.
     */
    suspend fun startNetworkTimer() = withContext(Dispatchers.Main) {
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
    suspend fun resetNetworkTimer() = withContext(Dispatchers.Main) {
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
                    pauseTransfersQueueUseCase(true)
                }.onFailure {
                    Timber.e(it)
                }
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

    fun setAreFailedTransfers(failed: Boolean) {
        areFailedTransfers = failed
        applicationScope.launch {
            broadcastFailedTransferUseCase(failed)
        }
    }

    fun getAreFailedTransfers(): Boolean = areFailedTransfers
}
