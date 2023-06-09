package mega.privacy.android.app.upgradeAccount.model

import mega.privacy.android.domain.entity.AccountType

/**
 * Upgrade Account state
 *
 * @property localisedSubscriptionsList list of all monthly localised subscriptions available on app, default empty
 * @property currentSubscriptionPlan current subscribed plan, default Free plan
 * @property showBillingWarning boolean to determine if billing warning should be shown or hidden
 * @property isMonthlySelected boolean to determine if monthly plan was selected
 * @property chosenPlan account type chosen by user (when user taps on one of the plans)
 * @property isPaymentMethodAvailable boolean to determine if Payments are available through Google Play Store
 * @constructor Create default Upgrade Account state
 */
data class UpgradeAccountState(
    val localisedSubscriptionsList: List<LocalisedSubscription>,
    val currentSubscriptionPlan: AccountType?,
    val showBillingWarning: Boolean,
    val showBuyNewSubscriptionDialog: Boolean = false,
    val currentPayment: UpgradePayment,
    val isMonthlySelected: Boolean = false,
    val chosenPlan: AccountType = AccountType.FREE,
    val isPaymentMethodAvailable: Boolean = true,
)