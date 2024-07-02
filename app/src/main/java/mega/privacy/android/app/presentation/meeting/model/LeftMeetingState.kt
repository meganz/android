package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.presentation.meeting.LeftMeetingViewModel

/**
 * Data class defining the state of [LeftMeetingViewModel]
 *
 * @property callEndedDueToFreePlanLimits               State event to show the force free plan limit participants dialog.
 * @property callEndedDueToTooManyParticipants          State event to show the snackbar when call ended due too many participants.
 */
data class LeftMeetingState(
    val callEndedDueToFreePlanLimits: Boolean = false,
    val callEndedDueToTooManyParticipants: Boolean = false,
)
