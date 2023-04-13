package mega.privacy.android.app.upgradeAccount.model

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Subscription

/**
 * Upgrade Account state
 *
 * @property subscriptionsList list of all subscriptions available on app, default empty
 * @property currentSubscriptionPlan current subscribed plan, default Free plan
 * @constructor Create default Upgrade Account state
 */
data class UpgradeAccountState(
    val subscriptionsList: List<Subscription>,
    val currentSubscriptionPlan: AccountType?,
    val showBillingWarning: Boolean,
    val showBuyNewSubscriptionDialog: Boolean = false,
    val currentPayment: UpgradePayment,
)