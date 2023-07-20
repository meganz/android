package mega.privacy.android.app.getLink

/**
 * Get link ui state
 *
 * @property key
 * @property linkWithoutKey
 * @property password
 * @property linkWithPassword
 */
data class GetLinkUiState(
    val key: String = "",
    val linkWithoutKey: String = "",
    val password: String? = null,
    val linkWithPassword: String? = null
)