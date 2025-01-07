package mega.privacy.android.app.globalmanagement

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton class for transfers management.
 *
 * @property megaApi    MegaApiAndroid instance to check transfers status.
 */
@Singleton
class TransfersManagement @Inject constructor() {

    var isTransferOverQuotaNotificationShown = false
    var isTransferOverQuotaBannerShown = false

    fun resetDefaults() {
        isTransferOverQuotaNotificationShown = false
        isTransferOverQuotaBannerShown = false
    }

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
