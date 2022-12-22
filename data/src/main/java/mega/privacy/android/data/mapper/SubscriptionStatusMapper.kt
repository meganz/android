package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.SubscriptionStatus
import nz.mega.sdk.MegaAccountDetails

/**
 * Map [Int] to [SubscriptionStatus]
 */
internal typealias SubscriptionStatusMapper = (@JvmSuppressWildcards Int) -> @JvmSuppressWildcards SubscriptionStatus?

/**
 * Map [Int] to [SubscriptionStatus]. Return value can be subclass of [SubscriptionStatus]
 */
internal fun toSubscriptionStatus(status: Int): SubscriptionStatus? = when (status) {
    MegaAccountDetails.SUBSCRIPTION_STATUS_NONE -> SubscriptionStatus.NONE
    MegaAccountDetails.SUBSCRIPTION_STATUS_VALID -> SubscriptionStatus.VALID
    MegaAccountDetails.SUBSCRIPTION_STATUS_INVALID -> SubscriptionStatus.INVALID
    else -> null
}