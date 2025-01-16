package mega.privacy.android.app.presentation.settings.compose.home.model

import kotlinx.collections.immutable.ImmutableList

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
    data class Data(val settings: ImmutableList<SettingListItem>) : SettingsUiState
}