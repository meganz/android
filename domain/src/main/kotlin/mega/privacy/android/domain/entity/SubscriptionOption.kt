package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.account.CurrencyPoint

/**
 * Subscription Option
 *
 * @property accountType     Account type (PRO I, PRO II, PRO III, PRO LITE, etc.)
 * @property months          Number of subscription months (1 for monthly or 12 for yearly)
 * @property handle          Subscription plan handle
 * @property storage         Amount of storage of the subscription plan
 * @property transfer        Amount of transfer quota of the subscription plan
 * @property amount          Price amount of the subscription plan
 * @property currency        Currency of the subscription plan
 * @property hasOffer        Flag to indicate if the subscription option has a mobile offer
 * @property discountName    Localised campaign name for an active discount, or null/empty when none
 * @property offerValidUntil Mobile offer expiry timestamp in seconds (utqa "mo.e"), or null when the offer has no expiry
 * @property offerFlags      Mobile offer flags bitmask (utqa "mo.f") from the SDK, or null when the
 * offer carries no flags. Bit 0 opts the campaign in to being advertised on the mobile promotion
 * surfaces (Home banner, menu banner, offer landing screen); absent flags mean opted out
 * @property offerReshowInterval Time in seconds that must pass before the mobile offer may be shown
 * again (utqa "mo.r"), or null when the offer must not be shown again
 * @property offerCampaignId Campaign group the mobile offer belongs to (utqa "mo.c"), or null when
 * the offer belongs to no campaign group. Offers of the same campaign share this identifier, so
 * they are advertised and dismissed as one
 */
data class SubscriptionOption(
    val sku: String,
    val accountType: AccountType,
    val months: Int,
    val handle: Long,
    val storage: Int,
    val transfer: Int,
    val amount: CurrencyPoint.SystemCurrencyPoint,
    val currency: Currency,
    val hasOffer: Boolean,
    val discountName: String? = null,
    val offerValidUntil: Long? = null,
    val offerFlags: Long? = null,
    val offerReshowInterval: Long? = null,
    val offerCampaignId: Long? = null,
)