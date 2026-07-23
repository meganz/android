package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.account.OfferPeriod

/**
 * Subscription
 *
 * @property accountType     Account type (PRO I, PRO II, PRO III, PRO LITE, etc.)
 * @property handle          Subscription option handle
 * @property storage         Amount of storage of the subscription option
 * @property transfer        Amount of transfer quota of the subscription option
 * @property amount          Currency amount object, containing price amount for subscription option and local currency
 * @property discountedAmountMonthly    Currency amount object, containing discounted price amount for subscription option and local currency
 * @property discountedPercentage  Discount percentage for the subscription option
 * @property offerId         Unique identifier for the offer
 * @property offerPeriod     Period of the offer (Day, Month, or Year)
 * @property discountName    Localised campaign name from the SDK, or null/empty when none
 * @property offerValidUntil Mobile offer expiry timestamp in seconds (utqa "mo.e"), or null when the offer has no expiry
 * @property offerFlags      Mobile offer flags bitmask from the SDK, or null when the offer carries no flags
 */
data class Subscription(
    val sku: String,
    val accountType: AccountType,
    val handle: Long,
    val storage: Int,
    val transfer: Int,
    val amount: CurrencyAmount,
    val offerId: String? = null,
    val discountedAmountMonthly: CurrencyAmount? = null,
    val discountedPercentage: Int? = null,
    val offerPeriod: OfferPeriod? = null,
    val discountName: String? = null,
    val offerValidUntil: Long? = null,
    val offerFlags: Long? = null,
) {
    /**
     * Has offer
     */
    val hasOffer: Boolean
        get() = discountedAmountMonthly != null
}