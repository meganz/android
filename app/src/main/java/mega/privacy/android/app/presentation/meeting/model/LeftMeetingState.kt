package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.presentation.meeting.LeftMeetingViewModel

/**
 * Data class defining the state of [LeftMeetingViewModel]
 *
 * @property callEndedDueToFreePlanLimits               State event to show the force free plan limit participants dialog.
 */
data class LeftMeetingState(
    val callEndedDueToFreePlanLimits: Boolean = false,
)
