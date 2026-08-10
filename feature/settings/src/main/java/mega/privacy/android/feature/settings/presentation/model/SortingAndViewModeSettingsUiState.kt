package mega.privacy.android.feature.settings.presentation.model

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.entity.preference.ViewModePreference

/**
 * UI state for the Sorting and view mode settings screen.
 */
@Stable
sealed interface SortingAndViewModeSettingsUiState {

    /**
     * Initial loading state.
     */
    data object Loading : SortingAndViewModeSettingsUiState

    /**
     * Data state.
     *
     * @property sortingPreference the current [SortingPreference]
     * @property viewModePreference the current [ViewModePreference]
     */
    data class Data(
        val sortingPreference: SortingPreference,
        val viewModePreference: ViewModePreference,
    ) : SortingAndViewModeSettingsUiState
}
