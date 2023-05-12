package mega.privacy.android.app.presentation.settings.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.settings.calls.model.SettingsCallsState
import mega.privacy.android.domain.entity.CallsMeetingInvitations
import mega.privacy.android.domain.entity.CallsMeetingReminders
import mega.privacy.android.domain.entity.CallsSoundNotifications
import mega.privacy.android.domain.entity.statistics.DisableSoundNotification
import mega.privacy.android.domain.entity.statistics.EnableSoundNotification
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.GetCallsMeetingInvitationsUseCase
import mega.privacy.android.domain.usecase.meeting.GetCallsMeetingRemindersUseCase
import mega.privacy.android.domain.usecase.meeting.GetCallsSoundNotifications
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.SetCallsMeetingInvitationsUseCase
import mega.privacy.android.domain.usecase.meeting.SetCallsMeetingRemindersUseCase
import mega.privacy.android.domain.usecase.meeting.SetCallsSoundNotifications
import javax.inject.Inject

/**
 * View model for [SettingsCallsFragment].
 */
@HiltViewModel
class SettingsCallsViewModel @Inject constructor(
    private val getCallsSoundNotifications: GetCallsSoundNotifications,
    private val setCallsSoundNotifications: SetCallsSoundNotifications,
    private val getCallsMeetingInvitations: GetCallsMeetingInvitationsUseCase,
    private val setCallsMeetingInvitations: SetCallsMeetingInvitationsUseCase,
    private val getCallsMeetingReminders: GetCallsMeetingRemindersUseCase,
    private val setCallsMeetingReminders: SetCallsMeetingRemindersUseCase,
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase,
    private val getFeatureFlagValue: GetFeatureFlagValueUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsCallsState())

    /**
     * Get Settings Calls state
     *
     * @return  SettingsCallsState
     */
    val uiState: StateFlow<SettingsCallsState> = _uiState

    init {
        viewModelScope.launch {
            getFeatureFlagValue(AppFeatures.MeetingNotificationSettings).let { flag ->
                _uiState.update { it.copy(meetingNotificationEnabled = flag) }
            }
        }
        viewModelScope.launch {
            getCallsSoundNotifications().collectLatest { result ->
                _uiState.update { it.copy(soundNotifications = result) }
            }
        }
        viewModelScope.launch {
            getCallsMeetingInvitations().collectLatest { result ->
                _uiState.update { it.copy(callsMeetingInvitations = result) }
            }
        }
        viewModelScope.launch {
            getCallsMeetingReminders().collectLatest { result ->
                _uiState.update { it.copy(callsMeetingReminders = result) }
            }
        }
    }

    /**
     * Enable or disable sound notifications.
     *
     * @param enable
     */
    fun setNewCallsSoundNotifications(enable: Boolean) {
        viewModelScope.launch {
            runCatching {
                if (enable) {
                    sendStatisticsMeetingsUseCase(EnableSoundNotification())
                    setCallsSoundNotifications(CallsSoundNotifications.Enabled)
                } else {
                    sendStatisticsMeetingsUseCase(DisableSoundNotification())
                    setCallsSoundNotifications(CallsSoundNotifications.Disabled)
                }
            }
        }
    }

    /**
     * Set calls meeting invitations
     *
     * @param enable
     */
    fun setNewCallsMeetingInvitations(enable: Boolean) {
        viewModelScope.launch {
            setCallsMeetingInvitations(
                if (enable) {
                    CallsMeetingInvitations.Enabled
                } else {
                    CallsMeetingInvitations.Disabled
                }
            )
        }
    }

    /**
     * Set calls meeting reminders
     *
     * @param enable
     */
    fun setNewCallsMeetingReminders(enable: Boolean) {
        viewModelScope.launch {
            setCallsMeetingReminders(
                if (enable) {
                    CallsMeetingReminders.Enabled
                } else {
                    CallsMeetingReminders.Disabled
                }
            )
        }
    }
}
