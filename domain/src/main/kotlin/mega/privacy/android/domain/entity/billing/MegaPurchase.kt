package mega.privacy.android.domain.entity.billing

import mega.privacy.android.domain.entity.account.Skus

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
     * Token of the purchase.
     */
    val token: String? = null,
) {
    /**
     * product level
     */
    val level: Int = when (sku) {
        Skus.SKU_PRO_LITE_MONTH, Skus.SKU_PRO_LITE_YEAR -> 0
        Skus.SKU_PRO_I_MONTH, Skus.SKU_PRO_I_YEAR -> 1
        Skus.SKU_PRO_II_MONTH, Skus.SKU_PRO_II_YEAR -> 2
        Skus.SKU_PRO_III_MONTH, Skus.SKU_PRO_III_YEAR -> 3
        else -> -1
    }
}