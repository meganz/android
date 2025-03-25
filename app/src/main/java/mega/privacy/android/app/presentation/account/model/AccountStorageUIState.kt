package mega.privacy.android.app.presentation.account.model

/**
 * UI state for account storage
 *
 * @property totalStorage   Total storage
 * @property totalStorageFormatted  Total storage formatted
 * @property baseStorage        Base storage
 * @property baseStorageFormatted Base storage formatted
 * @property lastAdsClosingTimestamp Last ads closing timestamp
 * @property storageUsedPercentage Storage used percentage
 */
data class AccountStorageUIState(
    val totalStorage: Long? = null,
    val totalStorageFormatted: String? = null,
    val baseStorage: Long? = null,
    val baseStorageFormatted: String = "",
    val lastAdsClosingTimestamp: Long = 0L,
    val storageUsedPercentage: Int = 0,
)