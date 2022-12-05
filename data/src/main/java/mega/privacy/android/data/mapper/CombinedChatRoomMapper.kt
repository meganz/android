package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatRoom

/**
 * Mapper to convert [MegaChatRoom] and [MegaChatListItem] to [CombinedChatRoom]
 */
typealias CombinedChatRoomMapper = (@JvmSuppressWildcards MegaChatRoom, @JvmSuppressWildcards MegaChatListItem) -> @JvmSuppressWildcards CombinedChatRoom

internal fun toCombinedChatRoom(
    megaChatRoom: MegaChatRoom,
    megaChatListItem: MegaChatListItem,
): CombinedChatRoom =
    CombinedChatRoom(
        megaChatRoom.chatId,
        megaChatRoom.changes.mapChatRoomChanges(),
        megaChatRoom.title,
        megaChatRoom.hasCustomTitle(),
        megaChatRoom.ownPrivilege.mapChatRoomOwnPrivilege(),
        megaChatListItem.unreadCount,
        megaChatListItem.lastMessage,
        megaChatListItem.lastMessageId,
        megaChatListItem.lastMessageType.mapLastMessageType(),
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
