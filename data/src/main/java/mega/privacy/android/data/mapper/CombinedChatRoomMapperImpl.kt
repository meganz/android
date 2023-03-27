package mega.privacy.android.data.mapper

import mega.privacy.android.data.mapper.chat.ChatRoomChangesMapper
import mega.privacy.android.data.mapper.chat.LastMessageTypeMapper
import mega.privacy.android.data.mapper.chat.ChatPermissionsMapper
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatRoom
import javax.inject.Inject

/**
 * Combined chat room mapper implementation
 */
internal class CombinedChatRoomMapperImpl @Inject constructor(
    private val lastMessageTypeMapper: LastMessageTypeMapper,
    private val chatRoomChangesMapper: ChatRoomChangesMapper,
    private val chatPermissionsMapper: ChatPermissionsMapper
) : CombinedChatRoomMapper {

    override fun invoke(
        megaChatRoom: MegaChatRoom,
        megaChatListItem: MegaChatListItem,
    ): CombinedChatRoom =
        CombinedChatRoom(
            megaChatRoom.chatId,
            chatRoomChangesMapper(megaChatRoom.changes),
            megaChatRoom.title,
            megaChatRoom.hasCustomTitle(),
            chatPermissionsMapper(megaChatRoom.ownPrivilege),
            megaChatListItem.unreadCount,
            megaChatListItem.lastMessage,
            megaChatListItem.lastMessageId,
            lastMessageTypeMapper(megaChatListItem.lastMessageType),
            megaChatListItem.lastMessageSender,
            megaChatListItem.lastTimestamp,
            megaChatRoom.peerCount,
            megaChatRoom.isGroup,
            megaChatRoom.isPublic,
            megaChatRoom.isPreview,
            megaChatRoom.isArchived,
            megaChatRoom.isActive,
            megaChatListItem.isDeleted,
            megaChatListItem.isCallInProgress,
            megaChatListItem.peerHandle,
            megaChatListItem.lastMessagePriv,
            megaChatListItem.lastMessageHandle,
            megaChatListItem.numPreviewers,
            megaChatRoom.retentionTime,
            megaChatRoom.isMeeting,
            megaChatRoom.isWaitingRoom,
            megaChatRoom.isOpenInvite,
            megaChatRoom.isSpeakRequest,
        )
}
