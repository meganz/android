package mega.privacy.android.app.presentation.meeting.chat.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.usecase.meeting.LoadMessagesUseCase.Companion.NUMBER_MESSAGES_TO_LOAD

/**
 * Chat ui state
 *
 * @property chatId ID of the chat
 * @property title title of the chat
 * @property isChatNotificationMute whether notification is mute
 * @property isPrivateChat whether the chat is private
 * @property userChatStatus User chat status if is a 1to1 conversation, null otherwise.
 * @property userLastGreen User chat last green if is a 1to1 conversation and if chat status is different than online, null otherwise.
 * @property myPermission [ChatRoomPermission] of the current logged in user.
 * @property isPreviewMode True if the current logged in user is in a chat link in preview mode (not participating).
 * @property isJoiningOrLeaving True if the current logged in user is joining or leaving this chat, false otherwise.
 * @property callInOtherChat [ChatCall] if I am already participating in a call in other chat. Null otherwise.
 * @property callInThisChat [ChatCall] if the current logged in user has a call in this chat, null otherwise.
 * @property isGroup True if is a chat group, false otherwise.
 * @property storageState [StorageState] of the chat.
 * @property isConnected True if current chat is connected.
 * @property schedIsPending True, if scheduled meeting is pending. False, if not.
 * @property scheduledMeeting  [ChatScheduledMeeting]
 * @property isOpenInvite True if the group is open for invitation other than moderators, false otherwise.
 * @property isActive True if currently a member of the chatroom (for group chats), or we are contacts with the peer (for 1on1 chats), false otherwise.
 * @property isArchived True if the chat is archived, false otherwise.
 * @property usersTyping list of user typing in the chat
 * @property isMeeting whether this chat is a meeting.
 * @property hasAnyContact True if the current logged in user has any contact, false otherwise.
 * @property customSubtitleList List of names for building a custom subtitle if the title is custom too, null otherwise.
 * @property participantsCount Number of participants if the chat is a group, null otherwise.
 * @property allContactsParticipateInChat True if all contacts participate in this chat, false otherwise.
 * @property isWaitingRoom True if the scheduled meeting has the waiting room setting enabled, false otherwise.
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
 * @property numPreviewers Number of previewers in the chat.
 */
data class ChatUiState(
    val chatId: Long = -1L,
    val title: String? = null,
    val isChatNotificationMute: Boolean = false,
    val isPrivateChat: Boolean? = null,
    val userChatStatus: UserChatStatus? = null,
    val userLastGreen: Int? = null,
    val myPermission: ChatRoomPermission = ChatRoomPermission.Unknown,
    val isPreviewMode: Boolean = false,
    val isJoiningOrLeaving: Boolean = false,
    val callInOtherChat: ChatCall? = null,
    val callInThisChat: ChatCall? = null,
    val isGroup: Boolean = false,
    val storageState: StorageState = StorageState.Unknown,
    val isConnected: Boolean = false,
    val schedIsPending: Boolean = false,
    val scheduledMeeting: ChatScheduledMeeting? = null,
    val isOpenInvite: Boolean = false,
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val usersTyping: List<String?> = emptyList(),
    val isMeeting: Boolean = false,
    val hasAnyContact: Boolean = false,
    val customSubtitleList: List<String>? = null,
    val participantsCount: Long? = null,
    val allContactsParticipateInChat: Boolean = false,
    val isWaitingRoom: Boolean = false,
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
    val numPreviewers: Long = 0,
) {

    /**
     * Has a call in this chat.
     */
    val hasACallInThisChat = callInThisChat != null
}