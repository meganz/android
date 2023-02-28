package mega.privacy.android.domain.entity

/**
 * Data class containing all the required to present a product (PRO plan subscription) available.
 *
 * @property handle     Product handle
 * @property level      Product level (PRO I = 1, PRO II = 2, PRO III = 3, PRO LITE = 4, etc.)
 * @property months     Number of subscription months of the (1 for monthly or 12 for yearly)
 * @property storage    Amount of storage of the product in Gb
 * @property transfer   Amount of transfer quota of the product in Gb
 * @property amount     Amount or price of the product
 * @property currency   Currency of the product
 * @property isBusiness Flag to indicate if the product is business or not
 */
data class Product(
    val handle: Long,
    val level: Int,
    val months: Int,
    val storage: Int,
    val transfer: Int,
    val amount: Int,
    val currency: String?,
    val isBusiness: Boolean,
) {
    /**
     * Is monthly
     */
    val isMonthly: Boolean
        get() = months == 1

    /**
     * Is yearly
     */
    val isYearly: Boolean
        get() = months == 12
}