package mega.privacy.android.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.entity.preference.ViewModePreference
import mega.privacy.android.domain.usecase.setting.MonitorSortingPreferenceUseCase
import mega.privacy.android.domain.usecase.setting.MonitorViewModePreferenceUseCase
import mega.privacy.android.domain.usecase.setting.SetSortingPreferenceUseCase
import mega.privacy.android.domain.usecase.setting.SetViewModePreferenceUseCase
import mega.privacy.android.feature.settings.presentation.model.SortingAndViewModeSettingsUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the Sorting and view mode settings screen.
 */
@HiltViewModel
class SortingAndViewModeSettingsViewModel @Inject constructor(
    private val monitorSortingPreferenceUseCase: MonitorSortingPreferenceUseCase,
    private val setSortingPreferenceUseCase: SetSortingPreferenceUseCase,
    private val monitorViewModePreferenceUseCase: MonitorViewModePreferenceUseCase,
    private val setViewModePreferenceUseCase: SetViewModePreferenceUseCase,
) : ViewModel() {

    /**
     * UI state for the screen. Starts as [SortingAndViewModeSettingsUiState.Loading] and emits
     * [SortingAndViewModeSettingsUiState.Data] once the current preferences are available.
     */
    val uiState: StateFlow<SortingAndViewModeSettingsUiState> by lazy {
        combine(
            monitorSortingPreferenceUseCase(),
            monitorViewModePreferenceUseCase(),
        ) { sortingPreference, viewModePreference ->
            SortingAndViewModeSettingsUiState.Data(
                sortingPreference = sortingPreference,
                viewModePreference = viewModePreference,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, SortingAndViewModeSettingsUiState.Loading)
    }

    /**
     * Set the sorting preference.
     */
    fun setSortingPreference(preference: SortingPreference) {
        viewModelScope.launch {
            runCatching { setSortingPreferenceUseCase(preference) }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Set the view mode preference.
     */
    fun setViewModePreference(preference: ViewModePreference) {
        viewModelScope.launch {
            runCatching { setViewModePreferenceUseCase(preference) }.onFailure { Timber.e(it) }
        }
    }
}
