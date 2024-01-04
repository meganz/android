package mega.privacy.android.app.presentation.meeting.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.meeting.CallType
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
 * @property meetingLink                                Meeting link.
 * @property shouldShareMeetingLink                     True, if should share link. False if not.
 * @property title                                      Title
 * @property chatParticipantSelected                    [ChatParticipant] selected
 * @property isSpeakerMode                              True, if it's speaker mode on. False, if not.
 * @property selectParticipantEvent                     Select [ChatParticipant] event.
 * @property removeParticipantDialog                    True, if should show remove participant dialog. False, if not.
 * @property shouldPinToSpeakerView                     True, if should change to speaker view. False, if not.
 * @property chatIdToOpen                               Chat Id of the chat that should be opened.
 * @property callType                                   [CallType]
 * @property isParticipantSharingScreen                 True, if a participant is sharing the screen. False, if not.
 * @property isSessionOnRecording                       True if a host is recording or False otherwise.
 * @property showRecordingConsentDialog                 True if should show the recording consent dialog or False otherwise.
 * @property isRecordingConsentAccepted                 True if recording consent dialog has been already accepted or False otherwise.
 * @property startOrStopRecordingParticipantName        Name of the [Participant] who has started/stopped the recording.
 * @property isNecessaryToUpdateCall                    True, it is necessary to update call. False, it's not necessary.
 * @property isScheduledMeeting                         True, if it is a scheduled meeting. False, if not.
 * @property myFullName                                 My full name
 * @property chatScheduledMeeting                       [ChatScheduledMeeting]
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
    val meetingLink: String = "",
    val shouldShareMeetingLink: Boolean = false,
    val title: String = "",
    val chatParticipantSelected: ChatParticipant? = null,
    val isSpeakerMode: Boolean = false,
    val selectParticipantEvent: StateEvent = consumed,
    val removeParticipantDialog: Boolean = false,
    val shouldPinToSpeakerView: Boolean = false,
    val chatIdToOpen: Long = -1L,
    val callType: CallType = CallType.OneToOne,
    val isParticipantSharingScreen: Boolean = false,
    val isSessionOnRecording: Boolean = false,
    val showRecordingConsentDialog: Boolean = false,
    val isRecordingConsentAccepted: Boolean = false,
    val startOrStopRecordingParticipantName: String? = null,
    val isNecessaryToUpdateCall: Boolean = false,
    val isScheduledMeeting: Boolean = false,
    val myFullName: String = "",
    val chatScheduledMeeting: ChatScheduledMeeting? = null
) {
    /**
     * Check if waiting room is opened
     */
    fun isWaitingRoomOpened() =
        participantsSection == ParticipantsSection.WaitingRoomSection && (shouldWaitingRoomListBeShown || isBottomPanelExpanded)
}