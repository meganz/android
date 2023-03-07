package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission

/**
 * Chat room item.
 *
 * @property chatId             Handle identifying the chat.
 * @property changes            Changes [ChatRoomChanges].
 * @property title              Title of the chat.
 * @property hasCustomTitle     Title of the chat.
 * @property ownPrivilege       Own privilege level in this chatroom [ChatRoomPermission].
 * @property unreadCount        Num of unread messages.
 * @property lastMessage        Last message.
 * @property lastMessageId      Last message id.
 * @property lastMessageType    Last message type.
 * @property lastMessageSender  Last message sender.
 * @property lastTimestamp      Last timestamp.
 * @property peerCount          Number of peers in a chat group.
 * @property isGroup            If chat room is a group chat room.
 * @property isPublic           If chat room is public.
 * @property isPreview          If chat room is in preview mode.
 * @property isArchived         If chat room is archived.
 * @property isActive           If chat room is active.
 * @property isDeleted          If chat room is deleted.
 * @property isCallInProgress   If chat room has a call in progress.
 * @property peerHandle         Peer handle.
 * @property lastMessagePriv    Last message.
 * @property lastMessageHandle  Last message user handle.
 * @property numPreviewers      Num of previewers.
 * @property retentionTime      Retention time.
 * @property isMeeting          If chat room is a meeting.
 * @property isWaitingRoom      If chat room is a waiting room.
 * @property isOpenInvite       If open invite option in enabled.
 * @property isSpeakRequest     If speaker request in enabled.
 */
data class CombinedChatRoom constructor(
    val chatId: Long,
    val changes: ChatRoomChanges? = null,
    val title: String = "",
    val hasCustomTitle: Boolean = false,
    val ownPrivilege: ChatRoomPermission = ChatRoomPermission.Standard,
    val unreadCount: Int = 0,
    val lastMessage: String = "",
    val lastMessageId: Long = -1,
    val lastMessageType: ChatRoomLastMessage = ChatRoomLastMessage.Unknown,
    val lastMessageSender: Long = -1,
    val lastTimestamp: Long = -1,
    val peerCount: Long = 0,
    val isGroup: Boolean = false,
    val isPublic: Boolean = false,
    val isPreview: Boolean = false,
    val isArchived: Boolean = false,
    val isActive: Boolean = false,
    val isDeleted: Boolean = false,
    val isCallInProgress: Boolean = false,
    val peerHandle: Long = -1,
    val lastMessagePriv: Int = 0,
    val lastMessageHandle: Long = -1,
    val numPreviewers: Long = -1,
    val retentionTime: Long = -1,
    val isMeeting: Boolean = false,
    val isWaitingRoom: Boolean = false,
    val isOpenInvite: Boolean = false,
    val isSpeakRequest: Boolean = false,
)
