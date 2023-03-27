package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.ChatRoom
import nz.mega.sdk.MegaChatRoom
import javax.inject.Inject

/**
 * Mapper to convert [MegaChatRoom] to [ChatRoom]
 */
internal class ChatRoomMapper @Inject constructor(
    private val chatPermissionsMapper: ChatPermissionsMapper,
    private val chatRoomChangesMapper: ChatRoomChangesMapper,
) {
    operator fun invoke(megaChatRoom: MegaChatRoom): ChatRoom = ChatRoom(
        megaChatRoom.chatId,
        chatRoomChangesMapper(megaChatRoom.changes),
        megaChatRoom.title,
        megaChatRoom.hasCustomTitle(),
        chatPermissionsMapper(megaChatRoom.ownPrivilege),
        megaChatRoom.isGroup,
        megaChatRoom.isPublic,
        megaChatRoom.isPreview,
        megaChatRoom.isArchived,
        megaChatRoom.isActive,
        megaChatRoom.retentionTime,
        megaChatRoom.isMeeting,
        megaChatRoom.isWaitingRoom,
        megaChatRoom.isOpenInvite,
        megaChatRoom.isSpeakRequest,
    )
}