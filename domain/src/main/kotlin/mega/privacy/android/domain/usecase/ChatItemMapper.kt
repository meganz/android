package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatItem
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import javax.inject.Inject

/**
 * Chat item mapper
 */
class ChatItemMapper @Inject constructor() {

    /**
     * Map [CombinedChatRoom] into [ChatItem]
     *
     * @param chatRoom
     * @return  ChatItem
     */
    operator fun invoke(chatRoom: CombinedChatRoom): ChatItem {
        val hasPermissions = chatRoom.ownPrivilege == ChatRoomPermission.Moderator
        val isLastMessageVoiceClip = chatRoom.lastMessageType == ChatRoomLastMessage.VoiceClip
        val highLight = chatRoom.unreadCount > 0
                || chatRoom.isCallInProgress
                || chatRoom.lastMessageType == ChatRoomLastMessage.CallStarted

        return when {
            chatRoom.isMeeting -> {
                ChatItem.MeetingChatItem(
                    chatId = chatRoom.chatId,
                    title = chatRoom.title,
                    isLastMessageVoiceClip = isLastMessageVoiceClip,
                    unreadCount = chatRoom.unreadCount,
                    isActive = chatRoom.isActive,
                    isArchived = chatRoom.isArchived,
                    isPublic = chatRoom.isPublic,
                    hasPermissions = hasPermissions,
                    highlight = highLight,
                    lastTimestamp = chatRoom.lastTimestamp,
                )
            }

            chatRoom.isGroup -> {
                ChatItem.GroupChatItem(
                    chatId = chatRoom.chatId,
                    title = chatRoom.title,
                    isLastMessageVoiceClip = isLastMessageVoiceClip,
                    unreadCount = chatRoom.unreadCount,
                    isActive = chatRoom.isActive,
                    isArchived = chatRoom.isArchived,
                    isPublic = chatRoom.isPublic,
                    hasPermissions = hasPermissions,
                    highlight = highLight,
                    lastTimestamp = chatRoom.lastTimestamp,
                )
            }

            else -> {
                ChatItem.IndividualChatItem(
                    chatId = chatRoom.chatId,
                    title = chatRoom.title,
                    isLastMessageVoiceClip = isLastMessageVoiceClip,
                    unreadCount = chatRoom.unreadCount,
                    isActive = chatRoom.isActive,
                    isArchived = chatRoom.isArchived,
                    hasPermissions = hasPermissions,
                    highlight = highLight,
                    lastTimestamp = chatRoom.lastTimestamp,
                    peerHandle = chatRoom.peerHandle,
                )
            }
        }
    }
}
