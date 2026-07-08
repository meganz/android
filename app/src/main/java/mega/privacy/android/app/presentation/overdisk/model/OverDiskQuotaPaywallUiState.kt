package mega.privacy.android.app.presentation.overdisk.model

import mega.privacy.android.domain.entity.Product

/**
 * UI state for the Over Disk Quota Paywall screen.
 *
 * @property isLoading whether the account data is still being loaded.
 * @property email current account email.
 * @property fileCount number of files/nodes in the account.
 * @property usedStorage storage used by the account, in bytes.
 * @property warningTimestamps over quota warning timestamps (Unix seconds).
 * @property deadlineTimestamp over quota deletion deadline (Unix seconds), negative if none.
 * @property products available pricing products, used to compute the required PRO plan.
 */
data class OverDiskQuotaPaywallUiState(
    val isLoading: Boolean = true,
    val email: String = "",
    val fileCount: Long = 0L,
    val usedStorage: Long = 0L,
    val warningTimestamps: List<Long> = emptyList(),
    val deadlineTimestamp: Long = -1L,
    val products: List<Product> = emptyList(),
)
