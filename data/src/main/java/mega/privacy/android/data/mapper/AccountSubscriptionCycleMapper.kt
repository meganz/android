package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import javax.inject.Inject

/**
 * Account Renew Cycle Type Mapper
 */
internal class AccountSubscriptionCycleMapper @Inject constructor() {
    /**
     * Invoke
     * @return [AccountSubscriptionCycle]
     * @param  subscriptionCycle
     */
    operator fun invoke(subscriptionCycle: String) = when (subscriptionCycle) {
        "1 M" -> AccountSubscriptionCycle.MONTHLY
        "1 Y" -> AccountSubscriptionCycle.YEARLY
        else -> AccountSubscriptionCycle.UNKNOWN
    }
}