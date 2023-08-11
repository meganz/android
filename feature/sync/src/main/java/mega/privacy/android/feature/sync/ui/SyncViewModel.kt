package mega.privacy.android.feature.sync.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.feature.sync.domain.usecase.IsOnboardingRequiredUseCase
import javax.inject.Inject

@HiltViewModel
internal class SyncViewModel @Inject constructor(
    private val isOnboardingRequiredUseCase: IsOnboardingRequiredUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncState())

    val state: StateFlow<SyncState> = _state.asStateFlow()

    init {
        checkUserSyncs()
    }

    private fun checkUserSyncs() {
        viewModelScope.launch {
            runCatching { isOnboardingRequiredUseCase() }
                .onSuccess { isOnboardingRequired ->
                    _state.value = _state.value.copy(showOnboarding = isOnboardingRequired)
                }
        }
    }
}
