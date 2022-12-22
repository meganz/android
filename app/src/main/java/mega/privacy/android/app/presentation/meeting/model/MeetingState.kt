package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel

/**
 * Data class defining the state of [MeetingActivityViewModel]
 *
 * @property isMeetingEnded   True, if the meeting is ended. False, otherwise.
 */
data class MeetingState(
    val isMeetingEnded: Boolean? = null,
)