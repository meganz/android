package mega.privacy.android.feature.payment.model

/**
 * UI state for account storage
 *
 * @property totalStorage   Total storage
 * @property baseStorage        Base storage
 * @property lastAdsClosingTimestamp Last ads closing timestamp
 * @property storageUsedPercentage Storage used percentage
 */
data class AccountStorageUIState(
    val totalStorage: Long? = null,
    val baseStorage: Long? = null,
    val lastAdsClosingTimestamp: Long = 0L,
    val storageUsedPercentage: Int = 0,
)