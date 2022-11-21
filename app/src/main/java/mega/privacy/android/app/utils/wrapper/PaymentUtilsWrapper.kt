package mega.privacy.android.app.utils.wrapper

import mega.privacy.android.app.utils.billing.PaymentUtils
import mega.privacy.android.domain.entity.account.MegaSku

/**
 * Payment Utils wrapper
 */
interface PaymentUtilsWrapper {
    /**
     * get SKU details
     */
    fun getSkuDetails(megaSkuList: List<MegaSku>, sku: String): MegaSku? {
        return PaymentUtils.getSkuDetails(megaSkuList, sku)
    }
}