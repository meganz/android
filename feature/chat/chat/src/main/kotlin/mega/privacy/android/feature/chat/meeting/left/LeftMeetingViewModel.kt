package mega.privacy.android.feature.chat.meeting.left

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.feature.chat.meeting.left.model.LeftMeetingUiState

/**
 * View model for the screen a guest lands on after leaving a meeting.
 */
@HiltViewModel(assistedFactory = LeftMeetingViewModel.Factory::class)
internal class LeftMeetingViewModel @AssistedInject constructor(
    @Assisted private val args: Args,
) : ViewModel() {

    /**
     * Factory for [LeftMeetingViewModel].
     */
    @AssistedFactory
    interface Factory {

        /**
         * @param args How the call the guest was in ended.
         */
        fun create(args: Args): LeftMeetingViewModel
    }

    /**
     * How the call the guest was in ended, as the meeting screen reported it.
     *
     * @property callEndedDueToFreePlanLimits Whether the call hit the free plan participant cap.
     * @property callEndedDueToTooManyParticipants Whether the call was already full.
     */
    data class Args(
        val callEndedDueToFreePlanLimits: Boolean,
        val callEndedDueToTooManyParticipants: Boolean,
    )

    private val noticeShown = MutableStateFlow(false)

    /**
     * UI state of the left meeting screen.
     */
    val uiState: StateFlow<LeftMeetingUiState> by lazy(LazyThreadSafetyMode.NONE) {
        noticeShown
            .map { shown -> if (shown) LeftMeetingUiState.None else callEndNotice() }
            .asUiStateFlow(viewModelScope, callEndNotice())
    }

    /**
     * Record that the call-end notice has been shown, so it is not raised again when the guest
     * rotates the device or returns to the screen.
     */
    fun onNoticeShown() {
        noticeShown.value = true
    }

    private fun callEndNotice(): LeftMeetingUiState = when {
        args.callEndedDueToFreePlanLimits -> LeftMeetingUiState.FreePlanParticipantsLimit
        args.callEndedDueToTooManyParticipants -> LeftMeetingUiState.TooManyParticipants
        else -> LeftMeetingUiState.None
    }
}
