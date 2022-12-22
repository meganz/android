package mega.privacy.android.domain.entity.account

/**
 * Generic SKU object, used to unify corresponding platform dependent purchase object.
 *
 * In HMS, it's ProductInfo.
 * In GMS, it's SkuDetails.
 */
data class MegaSku(
    /**
     * SKU of the product.
     */
    val sku: String,
    /**
     * Price of the sku in corresponding platform.
     */
    val priceAmountMicros: Long,
    /**
     * Currency code, used to format price.
     */
    val priceCurrencyCode: String
)