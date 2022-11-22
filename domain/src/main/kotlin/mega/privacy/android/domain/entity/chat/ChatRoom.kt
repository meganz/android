package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.ChatRoomPermission

/**
 * Chat room item.
 *
 * @property chatId             Handle identifying the chat.
 * @property changes            Changes [ChatRoomChanges].
 * @property title              Title of the chat.
 * @property ownPrivilege       Own privilege level in this chatroom [ChatRoomPermission].
 * @property isGroup            If chat room is a group chat room.
 * @property isMeeting          If chat room is a meeting.
 * @property isPublic           If chat room is public.
 * @property isPreview          If chat room is in preview mode.
 * @property isArchived         If chat room is archived.
 * @property isOpenInvite       If open invite option in enabled.

 */
data class ChatRoom(
    val chatId: Long,
    val changes: ChatRoomChanges? = null,
    val title: String = "",
    val ownPrivilege: ChatRoomPermission = ChatRoomPermission.Standard,
    val isGroup: Boolean,
    val isMeeting: Boolean,
    val isPublic: Boolean,
    val isPreview: Boolean,
    val isArchived: Boolean,
    val isOpenInvite: Boolean,
)