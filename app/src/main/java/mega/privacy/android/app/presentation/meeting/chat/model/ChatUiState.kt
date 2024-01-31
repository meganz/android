package mega.privacy.android.app.presentation.meeting.chat.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase.Companion.NUMBER_MESSAGES_TO_LOAD

/**
 * Chat ui state
 *
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
 * @property messages List of [TypedMessage] containing the chat history.
 * @property pendingMessagesToLoad Number of messages already requested, but pending to load.
 * @property chatHistoryLoadStatus [ChatHistoryLoadStatus]. Until this is not [ChatHistoryLoadStatus.NONE], we can request for more messages.
 * @property mutePushNotificationDialogEvent Event to show the dialog to mute push notifications.
 * @property openWaitingRoomScreen True if should open waiting room screen, false otherwise.
 * @property isGeolocationEnabled True if geolocation internal permission (not device one) is granted, false otherwise.
 * @property isLoadingGalleryFiles True if gallery files are being loaded, false otherwise.
 * @property userUpdate [UserUpdate] with the changes in the user.
 * @property isAnonymousMode True if the chat is in anonymous mode, false otherwise.
 * @property chatLink String with the chat link.
 * @property editingMessageId Id of the message being edited.
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
    val messages: List<UiChatMessage> = emptyList(),
    val pendingMessagesToLoad: Int = NUMBER_MESSAGES_TO_LOAD,
    val chatHistoryLoadStatus: ChatHistoryLoadStatus? = null,
    val mutePushNotificationDialogEvent: StateEventWithContent<List<ChatPushNotificationMuteOption>> = consumed(),
    val openWaitingRoomScreen: Boolean = false,
    val isGeolocationEnabled: Boolean = false,
    val isLoadingGalleryFiles: Boolean = true,
    val userUpdate: UserUpdate? = null,
    val isAnonymousMode: Boolean = false,
    val chatLink: String? = null,
    val editingMessageId: Long? = null,
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
}