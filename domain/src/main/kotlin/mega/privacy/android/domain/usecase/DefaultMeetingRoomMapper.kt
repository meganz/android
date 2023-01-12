package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import javax.inject.Inject
import kotlin.reflect.KSuspendFunction1

/**
 * Default meeting item room mapper
 */
class DefaultMeetingRoomMapper @Inject constructor() : MeetingRoomMapper {
    override suspend fun invoke(
        chatRoom: CombinedChatRoom,
        isChatNotifiable: KSuspendFunction1<Long, Boolean>,
        isChatLastMessageGeolocation: KSuspendFunction1<Long, Boolean>,
    ): MeetingRoomItem {
        val isMuted = !isChatNotifiable(chatRoom.chatId)
        val hasPermissions = chatRoom.ownPrivilege == ChatRoomPermission.Moderator
        val highLight = chatRoom.unreadCount > 0 || chatRoom.isCallInProgress
                || chatRoom.lastMessageType == ChatRoomLastMessage.CallStarted
        val isLastMessageVoiceClip =
            chatRoom.lastMessageType == ChatRoomLastMessage.VoiceClip
        val isLastMessageGeolocation = isChatLastMessageGeolocation(chatRoom.chatId)

        return MeetingRoomItem(
            chatId = chatRoom.chatId,
            title = chatRoom.title,
            isLastMessageVoiceClip = isLastMessageVoiceClip,
            isLastMessageGeolocation = isLastMessageGeolocation,
            unreadCount = chatRoom.unreadCount,
            isMuted = isMuted,
            isActive = chatRoom.isActive,
            isPublic = chatRoom.isPublic,
            hasPermissions = hasPermissions,
            highlight = highLight,
            lastTimestamp = chatRoom.lastTimestamp,
        )
    }
}
