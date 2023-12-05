package mega.privacy.android.app.presentation.meeting.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.AnotherCallType
import mega.privacy.android.domain.entity.meeting.CallUIStatusType
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.SubtitleCallType

/**
 * In meeting UI state
 *
 * @property error                                  String resource id for showing an error.
 * @property isOpenInvite                           True if it's enabled, false if not.
 * @property callUIStatus                           [CallUIStatusType]
 * @property participantsInCall                     List of [Participant]
 * @property call                                   [ChatCall]
 * @property currentChatId                          Chat Id
 * @property previousState                          [ChatCallStatus]
 * @property isSpeakerSelectionAutomatic            True, if is speaker selection automatic. False, if it's manual.
 * @property haveConnection                         True, have connection. False, if not.
 * @property showCallDuration                       True, should show call duration. False, if not.
 * @property isPublicChat                           True if it's public chat. False, if not
 * @property chatTitle                              Chat title
 * @property updateCallSubtitle                     If should update the call subtitle
 * @property updateAnotherCallBannerType            Update the banner of another call.
 * @property anotherChatTitle                       Chat title of another call.
 * @property updateModeratorsName                   Update moderator's name
 * @property updateNumParticipants                  Update the num of participants
 * @property isOneToOneCall                         True, if it's one to one call. False, if it's a group call or a meeting.
 * @property showMeetingInfoFragment                True to show meeting info fragment or False otherwise
 * @property snackbarMessage                        Message to show in Snackbar.
 * @property addScreensSharedParticipantsList       List of [Participant] to add the screen shared in the carousel
 * @property removeScreensSharedParticipantsList    List of [Participant] to remove the screen shared in the carousel
 * @property isMeeting                              True if it's meetings. False, if not.
 */
data class InMeetingUiState(
    val error: Int? = null,
    val isOpenInvite: Boolean? = null,
    var callUIStatus: CallUIStatusType = CallUIStatusType.None,
    val participantsInCall: List<Participant> = emptyList(),
    val call: ChatCall? = null,
    val currentChatId: Long = -1L,
    val previousState: ChatCallStatus = ChatCallStatus.Initial,
    val isSpeakerSelectionAutomatic: Boolean = true,
    val haveConnection: Boolean = false,
    val showCallDuration: Boolean = false,
    val chatTitle: String = " ",
    val isPublicChat: Boolean = false,
    val updateCallSubtitle: SubtitleCallType = SubtitleCallType.Connecting,
    val updateAnotherCallBannerType: AnotherCallType = AnotherCallType.NotCall,
    val anotherChatTitle: String = " ",
    val updateModeratorsName: String = " ",
    val updateNumParticipants: Int = 1,
    val isOneToOneCall: Boolean = true,
    val showMeetingInfoFragment: Boolean = false,
    val snackbarMessage: StateEventWithContent<Int> = consumed(),
    val addScreensSharedParticipantsList: List<Participant>? = null,
    val removeScreensSharedParticipantsList: List<Participant>? = null,
    val isMeeting: Boolean = false,
)