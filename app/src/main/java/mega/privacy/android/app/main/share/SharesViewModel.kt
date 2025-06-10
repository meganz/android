package mega.privacy.android.app.main.share

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import javax.inject.Inject

@HiltViewModel
class SharesViewModel @Inject constructor(
    monitorThemeModeUseCase: MonitorThemeModeUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SharesUiState())
    val state = _state.asStateFlow()

    val themeMode = monitorThemeModeUseCase()

    fun onTabSelected(tab: SharesTab) {
        _state.update { it.copy(currentTab = tab) }
    }
}