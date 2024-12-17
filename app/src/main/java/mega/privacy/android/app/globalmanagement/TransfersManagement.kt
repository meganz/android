package mega.privacy.android.app.globalmanagement

import android.os.CountDownTimer
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
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
) {

    companion object {
        private const val WAIT_TIME_TO_SHOW_WARNING = 60000L
    }

    private var networkTimer: CountDownTimer? = null

    private var transferOverQuotaTimestamp: Long = 0
    var isCurrentTransferOverQuota = false
    var isOnTransfersSection = false
    var isTransferOverQuotaNotificationShown = false
    var isTransferOverQuotaBannerShown = false

    init {
        resetTransferOverQuotaTimestamp()
    }

    fun resetDefaults() {
        networkTimer = null
        transferOverQuotaTimestamp = 0
        isCurrentTransferOverQuota = false
        isOnTransfersSection = false
        isTransferOverQuotaNotificationShown = false
        isTransferOverQuotaBannerShown = false
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
}
