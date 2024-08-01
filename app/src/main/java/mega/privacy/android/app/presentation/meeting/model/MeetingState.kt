package mega.privacy.android.app.presentation.meeting.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.call.CallType
import mega.privacy.android.domain.entity.meeting.ParticipantsSection

/**
 * Data class defining the state of [MeetingActivityViewModel]
 *
 * @property chatId                                     Chat Id
 * @property isMeetingEnded                             True, if the meeting is ended. False, otherwise.
 * @property shouldLaunchLeftMeetingActivity            True when user should be navigated to login page
 * @property hasHostPermission                          True, host permissions. False, other permissions.
 * @property myPermission                               My [ChatRoomPermission]
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
 * @property isNecessaryToUpdateCall                    True, it is necessary to update call. False, it's not necessary.
 * @property isScheduledMeeting                         True, if it is a scheduled meeting. False, if not.
 * @property myFullName                                 My full name
 * @property chatScheduledMeeting                       [ChatScheduledMeeting]
 * @property isRingingAll                               True if is ringing for all participants or False otherwise.
 * @property newInvitedParticipants                     List of emails of the new invited participants.
 * @property snackbarMsg                                State to show snackbar message
 * @property subscriptionPlan                           [AccountType]
 * @property guestFirstName                             Guest first name
 * @property guestLastName                              Guest last name
 * @property meetingName                                Meeting name
 * @property isCallUnlimitedProPlanFeatureFlagEnabled   True, if Call Unlimited Pro Plan feature flag enabled. False, otherwise.
 * @property callEndedDueToFreePlanLimits               State event to show the force free plan limit participants dialog.
 * @property action                                     Meeting action type
 * @property currentCall                                [ChatCall]
 * @property myUserHandle                               My user handle
 * @property userToShowInHandRaisedSnackbar             User identifiers with changes in the raised hand that should be shown in the snackbar.
 * @property shouldParticipantInCallListBeShown         True, it must be shown. False, must be hidden
 * @property handRaisedSnackbarMsg                      Message to show in Snackbar.
 * @property isRaiseToSpeakFeatureFlagEnabled           True, if Raise to speak feature flag enabled. False, otherwise.
 * @property isWaitingForGroupHandRaisedSnackbars       Waiting for group hand raised snackbars.
 * @property showLowerHandButtonInSnackbar              True, show lower hand button. False, show view button.
 * @property isPictureInPictureFeatureFlagEnabled       True, if Picture in Picture feature flag enabled. False, otherwise.
 * @property isInPipMode                                True, if is in Picture in Picture mode. False, otherwise.
 * @property startedMeetingChatId                       Chat id of the meeting started
 */
