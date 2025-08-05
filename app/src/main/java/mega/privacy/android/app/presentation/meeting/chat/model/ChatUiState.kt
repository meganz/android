package mega.privacy.android.app.presentation.meeting.chat.model

import androidx.compose.runtime.Composable
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.call.CallCompositionChanges
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction

/**
 * @property chat [ChatRoom]
 * @property isChatNotificationMute whether notification is mute
 * @property userChatStatus User chat status if is a 1to1 conversation, null otherwise.
 * @property userLastGreen User chat last green if is a 1to1 conversation and if chat status is different than online, null otherwise.
 * @property isJoining True if the current logged in user is joining this chat, false otherwise.
 * @property isLeaving True if the current logged in user is leaving this chat, false otherwise.
 * @property callsInOtherChats [ChatCall] List of calls in other chats which are not finished yet. Empty otherwise.
 * @property callInThisChat [ChatCall] if the current logged in user has a call in this chat, null otherwise.
 * @property storageState [StorageState] of the chat.
 * @property isConnected True if current chat is connected.
 * @property schedIsPending True, if scheduled meeting is pending. False, if not.
 * @property scheduledMeeting  [ChatScheduledMeeting]
 * @property usersTyping list of user typing in the chat
 * @property hasAnyContact True if the current logged in user has any contact, false otherwise.
 * @property customSubtitleList List of names for building a custom subtitle if the title is custom too, null otherwise.
 * @property participantsCount Number of participants if the chat is a group, null otherwise.
 * @property allContactsParticipateInChat True if all contacts participate in this chat, false otherwise.
 * @property infoToShowEvent Event to show some info. Set it to null in case the activity needs to be closed.
 * @property sendingText Text that is being sent.
 * @property isStartingCall True if it is starting a call, false otherwise.
 * @property chatHistoryLoadStatus [ChatHistoryLoadStatus]. Until this is not [ChatHistoryLoadStatus.NONE], we can request for more messages.
 * @property openWaitingRoomScreen True if should open waiting room screen, false otherwise.
 * @property isGeolocationEnabled True if geolocation internal permission (not device one) is granted, false otherwise.
 * @property isAnonymousMode True if the chat is in anonymous mode, false otherwise.
 * @property chatLink String with the chat link.
 * @property editingMessageId Id of the message being edited.
 * @property editingMessageContent Content of the message being edited.
 * @property myUserHandle User handle of current logged in user.
 * @property downloadEvent Event to start a download.
 * @property actionToManageEvent [ActionToManage].
 * @property callEndedDueToFreePlanLimits  State event to show the force free plan limit participants dialog.
 * @property shouldUpgradeToProPlan State event to show the upgrade to Pro plan dialog.
 * @property isCallUnlimitedProPlanFeatureFlagEnabled   True, if Call Unlimited Pro Plan feature flag enabled. False, otherwise.
 * @property usersCallLimitReminders   [UsersCallLimitReminders]
 * @property isSelectMode Indicates if the chat is in select mode.
 * @property selectedMessages Set of selected messages.
 * @property selectedReaction The selected reaction.
 * @property reactionList List of reactions.
 * @property pendingAction The pending action.
 * @property addingReactionTo The id of the message to which a reaction is being added.
 * @property gmsDocumentScanner The prepared ML Kit Document Scanner
 * @property documentScanningError The specific Error returned when using the modern Document Scanner
 */
