package mega.privacy.android.domain.entity.account

/**
 * Offer details for subscription products
 *
 * @property offerId Unique identifier for the offer
 * @property discountedPriceMonthly Discounted price in micros (null if no discount)
 * @property discountPercentage Discount percentage (e.g., 50 for 50% off, null if no discount)
 * @property offerPeriod Period of the offer (Day, Month, or Year)
 */
data class OfferDetail(
    val offerId: String?,
    val discountedPriceMonthly: CurrencyPoint?,
    val discountPercentage: Int?,
    val offerPeriod: OfferPeriod?,
)