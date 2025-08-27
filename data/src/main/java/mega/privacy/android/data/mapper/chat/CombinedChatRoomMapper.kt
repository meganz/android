package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatRoom
import javax.inject.Inject
import kotlin.math.abs

/**
 * Combined chat room mapper
 */
internal class CombinedChatRoomMapper @Inject constructor(
    private val chatPermissionsMapper: ChatPermissionsMapper,
    private val lastMessageTypeMapper: LastMessageTypeMapper,
    private val chatRoomChangesMapper: ChatRoomChangesMapper,
) {
    operator fun invoke(
        megaChatRoom: MegaChatRoom,
        megaChatListItem: MegaChatListItem,
    ): CombinedChatRoom = CombinedChatRoom(
        chatId = megaChatRoom.chatId,
        changes = chatRoomChangesMapper(megaChatRoom.changes),
        title = megaChatRoom.title,
        hasCustomTitle = megaChatRoom.hasCustomTitle(),
        ownPrivilege = chatPermissionsMapper(megaChatRoom.ownPrivilege),
        unreadCount = getFormattedUnreadCount(megaChatListItem.unreadCount),
        lastMessage = megaChatListItem.lastMessage,
        lastMessageId = megaChatListItem.lastMessageId,
        lastMessageType = lastMessageTypeMapper(megaChatListItem.lastMessageType),
        lastMessageSender = megaChatListItem.lastMessageSender,
        lastTimestamp = megaChatListItem.lastTimestamp,
        peerCount = megaChatRoom.peerCount,
        isGroup = megaChatRoom.isGroup,
        isPublic = megaChatRoom.isPublic,
        isPreview = megaChatRoom.isPreview,
        isArchived = megaChatRoom.isArchived,
        isActive = megaChatRoom.isActive,
        isDeleted = megaChatListItem.isDeleted,
        isCallInProgress = megaChatListItem.isCallInProgress,
        peerHandle = megaChatListItem.peerHandle,
        lastMessagePriv = megaChatListItem.lastMessagePriv,
        lastMessageHandle = megaChatListItem.lastMessageHandle,
        numPreviewers = megaChatListItem.numPreviewers,
        retentionTime = megaChatRoom.retentionTime,
        isMeeting = megaChatRoom.isMeeting,
        isWaitingRoom = megaChatRoom.isWaitingRoom,
        isOpenInvite = megaChatRoom.isOpenInvite,
        isSpeakRequest = megaChatRoom.isSpeakRequest,
        isNoteToSelf = megaChatRoom.isNoteToSelf
    )

    /**
     * Returns the absolute value of the unread count because it can be negative. A negative value doesn't
     * mean that there are no unread chats, in fact it's the opposite. Here are the details from
     * the SDK:
     *  - If the returned value is 0, then the indicator should be removed.
     *  - If the returned value is > 0, the indicator should show the exact count.
     *  - If the returned value is < 0, then there are at least that count unread messages,
     * and possibly more. In that case the indicator should show e.g. '2+'
     */
    private fun getFormattedUnreadCount(unreadCount: Int) = abs(unreadCount)
}
