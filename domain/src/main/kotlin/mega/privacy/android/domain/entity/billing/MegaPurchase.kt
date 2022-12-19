package mega.privacy.android.domain.entity.billing

/**
 * Generic purchase object, used to unify corresponding platform dependent purchase object.
 *
 * In HMS, it's InAppPurchaseData.
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
    val receipt: String?,

    /**
     * State of the purchase.
     */
    val state: Int = 0,

    /**
     * Token of the purchase.
     */
    val token: String?,
)