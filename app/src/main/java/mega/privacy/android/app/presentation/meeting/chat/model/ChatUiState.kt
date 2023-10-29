package mega.privacy.android.app.presentation.meeting.chat.model

import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.contacts.UserChatStatus

/**
 * Chat ui state
 *
 * @property title title of the chat
 * @property isChatNotificationMute whether notification is mute
 * @property isPrivateChat whether the chat is private
 * @property userChatStatus User chat status if is a 1to1 conversation, null otherwise.
 * @property userLastGreen User chat last green if is a 1to1 conversation and if chat status is different than online, null otherwise.
 * @property myPermission [ChatRoomPermission] of the current logged in user.
 * @property isPreviewMode True if the current logged in user is in a chat link in preview mode (not participating).
 * @property isJoiningOrLeaving True if the current logged in user is joining or leaving this chat, false otherwise.
 * @property isParticipatingInACall True if the current logged in user is participating in a call, false otherwise.
 * @property hasACallInThisChat True if the current logged in user has a call in this chat, false otherwise.
 * @property isGroup True if is a chat group, false otherwise.
 * @property storageState [StorageState] of the chat.
 * @property isOpenInvite True if the group is open for invitation other than moderators, false otherwise.
 * @property isActive True if currently a member of the chatroom (for group chats), or we are contacts with the peer (for 1on1 chats), false otherwise.
 * @property isArchived True if the chat is archived, false otherwise.
 * @property usersTyping list of user typing in the chat
 */
data class ChatUiState(
    val title: String? = null,
    val isChatNotificationMute: Boolean = false,
    val isPrivateChat: Boolean? = null,
    val userChatStatus: UserChatStatus? = null,
    val userLastGreen: Int? = null,
    val myPermission: ChatRoomPermission = ChatRoomPermission.Unknown,
    val isPreviewMode: Boolean = false,
    val isJoiningOrLeaving: Boolean = false,
    val isParticipatingInACall: Boolean = false,
    val hasACallInThisChat: Boolean = false,
    val isGroup: Boolean = false,
    val storageState: StorageState = StorageState.Unknown,
    val isOpenInvite: Boolean = false,
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val usersTyping: List<String?> = emptyList()
)