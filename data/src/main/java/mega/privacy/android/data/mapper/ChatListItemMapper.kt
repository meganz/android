package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatRoom

/**
 * Mapper to convert [MegaChatListItem] to [ChatListItem]
 */
typealias ChatListItemMapper = (@JvmSuppressWildcards MegaChatListItem) -> @JvmSuppressWildcards ChatListItem

internal fun toChatListItem(megaChatListItem: MegaChatListItem): ChatListItem =
    ChatListItem(
        megaChatListItem.chatId,
        mapChanges(megaChatListItem.changes),
        megaChatListItem.title,
        mapOwnPrivilege(megaChatListItem.ownPrivilege),
        megaChatListItem.unreadCount,
        megaChatListItem.lastMessage,
        megaChatListItem.lastMessageId,
        megaChatListItem.lastMessageType,
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

private fun mapOwnPrivilege(ownPrivilege: Int): ChatRoomPermission =
    when (ownPrivilege) {
        MegaChatRoom.PRIV_RM -> ChatRoomPermission.Removed
        MegaChatRoom.PRIV_RO -> ChatRoomPermission.ReadOnly
        MegaChatRoom.PRIV_STANDARD -> ChatRoomPermission.Standard
        MegaChatRoom.PRIV_MODERATOR -> ChatRoomPermission.Moderator
        else -> ChatRoomPermission.Unknown

    }

private fun mapChanges(changes: Int): ChatListItemChanges {
    return when (changes) {
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
}