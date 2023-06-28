package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel

/**
 * Data class defining the state of [MeetingActivityViewModel]
 *
 * @property isMeetingEnded   True, if the meeting is ended. False, otherwise.
 * @property shouldLaunchLeftMeetingActivity true when user should be navigated to login page
 */
data class MeetingState(
    val isMeetingEnded: Boolean? = null,
    val shouldLaunchLeftMeetingActivity: Boolean = false,
)