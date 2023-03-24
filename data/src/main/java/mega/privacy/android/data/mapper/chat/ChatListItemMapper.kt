package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.ChatListItem
import nz.mega.sdk.MegaChatListItem
import javax.inject.Inject

/**
 * Mapper to convert [MegaChatListItem] to [ChatListItem]
 */
internal class ChatListItemMapper @Inject constructor(
    private val chatPermissionsMapper: ChatPermissionsMapper,
    private val lastMessageTypeMapper: LastMessageTypeMapper,
    private val chatListItemChangesMapper: ChatListItemChangesMapper,
) {
    operator fun invoke(megaChatListItem: MegaChatListItem): ChatListItem = ChatListItem(
        megaChatListItem.chatId,
        chatListItemChangesMapper(megaChatListItem.changes),
        megaChatListItem.title,
        chatPermissionsMapper(megaChatListItem.ownPrivilege),
        megaChatListItem.unreadCount,
        megaChatListItem.lastMessage,
        megaChatListItem.lastMessageId,
        lastMessageTypeMapper(megaChatListItem.lastMessageType),
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
}