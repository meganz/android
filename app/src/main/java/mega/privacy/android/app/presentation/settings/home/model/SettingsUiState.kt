package mega.privacy.android.app.presentation.settings.home.model

/**
 * Settings ui state
 */
sealed interface SettingsUiState {
    /**
     * Loading
     */
    data object Loading : SettingsUiState

    /**
     * Data
     *
     * @property settings
     */
    data class Data(val settings: List<SettingSection>) : SettingsUiState
}