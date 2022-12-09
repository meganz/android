package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatMessage

/**
 * Mapper to convert [MegaChatListItem] to [ChatListItem]
 */
typealias ChatListItemMapper = (@JvmSuppressWildcards MegaChatListItem) -> @JvmSuppressWildcards ChatListItem

internal fun toChatListItem(megaChatListItem: MegaChatListItem): ChatListItem =
    ChatListItem(
        megaChatListItem.chatId,
        megaChatListItem.changes.mapListItemChanges(),
        megaChatListItem.title,
        megaChatListItem.ownPrivilege.mapChatRoomOwnPrivilege(),
        megaChatListItem.unreadCount,
        megaChatListItem.lastMessage,
        megaChatListItem.lastMessageId,
        megaChatListItem.lastMessageType.mapLastMessageType(),
        megaChatListItem.lastMessageSender,
        megaChatListItem.lastTimestamp,
        megaChatListItem.isGroup,
        megaChatListItem.isPublic,
        megaChatListItem.isPreview,
        megaChatListItem.isActive,
        megaChatListItem.isArchived,
        megaChatListItem.isDeleted,
        megaChatListItem.isCallInProgress,
        megaChatListItem.peerHandle,
        megaChatListItem.lastMessagePriv,
        megaChatListItem.lastMessageHandle,
        megaChatListItem.numPreviewers
    )

private fun Int.mapListItemChanges(): ChatListItemChanges = when (this) {
    MegaChatListItem.CHANGE_TYPE_STATUS -> ChatListItemChanges.Status
    MegaChatListItem.CHANGE_TYPE_OWN_PRIV -> ChatListItemChanges.OwnPrivilege
    MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT -> ChatListItemChanges.UnreadCount
    MegaChatListItem.CHANGE_TYPE_PARTICIPANTS -> ChatListItemChanges.Participants
    MegaChatListItem.CHANGE_TYPE_TITLE -> ChatListItemChanges.Title
    MegaChatListItem.CHANGE_TYPE_CLOSED -> ChatListItemChanges.Closed
    MegaChatListItem.CHANGE_TYPE_LAST_MSG -> ChatListItemChanges.LastMessage
    MegaChatListItem.CHANGE_TYPE_LAST_TS -> ChatListItemChanges.LastTS
    MegaChatListItem.CHANGE_TYPE_ARCHIVE -> ChatListItemChanges.Archive
    MegaChatListItem.CHANGE_TYPE_CALL -> ChatListItemChanges.Call
    MegaChatListItem.CHANGE_TYPE_CHAT_MODE -> ChatListItemChanges.ChatMode
    MegaChatListItem.CHANGE_TYPE_UPDATE_PREVIEWERS -> ChatListItemChanges.UpdatePreviewers
    MegaChatListItem.CHANGE_TYPE_PREVIEW_CLOSED -> ChatListItemChanges.PreviewClosed
    else -> ChatListItemChanges.Deleted
}

fun Int.mapLastMessageType(): ChatRoomLastMessage = when (this) {
    MegaChatMessage.TYPE_INVALID -> ChatRoomLastMessage.Invalid
    MegaChatMessage.TYPE_NORMAL -> ChatRoomLastMessage.Normal
    MegaChatMessage.TYPE_ALTER_PARTICIPANTS -> ChatRoomLastMessage.AlterParticipants
    MegaChatMessage.TYPE_TRUNCATE -> ChatRoomLastMessage.Truncate
    MegaChatMessage.TYPE_PRIV_CHANGE -> ChatRoomLastMessage.PrivChange
    MegaChatMessage.TYPE_CHAT_TITLE -> ChatRoomLastMessage.ChatTitle
    MegaChatMessage.TYPE_CALL_ENDED -> ChatRoomLastMessage.CallEnded
    MegaChatMessage.TYPE_CALL_STARTED -> ChatRoomLastMessage.CallStarted
    MegaChatMessage.TYPE_PUBLIC_HANDLE_CREATE -> ChatRoomLastMessage.PublicHandleCreate
    MegaChatMessage.TYPE_PUBLIC_HANDLE_DELETE -> ChatRoomLastMessage.PublicHandleDelete
    MegaChatMessage.TYPE_SET_PRIVATE_MODE -> ChatRoomLastMessage.SetPrivateMode
    MegaChatMessage.TYPE_SET_RETENTION_TIME -> ChatRoomLastMessage.SetRetentionTime
    MegaChatMessage.TYPE_SCHED_MEETING -> ChatRoomLastMessage.SchedMeeting
    MegaChatMessage.TYPE_NODE_ATTACHMENT -> ChatRoomLastMessage.NodeAttachment
    MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT -> ChatRoomLastMessage.RevokeNodeAttachment
    MegaChatMessage.TYPE_CONTACT_ATTACHMENT -> ChatRoomLastMessage.ContactAttachment
    MegaChatMessage.TYPE_CONTAINS_META -> ChatRoomLastMessage.ContainsMeta
    MegaChatMessage.TYPE_VOICE_CLIP -> ChatRoomLastMessage.VoiceClip
    else -> ChatRoomLastMessage.Unknown
}
