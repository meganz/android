package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel

/**
 * Data class defining the state of [MeetingActivityViewModel]
 *
 * @property chatId            Chat Id
 * @property isMeetingEnded   True, if the meeting is ended. False, otherwise.
 * @property shouldLaunchLeftMeetingActivity true when user should be navigated to login page
 * @property showParticipantsInWaitingRoomDialog     Show dialog when there are participants in the waiting room
 * @property usersInWaitingRoom                      User list in the waiting room
 */
data class MeetingState(
    val chatId: Long = -1L,
    val isMeetingEnded: Boolean? = null,
    val shouldLaunchLeftMeetingActivity: Boolean = false,
    val showParticipantsInWaitingRoomDialog: Boolean = false,
    val usersInWaitingRoom: Map<Long, String>? = null,
)