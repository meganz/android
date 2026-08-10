package mega.privacy.android.app.presentation.business.model

/**
 * UI state for the Business Expired Alert screen
 *
 * @property isProFlexiAccount Whether the account is Pro Flexi
 * @property isMasterBusinessAccount Whether the user is a master business account admin
 */
data class BusinessExpiredAlertUiState(
    val isProFlexiAccount: Boolean = false,
    val isMasterBusinessAccount: Boolean = false,
)
