package mega.privacy.android.feature.sharelink.presentation

/**
 * UI state for the revamped Share link screen.
 *
 * MR0 foundation stub — fleshed out in MR1 (AND-24035) with the link, key, password,
 * expiry and account-type fields backed by the existing export/password use cases.
 *
 * @property handles Node handles whose link is being shared.
 * @property isLoading Whether the screen is loading the link details.
 */
data class ShareLinkUiState(
    val handles: List<Long> = emptyList(),
    val isLoading: Boolean = true,
)
