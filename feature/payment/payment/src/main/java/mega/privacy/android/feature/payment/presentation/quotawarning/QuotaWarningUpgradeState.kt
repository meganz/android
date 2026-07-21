package mega.privacy.android.feature.payment.presentation.quotawarning

import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.feature.payment.model.LocalisedSubscription

/**
 * UI state for the quota-warning upsell screen.
 *
 * @property currentPlan the user's current plan, null until loaded
 * @property subscriptionCycle the billing cycle of the current plan, used to recommend the matching cycle
 * @property storageUsed used storage in bytes, null until loaded
 * @property storageTotal total storage of the current plan in bytes, null until loaded
 * @property storageUsedPercentage storage usage as a 0..100 percentage (backend-provided)
 * @property transferUsed used transfer quota in bytes, null until loaded
 * @property transferTotal total transfer quota in bytes, null until loaded
 * @property transferUsedPercentage transfer usage as a 0..100 percentage (backend-provided)
 * @property storageState backend storage state that determines the storage warning severity
 * @property isTransferOverQuota whether the backend reports the transfer quota as exceeded
 * @property recommendedSubscription the next-tier plan to recommend, null until loaded or none available
 * @property isHighestPlan whether the user is already on the highest available plan, so no upgrade is offered
 * @property email the current user's email, used to pre-fill the custom-plan support request
 * @property isLoading whether the recommended plan is still being resolved
 */
data class QuotaWarningUpgradeState(
    val currentPlan: AccountType? = null,
    val subscriptionCycle: AccountSubscriptionCycle = AccountSubscriptionCycle.UNKNOWN,
    val storageUsed: Long? = null,
    val storageTotal: Long? = null,
    val storageUsedPercentage: Int = 0,
    val transferUsed: Long? = null,
    val transferTotal: Long? = null,
    val transferUsedPercentage: Int = 0,
    val storageState: StorageState = StorageState.Unknown,
    val isTransferOverQuota: Boolean = false,
    val recommendedSubscription: LocalisedSubscription? = null,
    val isHighestPlan: Boolean = false,
    val email: String? = null,
    val isLoading: Boolean = true,
)
