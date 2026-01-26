package mega.privacy.android.feature.payment.model

import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.agesignal.UserAgeComplianceStatus

/**
 * Upgrade Account state
 *
 * @property localisedSubscriptionsList list of all monthly localised subscriptions available on app, default empty
 * @property product list of Product subscriptions
 * @property cheapestSubscriptionAvailable cheapest subscription, which is available for user (Pro Lite or Pro I)
 * @property currentSubscriptionPlan current subscribed plan, default Free plan
 * @property subscriptionCycle current subscription cycle (monthly/yearly), default UNKNOWN
 * @constructor Create default Upgrade Account state
 */
data class ChooseAccountState(
    val localisedSubscriptionsList: List<LocalisedSubscription> = emptyList(),
    val product: List<Product> = emptyList(),
    val cheapestSubscriptionAvailable: LocalisedSubscription? = null,
    val currentSubscriptionPlan: AccountType? = null,
    val subscriptionCycle: AccountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
    val userAgeComplianceStatus: UserAgeComplianceStatus = UserAgeComplianceStatus.AdultVerified,
    val isExternalCheckoutEnabled: Boolean = false,
    val isExternalCheckoutDefault: Boolean = false,
    val isSingleActivityEnabled: Boolean = false,
) {
    // checking if there is any discount available it's different from current plan
    fun hasDiscount() = localisedSubscriptionsList.any {
        when (subscriptionCycle) {
            AccountSubscriptionCycle.MONTHLY ->
                it.yearlySubscription.discountedAmountMonthly != null && it.accountType != currentSubscriptionPlan

            AccountSubscriptionCycle.YEARLY ->
                it.monthlySubscription.discountedAmountMonthly != null && it.accountType != currentSubscriptionPlan

            else -> (it.monthlySubscription.discountedAmountMonthly != null || it.yearlySubscription.discountedAmountMonthly != null)
                    && it.accountType != currentSubscriptionPlan
        }
    }
}
