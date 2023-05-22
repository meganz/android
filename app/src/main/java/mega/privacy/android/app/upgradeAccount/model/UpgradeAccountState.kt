package mega.privacy.android.app.upgradeAccount.model

import mega.privacy.android.domain.entity.AccountType

/**
 * Upgrade Account state
 *
 * @property localisedSubscriptionsList list of all monthly localised subscriptions available on app, default empty
 * @property currentSubscriptionPlan current subscribed plan, default Free plan
 * @constructor Create default Upgrade Account state
 */
data class UpgradeAccountState(
    val localisedSubscriptionsList: List<LocalisedSubscription>,
    val currentSubscriptionPlan: AccountType?,
    val showBillingWarning: Boolean,
    val showBuyNewSubscriptionDialog: Boolean = false,
    val currentPayment: UpgradePayment,
)