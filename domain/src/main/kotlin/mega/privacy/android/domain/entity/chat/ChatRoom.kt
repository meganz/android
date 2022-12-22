package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.ChatRoomPermission

/**
 * Chat room item.
 *
 * @property chatId             Handle identifying the chat.
 * @property changes            Changes [ChatRoomChanges].
 * @property title              Title of the chat.
 * @property hasCustomTitle     Title of the chat.
 * @property ownPrivilege       Own privilege level in this chatroom [ChatRoomPermission].
 * @property isGroup            If chat room is a group chat room.
 * @property isPublic           If chat room is public.
 * @property isPreview          If chat room is in preview mode.
 * @property isArchived         If chat room is archived.
 * @property isActive           If chat room is active.
 * @property retentionTime      Retention time.
 * @property isMeeting          If chat room is a meeting.
 * @property isWaitingRoom      If chat room is a waiting room.
 * @property isOpenInvite       If open invite option in enabled.
 * @property isSpeakRequest     If speaker request in enabled.
 */
data class ChatRoom(
    val chatId: Long,
    val changes: ChatRoomChanges? = null,
    val title: String = "",
    val hasCustomTitle: Boolean = false,
    val ownPrivilege: ChatRoomPermission = ChatRoomPermission.Standard,
    val isGroup: Boolean = false,
    val isPublic: Boolean = false,
    val isPreview: Boolean = false,
    val isArchived: Boolean = false,
    val isActive: Boolean = false,
    val retentionTime: Long = -1,
    val isMeeting: Boolean = false,
    val isWaitingRoom: Boolean = false,
    val isOpenInvite: Boolean = false,
    val isSpeakRequest: Boolean = false,
)