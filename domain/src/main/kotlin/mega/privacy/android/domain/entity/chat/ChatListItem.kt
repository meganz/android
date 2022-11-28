package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.ChatRoomPermission

/**
 * Chat room item.
 *
 * @property chatId                 Handle identifying the chat.
 * @property changes                Changes [ChatListItemChanges].
 * @property title                  Title of the chat.
 * @property ownPrivilege           Own privilege level in this chatroom [ChatRoomPermission].
 * @property unreadCount            Num of unread messages.
 * @property lastMessage            Last message.
 * @property lastMessageId          Last message id.
 * @property lastMessageType        Last message type.
 * @property lastMessageSender      Last message sender.
 * @property lastTimestamp          Last timestamp.
 * @property isGroup                If chat room is a group chat room.
 * @property isPublic               If chat room is public.
 * @property isPreview              If chat room is in preview mode.
 * @property isActive               If chat room is active.
 * @property isArchived             If chat room is archived.
 * @property isDeleted              If chat room is deleted.
 * @property isCallInProgress       If chat room has a call in progress.
 * @property peerHandle             Peer handle.
 * @property lastMessagePriv        Last message.
 * @property lastMessageHandle      Last message user handle.
 * @property numPreviewers          Num of previewers.
 */
data class ChatListItem(
    val chatId: Long,
    val changes: ChatListItemChanges? = null,
    val title: String = "",
    val ownPrivilege: ChatRoomPermission = ChatRoomPermission.Standard,
    val unreadCount: Int = 0,
    val lastMessage: String = "",
    val lastMessageId: Long = -1,
    val lastMessageType: Int = 0,
    val lastMessageSender: Long,
    val lastTimestamp: Long,
    val isGroup: Boolean = false,
    val isPublic: Boolean = false,
    val isPreview: Boolean = false,
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val isCallInProgress: Boolean = false,
    val peerHandle: Long = -1,
    val lastMessagePriv: Int = 0,
    val lastMessageHandle: Long = -1,
    val numPreviewers: Long = -1,
)
