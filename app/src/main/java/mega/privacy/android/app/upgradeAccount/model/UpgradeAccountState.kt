package mega.privacy.android.app.upgradeAccount.model

import mega.privacy.android.domain.entity.AccountType

/**
 * Upgrade Account state
 *
 * @property localisedSubscriptionsList list of all monthly localised subscriptions available on app, default empty
 * @property currentSubscriptionPlan current subscribed plan, default Free plan
 * @property showBillingWarning boolean to determine if billing warning should be shown or hidden
 * @property showBuyNewSubscriptionDialog boolean to determine if buy new subscription dialog should be shown or hidden
 * @property currentPayment current payment method, default Credit Card
 * @property isMonthlySelected boolean to determine if monthly plan was selected
 * @property chosenPlan account type chosen by user (when user taps on one of the plans)
 * @property isPaymentMethodAvailable boolean to determine if Payments are available through Google Play Store
 * @property userSubscription user subscription to determine if user has current yearly or monthly subscription
 * @property showNoAdsFeature boolean to determine if No Ads feature should be shown or hidden (part of the In-App Advertisement experiment)
 * @property isCrossAccountMatch boolean to determine if the account passed is the same as the one currently logged in
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
    val userSubscription: UserSubscription = UserSubscription.NOT_SUBSCRIBED,
    val isCrossAccountMatch: Boolean = true,
    val showNoAdsFeature: Boolean = false,
)