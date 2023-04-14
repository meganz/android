package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.presentation.meeting.model.ScheduleMeetingState
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import javax.inject.Inject

/**
 * ScheduleMeetingActivity view model.
 * @property monitorConnectivityUseCase     [MonitorConnectivityUseCase]
 * @property state                          Current view state as [ScheduleMeetingState]
 */
@HiltViewModel
class ScheduleMeetingViewModel @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduleMeetingState())
    val state: StateFlow<ScheduleMeetingState> = _state

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivityUseCase().shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = monitorConnectivityUseCase().value

    /**
     * Enable or disable meeting link option
     */
    fun onMeetingLinkTap() {
        _state.update { state ->
            state.copy(enabledMeetingLinkOption = !state.enabledMeetingLinkOption)
        }
    }

    /**
     * Enable or disable allow non-hosts to add participants option
     */
    fun onAllowNonHostAddParticipantsTap() {
        _state.update { state ->
            state.copy(enabledAllowAddParticipantsOption = !state.enabledAllowAddParticipantsOption)
        }
    }

    /**
     * Show snackBar with a text
     *
     * @param stringId String id.
     */
    private fun showSnackBar(stringId: Int) =
        _state.update { it.copy(snackBar = stringId) }

    /**
     * Discard meeting button clicked
     */
    fun onDiscardMeetingTap() =
        _state.update { state ->
            state.copy(discardMeetingDialog = !state.discardMeetingDialog)
        }

    /**
     * Dismiss alert dialogs
     */
    fun dismissDialog() =
        _state.update { state ->
            state.copy(
                discardMeetingDialog = false,
            )
        }
}