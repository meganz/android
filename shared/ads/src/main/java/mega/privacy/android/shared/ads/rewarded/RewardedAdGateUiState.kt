package mega.privacy.android.shared.ads.rewarded

/**
 * Immutable UI state for the Rewarded Ad Gate dialog.
 */
data class RewardedAdGateUiState(
    val showDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
