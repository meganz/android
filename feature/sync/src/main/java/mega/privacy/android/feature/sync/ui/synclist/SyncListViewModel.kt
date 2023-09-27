package mega.privacy.android.feature.sync.ui.synclist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.feature.sync.domain.usecase.SetOnboardingShownUseCase
import javax.inject.Inject

@HiltViewModel
internal class SyncListViewModel @Inject constructor(
    private val setOnboardingShownUseCase: SetOnboardingShownUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncListState())
    val state: StateFlow<SyncListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            setOnboardingShownUseCase(true)
        }
        setMockStalledIssuesCount()
    }

    private fun setMockStalledIssuesCount() {
        _state.value = _state.value.copy(
            stalledIssuesCount = 3
        )
    }
}