package mega.privacy.android.feature.payment.model

import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Product
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.domain.entity.agesignal.UserAgeComplianceStatus

/**
 * Upgrade Account state
 *
 * @property localisedSubscriptionsList list of all monthly localised subscriptions available on app, default empty
 * @property product list of Product subscriptions
 * @property cheapestSubscriptionAvailable cheapest subscription, which is available for user (Pro Lite or Pro I)
 * @property currentSubscriptionPlan current subscribed plan, default Free plan
 * @property subscriptionCycle current subscription cycle (monthly/yearly), default UNKNOWN
 * @property subscriptionStatus current subscription status (VALID/INVALID/NONE), null if unknown
 * @property subscriptionRenewTime renewal timestamp of the current subscription in seconds, null if unknown
 * @property proExpirationTime expiration timestamp of the current Pro plan in seconds, null if unknown
 * @constructor Create default Upgrade Account state
 */
data class UpgradeAccountState(
    val localisedSubscriptionsList: List<LocalisedSubscription> = emptyList(),
    val product: List<Product> = emptyList(),
    val cheapestSubscriptionAvailable: LocalisedSubscription? = null,
    val currentSubscriptionPlan: AccountType? = null,
    val subscriptionCycle: AccountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
    val subscriptionStatus: SubscriptionStatus? = null,
    val subscriptionRenewTime: Long? = null,
    val proExpirationTime: Long? = null,
    val userAgeComplianceStatus: UserAgeComplianceStatus = UserAgeComplianceStatus.AdultVerified,
    val isSubscriptionFeatureAvailable: Boolean? = null,
) {
    /**
     * Whether the current subscription is an active recurring subscription that renews
     * (as opposed to a one-off purchase / cancelled subscription that expires).
     */
    val isCurrentSubscriptionRenewing: Boolean
        get() = subscriptionStatus == SubscriptionStatus.VALID

    /**
     * Is PRO_III plan
     */
    fun isHighestPlan(): Boolean {
        return currentSubscriptionPlan == AccountType.PRO_III
    }

    // checking if there is any discount available it's different from current plan
    fun hasDiscount() = localisedSubscriptionsList.any {
        when (subscriptionCycle) {
            AccountSubscriptionCycle.MONTHLY ->
                it.yearlySubscription?.discountedAmountMonthly != null && it.accountType != currentSubscriptionPlan

            AccountSubscriptionCycle.YEARLY ->
                it.monthlySubscription?.discountedAmountMonthly != null && it.accountType != currentSubscriptionPlan

            else -> (it.monthlySubscription?.discountedAmountMonthly != null ||
                    it.yearlySubscription?.discountedAmountMonthly != null)
                    && it.accountType != currentSubscriptionPlan
        }
    }
}
