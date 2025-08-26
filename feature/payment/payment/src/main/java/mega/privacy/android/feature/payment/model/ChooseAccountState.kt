package mega.privacy.android.feature.payment.model

import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Product

/**
 * Upgrade Account state
 *
 * @property localisedSubscriptionsList list of all monthly localised subscriptions available on app, default empty
 * @property product list of Product subscriptions
 * @property cheapestSubscriptionAvailable cheapest subscription, which is available for user (Pro Lite or Pro I)
 * @property isPaymentMethodAvailable boolean to determine if Payments are available through Google Play Store
 * @property currentSubscriptionPlan current subscribed plan, default Free plan
 * @property subscriptionCycle current subscription cycle (monthly/yearly), default UNKNOWN
 * @constructor Create default Upgrade Account state
 */
data class ChooseAccountState(
    val localisedSubscriptionsList: List<LocalisedSubscription> = emptyList(),
    val product: List<Product> = emptyList(),
    val cheapestSubscriptionAvailable: LocalisedSubscription? = null,
    val isPaymentMethodAvailable: Boolean = true,
    val currentSubscriptionPlan: AccountType? = null,
    val subscriptionCycle: AccountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
)