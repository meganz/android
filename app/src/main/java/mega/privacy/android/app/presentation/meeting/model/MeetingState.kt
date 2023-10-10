package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.meeting.ParticipantsSection

/**
 * Data class defining the state of [MeetingActivityViewModel]
 *
 * @property chatId                                     Chat Id
 * @property isMeetingEnded                             True, if the meeting is ended. False, otherwise.
 * @property shouldLaunchLeftMeetingActivity            True when user should be navigated to login page
 * @property hasHostPermission                          True, host permissions. False, other permissions.
 * @property chatParticipantsInCall                     List of [ChatParticipant] in the call.
 * @property usersInCall                                List of [Participant] in the call.
 * @property chatParticipantsNotInCall                  List of [ChatParticipant] not in the call.
 * @property chatParticipantsInWaitingRoom              List of [ChatParticipant] in the waiting room.
 * @property usersInWaitingRoomIDs                      List of user handles in the waiting room.
 * @property participantsSection                        [ParticipantsSection] opened
 * @property chatParticipantList                        List of [ChatParticipant]
 * @property isOpenInvite                               If open invite option is enabled.
 * @property enabledAllowNonHostAddParticipantsOption   True if is enabled the allow non-host participants option, false otherwise.
 * @property hasWaitingRoom                             True if has waiting room. False if not.
 * @property shouldWaitingRoomListBeShown               True if waiting room list must be shown or False otherwise (hidden).
 * @property shouldInCallListBeShown                    True if in call list must be shown or False otherwise (hidden).
 * @property shouldNotInCallListBeShown                 True if not in call list must be shown or False otherwise (hidden).
 * @property isBottomPanelExpanded                      True if bottom panel is expanded or False otherwise (collapsed).
 * @property isGuest                                    True if it's guest. False, if not.
 * @property sendMeetingLink                            True, should send the meeting link. False, if not.
 */
data class MeetingState(
    val chatId: Long = -1L,
    val isMeetingEnded: Boolean? = null,
    val shouldLaunchLeftMeetingActivity: Boolean = false,
    val hasHostPermission: Boolean = false,
    val chatParticipantsInCall: List<ChatParticipant> = emptyList(),
    val usersInCall: List<Participant> = emptyList(),
    val chatParticipantsNotInCall: List<ChatParticipant> = emptyList(),
    val chatParticipantsInWaitingRoom: List<ChatParticipant> = emptyList(),
    val usersInWaitingRoomIDs: List<Long> = emptyList(),
    val participantsSection: ParticipantsSection = ParticipantsSection.InCallSection,
    val chatParticipantList: List<ChatParticipant> = emptyList(),
    val isOpenInvite: Boolean = false,
    val enabledAllowNonHostAddParticipantsOption: Boolean = true,
    val hasWaitingRoom: Boolean = false,
    val shouldWaitingRoomListBeShown: Boolean = false,
    val shouldInCallListBeShown: Boolean = false,
    val shouldNotInCallListBeShown: Boolean = false,
    val isBottomPanelExpanded: Boolean = false,
    val isGuest: Boolean = false,
    val sendMeetingLink: Boolean = false,
) {
    /**
     * Check if waiting room is opened
     */
    fun isWaitingRoomOpened() =
        participantsSection == ParticipantsSection.WaitingRoomSection && (shouldWaitingRoomListBeShown || isBottomPanelExpanded)
}