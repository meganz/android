package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.presentation.meeting.model.LeftMeetingState
import javax.inject.Inject

/**
 * LeftMeeting view model.
 *
 * @property state                    Current view state as [LeftMeetingViewModel]
 */
@HiltViewModel
class LeftMeetingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        LeftMeetingState(
            callEndedDueToFreePlanLimits = savedStateHandle[MeetingActivity.MEETING_FREE_PLAN_USERS_LIMIT]
                ?: false,
            callEndedDueToTooManyParticipants = savedStateHandle[MeetingActivity.MEETING_PARTICIPANTS_LIMIT]
                ?: false
        )
    )
    val state = _state.asStateFlow()

    /**
     * Consume show free plan participants limit dialog event
     *
     */
    fun onConsumeShowFreePlanParticipantsLimitDialogEvent() {
        _state.update { state -> state.copy(callEndedDueToFreePlanLimits = false) }
    }

    /**
     * Consume show participants limit snackbar event
     *
     */
    fun onConsumeShowParticipantsLimitSnackbarEvent() {
        _state.update { state -> state.copy(callEndedDueToTooManyParticipants = false) }
    }
}
