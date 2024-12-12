package mega.privacy.android.feature.chat.settings.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.CallsSoundEnabledState
import mega.privacy.android.domain.usecase.call.MonitorCallSoundEnabledUseCase
import mega.privacy.android.domain.usecase.call.SetCallsSoundEnabledStateUseCase
import mega.privacy.android.feature.chat.settings.calls.model.CallSettingsUiState
import javax.inject.Inject

/**
 * View model for call settings
 */
@HiltViewModel
class CallSettingsViewModel @Inject constructor(
    private val monitorCallSoundEnabledUseCase: MonitorCallSoundEnabledUseCase,
    private val setCallSoundEnabledUseCase: SetCallsSoundEnabledStateUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CallSettingsUiState())

    /**
     * Current Ui state for call settings
     */
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                monitorCallSoundEnabledUseCase()
                    .collectLatest { callSoundState ->
                        _uiState.update {
                            it.copy(isSoundNotificationActive = callSoundState == CallsSoundEnabledState.Enabled)
                        }
                    }
            }
        }
    }

    /**
     * Set the sound notification flag
     */
    fun setSoundNotification(active: Boolean) {
        viewModelScope.launch {
            setCallSoundEnabledUseCase(
                if (active) CallsSoundEnabledState.Enabled else CallsSoundEnabledState.Disabled
            )
        }
    }
}