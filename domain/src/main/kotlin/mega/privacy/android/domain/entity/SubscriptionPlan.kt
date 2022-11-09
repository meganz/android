package mega.privacy.android.domain.entity

/**
 * Subscription plan
 *
 * @property pricing pricing class containg all pricing info
 * @property subscription subscription class containing all info for subscription plans (PRO I, etc)
 */
data class SubscriptionPlan(
    val pricing: Pricing,
    val subscription: Subscription,
)
