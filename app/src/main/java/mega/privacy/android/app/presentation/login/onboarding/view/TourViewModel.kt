package mega.privacy.android.app.presentation.login.onboarding.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.login.onboarding.model.TourUiState
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import javax.inject.Inject

/**
 * The [ViewModel] class for [mega.privacy.android.app.presentation.login.onboarding.view.NewTourScreen].
 */
@HiltViewModel
class TourViewModel @Inject constructor(
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TourUiState())
    internal val uiState = _uiState.asStateFlow()

    init {
        setupThemeMonitoring()
    }

    private fun setupThemeMonitoring() {
        viewModelScope.launch {
            monitorThemeModeUseCase().collectLatest { themeMode ->
                _uiState.update { state -> state.copy(themeMode = themeMode) }
            }
        }
    }
}
