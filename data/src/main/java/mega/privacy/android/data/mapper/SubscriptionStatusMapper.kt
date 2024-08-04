package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.SubscriptionStatus
import nz.mega.sdk.MegaAccountDetails
import javax.inject.Inject

/**
 * Subscription Status Mapper
 */
internal class SubscriptionStatusMapper @Inject constructor() {

    /**
     * Invoke
     * @param status Int
     * @return [SubscriptionStatus]
     */
    operator fun invoke(status: Int) = when (status) {
        MegaAccountDetails.SUBSCRIPTION_STATUS_NONE -> SubscriptionStatus.NONE
        MegaAccountDetails.SUBSCRIPTION_STATUS_VALID -> SubscriptionStatus.VALID
        MegaAccountDetails.SUBSCRIPTION_STATUS_INVALID -> SubscriptionStatus.INVALID
        else -> null
    }
}