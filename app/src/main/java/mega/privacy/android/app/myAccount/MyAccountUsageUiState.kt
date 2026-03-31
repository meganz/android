package mega.privacy.android.app.myAccount

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
 * @property versionsInfo Formatted string of previous versions size
 * @property accountType The type of the account
 * @property storageState The current state of the storage
 * @property isBusinessAccount Whether this is a business account
 * @property isProFlexiAccount Whether this is a Pro Flexi account
 * @property isMasterBusinessAccount Whether this is the master admin of a business account (sub-accounts are false)
 * @property usedStoragePercentage Percentage of storage used
 * @property usedStorage Formatted string of used storage
 * @property totalStorage Formatted string of total storage
 * @property usedTransfer Formatted string of used transfer
 * @property usedTransferPercentage Percentage of transfer used
 * @property totalTransfer Formatted string of total transfer
 * @property usedTransferStatus Status of transfer usage
 * @property cloudStorage Formatted string of cloud drive storage
 * @property incomingStorage Formatted string of incoming shares storage
 * @property rubbishStorage Formatted string of rubbish bin storage
 * @property backupStorageSize Raw backup storage size in bytes
 * @property backupStorage Formatted string of backup storage
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
    val versionsInfo: String = "",
    val accountType: AccountType = AccountType.FREE,
    val storageState: StorageState = StorageState.Unknown,
    val isBusinessAccount: Boolean = false,
    val isProFlexiAccount: Boolean = false,
    val isMasterBusinessAccount: Boolean = false,
    val usedStoragePercentage: Int = 0,
    val usedStorage: String = "",
    val totalStorage: String = "",
    val usedTransfer: String = "",
    val usedTransferPercentage: Int = 0,
    val totalTransfer: String = "",
    val usedTransferStatus: UsedTransferStatus = UsedTransferStatus.NoTransferProblems,
    val cloudStorage: String = "",
    val incomingStorage: String = "",
    val rubbishStorage: String = "",
    val backupStorageSize: Long = 0L,
    val backupStorage: String = "",
    val renewTime: Long = 0L,
    val proExpirationTime: Long = 0L,
    val hasRenewableSubscription: Boolean = false,
    val hasExpirableSubscription: Boolean = false,
    val businessStatus: BusinessAccountStatus? = null,
    val paymentAlertType: PaymentAlertType = PaymentAlertType.None,
    val paymentAlertDate: Long = 0L,
)

