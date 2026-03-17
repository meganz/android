package mega.privacy.mobile.home.presentation.home.widget.chips.model

/**
 * UI state for the Home chips widget.
 *
 * @property isMediaRevampPhase2Enabled whether the isMediaRevampPhase2Enabled is enabled
 * @property isAudiosChipVisible whether the audios chip is visible
 */
data class HomeChipsUiState(
    val isMediaRevampPhase2Enabled: Boolean = false,
    val isAudiosChipVisible: Boolean = false,
)
