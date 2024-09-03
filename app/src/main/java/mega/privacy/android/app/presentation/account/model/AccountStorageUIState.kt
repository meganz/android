package mega.privacy.android.app.presentation.account.model

import mega.privacy.android.domain.entity.account.AccountDetail

/**
 * UI state for account storage
 *
 * @property totalStorage   Total storage
 * @property totalStorageFormatted  Total storage formatted
 * @property baseStorage        Base storage
 * @property baseStorageFormatted Base storage formatted
 */
data class AccountStorageUIState(
    val totalStorage: Long? = null,
    val totalStorageFormatted: String? = null,
    val baseStorage: Long? = null,
    val baseStorageFormatted: String = "",
)