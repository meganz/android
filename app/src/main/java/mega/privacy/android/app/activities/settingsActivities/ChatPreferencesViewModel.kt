package mega.privacy.android.app.activities.settingsActivities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.MonitorChatSignalPresenceUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import javax.inject.Inject

/**
 * ChatPreferencesViewModel
 */
@HiltViewModel
class ChatPreferencesViewModel @Inject constructor(
    monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    monitorChatSignalPresenceUseCase: MonitorChatSignalPresenceUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatPreferencesState())

    /**
     * UI State ChatPreference
     * Flow of [ChatPreferencesState]
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            merge(
                monitorUpdatePushNotificationSettingsUseCase().map {
                    { state: ChatPreferencesState ->
                        state.copy(
                            isPushNotificationSettingsUpdatedEvent = true
                        )
                    }
                },
                monitorChatSignalPresenceUseCase().map {
                    { state: ChatPreferencesState ->
                        state.copy(
                            signalPresenceUpdate = true
                        )
                    }
                }
            ).collect {
                _state.update(it)
            }
        }
    }

    /**
     * on Consume Push notification settings updated event
     */
    fun onConsumePushNotificationSettingsUpdateEvent() {
        viewModelScope.launch {
            _state.update { it.copy(isPushNotificationSettingsUpdatedEvent = false) }
        }
    }

    /**
     * Signal presence update consumed.
     */
    fun onSignalPresenceUpdateConsumed() {
        viewModelScope.launch {
            _state.update { it.copy(signalPresenceUpdate = false) }
        }
    }
}