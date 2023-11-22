package mega.privacy.android.app.presentation.meeting.model

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
 * @property showPoorConnectionBanner               True, should show poor connection banner. False, if not.
 * @property showReconnectingBanner                 True, should show reconnecting banner. False, if not.
 * @property showOnlyMeBanner                       True, should show only me banner. False, if not.
 * @property showWaitingForOthersBanner             True, should show waiting for others banner. False, if not.
 * @property showEndMeetingAsModeratorBottomPanel   True, should show end meeting as moderator bottom panel. False, if not.
 * @property showAssignModeratorBottomPanel         True, should show assign moderator bottom panel. False, if not.

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
    val chatTitle:String = " ",
    val isPublicChat:Boolean = false,
    val updateCallSubtitle: SubtitleCallType = SubtitleCallType.Connecting,
    val updateAnotherCallBannerType: AnotherCallType = AnotherCallType.NotCall,
    val anotherChatTitle: String = " ",
    val updateModeratorsName: String = " ",
    val updateNumParticipants: Int = 1,
    val isOneToOneCall: Boolean = false,
    val showMeetingInfoFragment: Boolean = false,
)