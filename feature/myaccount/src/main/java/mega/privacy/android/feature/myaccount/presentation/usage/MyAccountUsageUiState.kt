package mega.privacy.android.feature.myaccount.presentation.usage

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.transfer.UsedTransferStatus

/**
 * Payment alert type for account subscription
 */
enum class PaymentAlertType {
    None,                    // No alert to display
    BusinessExpired,         // Business account expired - red color
    BusinessGracePeriod,     // Business account grace period - amber color
    AccountRenewsOn,         // Account renewal date
    AccountExpiresOn         // Account expiration date
}

/**
 * UI State for My Account Usage Screen
 *
 * @property isUsageContentReady When false, account usage data is not ready for display.
 * @property isFileVersioningEnabled Whether file versioning is enabled
 * @property versionsSize Previous versions size in bytes, null when unavailable
 * @property accountType The type of the account
 * @property storageState The current state of the storage
 * @property isBusinessAccount Whether this is a business account
 * @property isProFlexiAccount Whether this is a Pro Flexi account
 * @property isMasterBusinessAccount Whether this is the master admin of a business account (sub-accounts are false)
 * @property usedStoragePercentage Percentage of storage used
 * @property usedStorage Used storage in bytes, null until loaded
 * @property totalStorage Total storage in bytes, null until loaded
 * @property usedTransfer Used transfer in bytes, null until loaded
 * @property usedTransferPercentage Percentage of transfer used
 * @property totalTransfer Total transfer in bytes, null until loaded
 * @property usedTransferStatus Status of transfer usage
 * @property cloudStorage Cloud drive storage in bytes, null until loaded
 * @property incomingStorage Incoming shares storage in bytes, null until loaded
 * @property rubbishStorage Rubbish bin storage in bytes, null until loaded
 * @property backupStorageSize Backup storage size in bytes
 * @property renewTime Subscription renewal time in milliseconds
 * @property proExpirationTime Subscription expiration time in milliseconds
 * @property hasRenewableSubscription Whether the account has a renewable subscription
 * @property hasExpirableSubscription Whether the account has an expirable subscription
 * @property businessStatus The business account status, null if not a business account
 * @property paymentAlertType The type of payment alert to display
 * @property paymentAlertDate The date to display in the payment alert (renewal or expiration time)
 * @property usageLoadFailed True when any required load path failed (bootstrap or live account detail); show error and navigate back.
 */
@Stable
data class MyAccountUsageUiState(
    val usageLoadFailed: Boolean = false,
    val isUsageContentReady: Boolean = false,
    val isFileVersioningEnabled: Boolean = true,
    val versionsSize: Long? = null,
    val accountType: AccountType = AccountType.FREE,
    val storageState: StorageState = StorageState.Unknown,
    val isBusinessAccount: Boolean = false,
    val isProFlexiAccount: Boolean = false,
    val isMasterBusinessAccount: Boolean = false,
    val usedStoragePercentage: Int = 0,
    val usedStorage: Long? = null,
    val totalStorage: Long? = null,
    val usedTransfer: Long? = null,
    val usedTransferPercentage: Int = 0,
    val totalTransfer: Long? = null,
    val usedTransferStatus: UsedTransferStatus = UsedTransferStatus.NoTransferProblems,
    val cloudStorage: Long? = null,
    val incomingStorage: Long? = null,
    val rubbishStorage: Long? = null,
    val backupStorageSize: Long = 0L,
    val renewTime: Long = 0L,
    val proExpirationTime: Long = 0L,
    val hasRenewableSubscription: Boolean = false,
    val hasExpirableSubscription: Boolean = false,
    val businessStatus: BusinessAccountStatus? = null,
    val paymentAlertType: PaymentAlertType = PaymentAlertType.None,
    val paymentAlertDate: Long = 0L,
) {
    /** True when the account is not a paid account. */
    val isFreeAccount: Boolean
        get() = !accountType.isPaid

    /** True when the upgrade button should be shown (not Business or Pro Flexi). */
    val showUpgradeButton: Boolean
        get() = !isBusinessAccount && !isProFlexiAccount

    /** True when the payment alert section should be shown. */
    val showPaymentAlert: Boolean
        get() = when {
            isProFlexiAccount -> true
            isBusinessAccount -> isMasterBusinessAccount
            else -> (hasRenewableSubscription || hasExpirableSubscription) && !isFreeAccount
        }
}

