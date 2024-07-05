package mega.privacy.android.feature.sync.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.feature.sync.domain.usecase.IsOnboardingRequiredUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SyncViewModel @Inject constructor(
    private val isOnboardingRequiredUseCase: IsOnboardingRequiredUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SyncState())

    val state: StateFlow<SyncState> = _state.asStateFlow()

    init {
        monitorNetworkState()
    }

    private fun monitorNetworkState() {
        viewModelScope.launch {
            monitorConnectivityUseCase()
                .catch { Timber.d("SyncViewModel monitoring connectivity failed") }
                .collect { isNetworkConnected ->
                    _state.update {
                        it.copy(isNetworkConnected = isNetworkConnected)
                    }
                }
        }
    }

    // Checking onboarding is not currently required
    private fun checkUserSyncs() {
        viewModelScope.launch {
            runCatching { isOnboardingRequiredUseCase() }
                .onSuccess { isOnboardingRequired ->
                    _state.value = _state.value.copy(showOnboarding = isOnboardingRequired)
                }
        }
    }
}
