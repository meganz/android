package mega.privacy.android.feature.chat.meeting.left.model

import androidx.compose.runtime.Immutable

/**
 * State of the screen a guest lands on after leaving a meeting, which is entirely the notice it
 * raises about why the call ended. The meeting screen derives both reasons from a single call term
 * code, so at most one of them applies to any given call.
 */
@Immutable
sealed interface LeftMeetingUiState {

    /**
     * The call ended without a limit being hit, so the screen only invites the guest to create an
     * account.
     */
    data object None : LeftMeetingUiState

    /**
     * The call ended because the organiser's free plan caps it at 100 participants, which the
     * screen reports in a dialog the guest has to acknowledge.
     */
    data object FreePlanParticipantsLimit : LeftMeetingUiState

    /**
     * The call could not be joined because it already held the maximum number of participants,
     * which the screen reports in a snackbar.
     */
    data object TooManyParticipants : LeftMeetingUiState
}
