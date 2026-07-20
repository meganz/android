package mega.privacy.android.domain.entity.billing

import mega.privacy.android.domain.entity.account.subscriptionSkuLevel

/**
 * Generic purchase object, used to unify corresponding platform dependent purchase object.
 *
 * In GMS, it's Purchase.
 */
data class MegaPurchase(
    /**
     * SKU of the product.
     */
    val sku: String?,

    /**
     * Receipt of the purchase, will be submitted to API.
     */
    val receipt: String? = null,

    /**
     * State of the purchase.
     */
    val state: Int = 0,

    /**
     * Mega State of the purchase.
     */
    val megaPurchaseState: MegaPurchaseState = MegaPurchaseState.Unspecified,

    /**
     * Token of the purchase.
     */
    val token: String? = null,

    /**
     * Time of the purchase, in milliseconds since epoch (Jan 1, 1970).
     */
    val time: Long = 0,

    /**
     * Whether the subscription is set to auto-renew.
     */
    val isAutoRenewing: Boolean = false,

    /**
     * Obfuscated account id stored on this purchase, reused when replacing the subscription.
     */
    val obfuscatedAccountId: String? = null,
) {
    /**
     * product level
     */
    val level: Int = sku.subscriptionSkuLevel

    val isMonthly: Boolean
        get() = sku?.contains("onemonth") == true
}