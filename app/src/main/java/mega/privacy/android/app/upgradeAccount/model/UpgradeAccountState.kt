package mega.privacy.android.app.upgradeAccount.model

import mega.privacy.android.domain.entity.AccountType

/**
 * Upgrade Account state
 *
 * @property subscriptionsList list of all  localised subscriptions available on app, default empty
 * @property currentSubscriptionPlan current subscribed plan, default Free plan
 * @constructor Create default Upgrade Account state
 */
data class UpgradeAccountState(
    val subscriptionsList: List<LocalisedSubscription>,
    val currentSubscriptionPlan: AccountType?,
    val showBillingWarning: Boolean,
    val showBuyNewSubscriptionDialog: Boolean = false,
    val currentPayment: UpgradePayment,
)