package mega.privacy.android.app.activities.settingsActivities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.chat.SetLastGreenVisibleUseCase
import mega.privacy.android.domain.usecase.chat.SetPresenceAutoAwayUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ChatPreferencesViewModel
 */
@HiltViewModel
class ChatPreferencesViewModel @Inject constructor(
    monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val setPresenceAutoAwayUseCase: SetPresenceAutoAwayUseCase,
    private val setLastGreenVisibleUseCase: SetLastGreenVisibleUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatPreferencesState())

    /**
     * UI State ChatPreference
     * Flow of [ChatPreferencesState]
     */
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monitorUpdatePushNotificationSettingsUseCase().collect {
                _state.update { state ->
                    state.copy(isPushNotificationSettingsUpdatedEvent = true)
                }
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
     * Enables the auto-away presence option with the given timeout.
     *
     * @param timeoutSeconds Seconds to wait before turning away.
     */
    fun setAutoAwayPresence(timeoutSeconds: Int) {
        viewModelScope.launch {
            runCatching { setPresenceAutoAwayUseCase(true, timeoutSeconds) }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Enables or disables the visibility of the last time the user was online.
     *
     * @param enabled True to enable. False to disable.
     */
    fun setLastGreenVisible(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { setLastGreenVisibleUseCase(enabled) }
                .onFailure { Timber.e(it) }
        }
    }
}