data class MeetingState(
    val chatId: Long = -1L,
    val isMeetingEnded: Boolean? = null,
    val shouldLaunchLeftMeetingActivity: Boolean = false,
    val myPermission: ChatRoomPermission = ChatRoomPermission.Unknown,
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
    val isNecessaryToUpdateCall: Boolean = false,
    val isScheduledMeeting: Boolean = false,
    val myFullName: String = "",
    val chatScheduledMeeting: ChatScheduledMeeting? = null,
    val isRingingAll: Boolean = false,
    val newInvitedParticipants: List<String> = emptyList(),
    val snackbarMsg: StateEventWithContent<String> = consumed(),
    val subscriptionPlan: AccountType = AccountType.UNKNOWN,
    val guestFirstName: String = "",
    val guestLastName: String = "",
    val meetingName: String = "",
    val isCallUnlimitedProPlanFeatureFlagEnabled: Boolean = false,
    val callEndedDueToFreePlanLimits: Boolean = false,
    val action: String? = null,
    val currentCall: ChatCall? = null,
    val myUserHandle: Long? = null,
    val shouldParticipantInCallListBeShown: Boolean = false,
    val handRaisedSnackbarMsg: StateEventWithContent<String> = consumed(),
    val isRaiseToSpeakFeatureFlagEnabled: Boolean = false,
    val userToShowInHandRaisedSnackbar: Map<Long, Boolean> = emptyMap(),
    val isWaitingForGroupHandRaisedSnackbars: Boolean = false,
    val showLowerHandButtonInSnackbar: Boolean = false,
    val isPictureInPictureFeatureFlagEnabled: Boolean = false,
    val isInPipMode: Boolean = false,
    val callEndedDueToTooManyParticipants: Boolean = false,
    val startedMeetingChatId: Long? = null,
) {

    /**
     * Check if waiting room is opened
     */
    fun isWaitingRoomOpened() =
        participantsSection == ParticipantsSection.WaitingRoomSection && (shouldWaitingRoomListBeShown || isBottomPanelExpanded)

    /**
     * Check if all participants are muted
     *
     * @return True, if all are muted, false if not.
     */
    fun areAllParticipantsMuted(): Boolean {
        chatParticipantsInCall.find { it.callParticipantData.isAudioOn && it.callParticipantData.clientId != -1L }
            ?.let {
                return false
            }

        return true
    }

    /**
     * Monitor if is my hand raised to speak
     */
    val isMyHandRaisedToSpeak
        get():Boolean = myUserHandle?.let {
            currentCall?.usersRaiseHands?.get(
                it
            )
        } ?: false

    /**
     * Check if is my hand raised to show snackbar
     */
    val isMyHandRaisedToShowSnackbar
        get():Boolean = myUserHandle?.let {
            userToShowInHandRaisedSnackbar.contains(it) && userToShowInHandRaisedSnackbar[it] == true
        } ?: false


    /**
     * User ID with hand raised
     */
    fun getParticipantNameWithRaisedHand(): String {
        userToShowInHandRaisedSnackbar.filter { it.value && it.key != myUserHandle }.let { list ->
            list.entries.first().let { first ->
                usersInCall.first { it.peerId == first.key }.apply {
                    return name
                }
            }
        }
    }

    /**
     * Check if has host permission
     */
    fun hasHostPermission() = myPermission == ChatRoomPermission.Moderator

    /**
     * Check if Mute all item should be shown
     */
    fun shouldMuteAllItemBeShown() =
        hasHostPermission() && participantsSection == ParticipantsSection.InCallSection

    /**
     * Check if Admit all item should be shown
     */
    fun shouldAdmitAllItemBeShown() =
        usersInWaitingRoomIDs.isNotEmpty() && participantsSection == ParticipantsSection.WaitingRoomSection

    /**
     * Check if Call all item should be shown
     */
    fun shouldCallAllItemBeShown() =
        myPermission > ChatRoomPermission.ReadOnly && participantsSection == ParticipantsSection.NotInCallSection

    /**
     * Check if Invite participants item should be shown
     */
    fun shouldInviteParticipantsItemBeShown() =
        participantsSection == ParticipantsSection.InCallSection && !isGuest && (hasHostPermission() || isOpenInvite)

    /**
     * Check if Waiting room section should be shown
     */
    fun shouldWaitingRoomSectionBeShown() = hasWaitingRoom && hasHostPermission()

    /**
     * Number of user for show hand raised snackbar
     */
    fun userToShowInHandRaisedSnackbarNumber(): Int =
        userToShowInHandRaisedSnackbar.filter { it.value }.size

    /**
     * Check if Number of participants item should be shown
     */
    fun shouldNumberOfParticipantsItemBeShown() =
        participantsSection == ParticipantsSection.InCallSection ||
                (participantsSection == ParticipantsSection.NotInCallSection && chatParticipantsNotInCall.isNotEmpty()) ||
                (participantsSection == ParticipantsSection.WaitingRoomSection && chatParticipantsInWaitingRoom.isNotEmpty())

    /**
     * Check if the section is right
     */
    fun isRightSection(): Boolean =
        ((participantsSection == ParticipantsSection.WaitingRoomSection &&
                shouldWaitingRoomListBeShown &&
                chatParticipantsInWaitingRoom.isNotEmpty()) ||
                (participantsSection == ParticipantsSection.InCallSection &&
                        shouldInCallListBeShown &&
                        chatParticipantsInCall.isNotEmpty()) ||
                (participantsSection == ParticipantsSection.NotInCallSection &&
                        shouldNotInCallListBeShown &&
                        chatParticipantsNotInCall.isNotEmpty())
                )

    /**
     * Check if see all should be shown
     */
    val showSeeAllButton
        get() = ((participantsSection == ParticipantsSection.WaitingRoomSection && shouldWaitingRoomSectionBeShown() && chatParticipantsInWaitingRoom.size > MAX_PARTICIPANTS_IN_BOTTOM_PANEL) ||
                (participantsSection == ParticipantsSection.InCallSection && chatParticipantsInCall.size > MAX_PARTICIPANTS_IN_BOTTOM_PANEL) ||
                (participantsSection == ParticipantsSection.NotInCallSection && chatParticipantsNotInCall.size > MAX_PARTICIPANTS_IN_BOTTOM_PANEL))

    companion object {
        /**
         * Free plan participants limit
         */
        const val FREE_PLAN_PARTICIPANTS_LIMIT = 100

        /**
         * Max participants in bottom panel
         */
        const val MAX_PARTICIPANTS_IN_BOTTOM_PANEL = 4
    }
}
