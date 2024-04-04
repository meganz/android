package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.presentation.meeting.model.LeftMeetingState
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * LeftMeeting view model.
 *
 * @property getFeatureFlagValueUseCase Use case for getting the value of a feature flag.
 * @property state                    Current view state as [LeftMeetingViewModel]
 */
@HiltViewModel
class LeftMeetingViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        LeftMeetingState(
            callEndedDueToFreePlanLimits = savedStateHandle[MeetingActivity.MEETING_FREE_PLAN_USERS_LIMIT]
                ?: false
        )
    )
    val state: StateFlow<LeftMeetingState> = _state

    init {
        viewModelScope.launch {
            getFeatureFlagValueUseCase(AppFeatures.CallUnlimitedProPlan).let { flag ->
                _state.update { state ->
                    state.copy(
                        isCallUnlimitedProPlanFeatureFlagEnabled = flag,
                    )
                }
            }
        }
    }

    /**
     * Consume show free plan participants limit dialog event
     *
     */
    fun onConsumeShowFreePlanParticipantsLimitDialogEvent() {
        _state.update { state -> state.copy(callEndedDueToFreePlanLimits = false) }
    }
}
