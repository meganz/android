package mega.privacy.android.feature.settings.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View model for call settings
 */
@HiltViewModel
class CallSettingsViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(CallSettingsUiState())

    /**
     * Current Ui state for call settings
     */
    val uiState = _uiState.asStateFlow()

    init {
        //should monitor the value from use case
        setSoundNotification(true)
    }

    /**
     * Set the sound notification flag
     */
    fun setSoundNotification(active: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isSoundNotificationActive = active)
            }
        }
    }
}