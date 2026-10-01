package mega.privacy.android.domain.entity.billing

import mega.privacy.android.domain.entity.Subscription

/**
 * The discounted plan promoted by the offer landing dialog (DSN-3130), together with whether the
 * campaign discounts other plans too.
 *
 * @property subscription the plan to promote
 * @property hasMultipleOffers whether at least two plans carry an active offer, in which case the
 * dialog offers a way to see all plans instead of only [subscription]. Counts plans, not billing
 * periods: a plan discounted on both its monthly and yearly option is a single offer.
 */
data class RecommendedSubscriptionOffer(
    val subscription: Subscription,
    val hasMultipleOffers: Boolean,
)
