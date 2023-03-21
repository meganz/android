package mega.privacy.android.app.presentation.settings.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.settings.calls.model.SettingsCallsState
import mega.privacy.android.domain.entity.CallsSoundNotifications
import mega.privacy.android.domain.entity.statistics.DisableSoundNotification
import mega.privacy.android.domain.entity.statistics.EnableSoundNotification
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.meeting.GetCallsSoundNotifications
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.SetCallsSoundNotifications
import javax.inject.Inject

/**
 * View model for [SettingsCallsFragment].
 */
@HiltViewModel
class SettingsCallsViewModel @Inject constructor(
    private val getCallsSoundNotifications: GetCallsSoundNotifications,
    private val setCallsSoundNotifications: SetCallsSoundNotifications,
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsCallsState())

    /**
     * Get Settings Calls state
     *
     * @return  SettingsCallsState
     */
    val uiState: StateFlow<SettingsCallsState> = _uiState

    init {
        viewModelScope.launch(ioDispatcher) {
            getCallsSoundNotifications().map { result ->
                { state: SettingsCallsState -> state.copy(soundNotifications = result) }
            }.collect {
                _uiState.update(it)
            }
        }
    }

    /**
     * Enable or disable sound notifications.
     *
     * @param soundNotifications The sound notifications status.
     */
    fun setNewCallsSoundNotifications(
        soundNotifications: CallsSoundNotifications,
    ) {
        viewModelScope.launch {
            kotlin.runCatching {
                setCallsSoundNotifications(soundNotifications)
                when (soundNotifications) {
                    CallsSoundNotifications.Enabled -> sendStatisticsMeetingsUseCase(
                        EnableSoundNotification()
                    )
                    CallsSoundNotifications.Disabled -> sendStatisticsMeetingsUseCase(
                        DisableSoundNotification()
                    )
                }
            }
        }
    }

}