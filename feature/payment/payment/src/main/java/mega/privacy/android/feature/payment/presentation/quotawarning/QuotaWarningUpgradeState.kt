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
 * @property isLoggedIn whether a user is signed in; anonymous users reach the screen from public
 * links, so the screen upsells them without any account data and sends both actions to login
 * @property isLoading whether the recommended plan is still being resolved
 * @property isConnected whether the device has an internet connection
 * @property hasLoadError whether fetching the available plans failed; stays false while a fetch is
 * still in flight, so [isLoading] keeps the skeleton up instead
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
    val isLoggedIn: Boolean = true,
    val isLoading: Boolean = true,
    val isConnected: Boolean = true,
    val hasLoadError: Boolean = false,
) {
    /**
     * Whether the current account is on a paid (Pro) plan. An unknown plan counts as free, matching
     * the plan name the screen falls back to.
     */
    val isProUser: Boolean = isLoggedIn && currentPlan != null && currentPlan != AccountType.FREE

    /**
     * Whether the screen shows the current plan and the usage figures for [metric]. Free accounts
     * have a storage total to compare against but no transfer total, so only their transfer usage
     * is left out. Logged-out users have no account data at all.
     */
    fun showQuotaDetails(metric: QuotaMetric): Boolean =
        isLoggedIn && (isProUser || metric == QuotaMetric.Storage)

    /**
     * Whether the loaded quota data is being shown, as opposed to the skeleton or the error state.
     */
    val isContentShown: Boolean = !isLoading && isConnected && !hasLoadError
}
