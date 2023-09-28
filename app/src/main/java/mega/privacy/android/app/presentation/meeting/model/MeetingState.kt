package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.meeting.ParticipantsSection

/**
 * Data class defining the state of [MeetingActivityViewModel]
 *
 * @property chatId                                     Chat Id
 * @property isMeetingEnded                             True, if the meeting is ended. False, otherwise.
 * @property shouldLaunchLeftMeetingActivity            True when user should be navigated to login page
 * @property hasHostPermission                          True, host permissions. False, other permissions.
 * @property usersInCallList                            List of [ChatParticipant] in the call.
 * @property usersNotInCallList                         List of [ChatParticipant] not in the call.
 * @property chatParticipantsInWaitingRoom              List of [ChatParticipant] in the waiting room.
 * @property participantsSection                        [ParticipantsSection] opened
 * @property isOpenInvite                               If open invite option is enabled.
 * @property isWaitingRoomFeatureFlagEnabled            True, if waiting room feature flag is enabled. False, if not.
 */
data class MeetingState(
    val chatId: Long = -1L,
    val isMeetingEnded: Boolean? = null,
    val shouldLaunchLeftMeetingActivity: Boolean = false,
    val hasHostPermission: Boolean = false,
    val usersInCallList: List<ChatParticipant> = emptyList(),
    val usersNotInCallList: List<ChatParticipant> = emptyList(),
    val chatParticipantsInWaitingRoom: List<ChatParticipant> = emptyList(),
    val participantsSection: ParticipantsSection = ParticipantsSection.InCallSection,
    val isOpenInvite: Boolean = false,
    val isWaitingRoomFeatureFlagEnabled: Boolean = false,
)