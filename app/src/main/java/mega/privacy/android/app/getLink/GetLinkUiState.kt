package mega.privacy.android.app.getLink

import mega.privacy.android.domain.entity.AccountType

/**
 * Get link ui state
 *
 * @property key
 * @property linkWithoutKey
 * @property password
 * @property linkWithPassword
 * @property accountType
 * @property isBusinessAccountExpired
 */
data class GetLinkUiState(
    val key: String = "",
    val linkWithoutKey: String = "",
    val password: String? = null,
    val linkWithPassword: String? = null,
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
)