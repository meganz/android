package mega.privacy.android.app.upgradeAccount.model

import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.Subscription

/**
 * Upgrade Account state
 *
 * @property localisedSubscriptionsList list of all monthly localised subscriptions available on app, default empty
 * @property product list of Product subscriptions
 * @property cheapestSubscriptionAvailable cheapest subscription, which is available for user (Pro Lite or Pro I)
 * @constructor Create default Upgrade Account state
 */
data class ChooseAccountState(
    val localisedSubscriptionsList: List<LocalisedSubscription> = emptyList(),
    val product: List<Product> = emptyList(),
    val cheapestSubscriptionAvailable: Subscription? = null,
)