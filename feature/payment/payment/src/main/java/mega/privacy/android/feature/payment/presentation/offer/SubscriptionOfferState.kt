package mega.privacy.android.feature.payment.presentation.offer

import mega.privacy.android.feature.payment.model.LocalisedSubscription

/**
 * UI state for the subscription offer landing screen.
 *
 * @property isLoading true while the recommended offer is being loaded
 * @property offerSubscription the discounted plan to promote, null while loading or when no plan
 * carries an offer
 * @property isMonthly whether the promoted offer is on the monthly billing period
 * @property offerValidUntil the offer expiry as epoch seconds, null to hide the countdown
 * @property hasMultipleOffers whether the campaign discounts more than one plan, in which case the
 * screen offers a way to see all plans instead of only the promoted one
 * @property isConnected whether the device has an internet connection
 * @property hasLoadError whether loading the offer failed; stays false when the load succeeded but
 * simply found no offer, so that case still closes the screen instead of showing an error
 */
data class SubscriptionOfferState(
    val isLoading: Boolean = true,
    val offerSubscription: LocalisedSubscription? = null,
    val isMonthly: Boolean = true,
    val offerValidUntil: Long? = null,
    val hasMultipleOffers: Boolean = false,
    val isConnected: Boolean = true,
    val hasLoadError: Boolean = false,
)