data class ChatUiState(
    val chat: ChatRoom? = null,
    val isChatNotificationMute: Boolean = false,
    val userChatStatus: UserChatStatus? = null,
    val userLastGreen: Int? = null,
    val isJoining: Boolean = false,
    val isLeaving: Boolean = false,
    val callsInOtherChats: List<ChatCall> = emptyList(),
    val callInThisChat: ChatCall? = null,
    val storageState: StorageState = StorageState.Unknown,
    val isConnected: Boolean = false,
    val schedIsPending: Boolean = false,
    val scheduledMeeting: ChatScheduledMeeting? = null,
    val usersTyping: List<String?> = emptyList(),
    val hasAnyContact: Boolean = false,
    val customSubtitleList: List<String>? = null,
    val participantsCount: Long? = null,
    val allContactsParticipateInChat: Boolean = false,
    val infoToShowEvent: StateEventWithContent<InfoToShow?> = consumed(),
    val sendingText: String = "",
    val isStartingCall: Boolean = false,
    val chatHistoryLoadStatus: ChatHistoryLoadStatus? = null,
    val openWaitingRoomScreen: Boolean = false,
    val isGeolocationEnabled: Boolean = false,
    val isAnonymousMode: Boolean = false,
    val chatLink: String? = null,
    val editingMessageId: Long? = null,
    val editingMessageContent: String? = null,
    val myUserHandle: Long? = null,
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val actionToManageEvent: StateEventWithContent<ActionToManage> = consumed(),
    val callEndedDueToFreePlanLimits: Boolean = false,
    val shouldUpgradeToProPlan: Boolean = false,
    val isCallUnlimitedProPlanFeatureFlagEnabled: Boolean = false,
    val usersCallLimitReminders: UsersCallLimitReminders = UsersCallLimitReminders.Enabled,
    val isSelectMode: Boolean = false,
    val selectedMessages: Set<TypedMessage> = emptySet(),
    val selectedReaction: String = "",
    val reactionList: List<UIReaction> = emptyList(),
    val pendingAction: (@Composable () -> Unit)? = null,
    val addingReactionTo: Long? = null,
    val gmsDocumentScanner: StateEventWithContent<GmsDocumentScanner> = consumed(),
    val documentScanningError: DocumentScanningError? = null,
) {

    /**
     * Chat id.
     */
    val chatId = chat?.chatId ?: -1L

    /**
     * Chat title.
     */
    val title = chat?.title

    /**
     * Is note to self chat
     */
    val isNoteToSelf = chat?.isNoteToSelf == true

    /**
     * True if the chat is private, false otherwise.
     */
    val isPrivateChat = chat?.isPublic == false

    /**
     * [ChatRoomPermission] of the current logged in user.
     */
    val myPermission = chat?.ownPrivilege ?: ChatRoomPermission.Unknown

    /**
     * True if the current logged in user has moderator permission, false otherwise.
     */
    val haveWritePermission = chat?.ownPrivilege == ChatRoomPermission.Standard
            || chat?.ownPrivilege == ChatRoomPermission.Moderator

    /**
     * True if the current chat is opened from a link and the current logged in user is not participating.
     */
    val isPreviewMode = chat?.isPreview == true

    /**
     * Number or previewers if the current chat is opened from a link and the current logged in user is not participating.
     */
    val numPreviewers = chat?.numPreviewers ?: 0

    /**
     * True if the current logged in user is joining or leaving this chat, false otherwise.
     */
    val isJoiningOrLeaving = isJoining || isLeaving

    /**
     * True if has a call in this chat, false otherwise.
     */
    val hasACallInThisChat = callInThisChat != null

    /**
     * True if is a chat group, false otherwise.
     */
    val isGroup = chat?.isGroup == true

    /**
     * True if the group is open for invitation other than moderators, false otherwise.
     */
    val isOpenInvite = chat?.isOpenInvite == true

    /**
     *  True if currently a member of the chatroom (for group chats), or we are contacts with the peer (for 1on1 chats), false otherwise.
     */
    val isActive = chat?.isActive == true

    /**
     * True if the chat is archived, false otherwise.
     */
    val isArchived = chat?.isArchived == true

    /**
     * True if the chat is a meeting, false otherwise.
     */
    val isMeeting = chat?.isMeeting == true

    /**
     * True if the chat is a waiting room, false otherwise.
     */
    val isWaitingRoom = chat?.isWaitingRoom == true

    /**
     * Check if I'm participating in the call with another client
     */
    val isParticipatingWithAnotherClient = myUserHandle?.let {
        callInThisChat?.peerIdParticipants?.contains(it) == true && callInThisChat.status == ChatCallStatus.UserNoPresent
    }

    /**
     * Check if the one-to-one call is ending
     */
    val isOneToOneCallBeingEnded = chat?.isOneToOneChat == true &&
            callInThisChat?.changes?.contains(ChatCallChanges.CallComposition) == true &&
            callInThisChat.callCompositionChange == CallCompositionChanges.Removed &&
            (callInThisChat.peerIdCallCompositionChange == myUserHandle || callInThisChat.numParticipants == 0)
}
