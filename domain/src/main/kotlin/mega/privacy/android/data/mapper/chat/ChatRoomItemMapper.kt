package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import javax.inject.Inject

/**
 * Chat room item mapper
 */
class ChatRoomItemMapper @Inject constructor() {

    /**
     * Map [CombinedChatRoom] into [ChatRoomItem]
     *
     * @param chatRoom  [CombinedChatRoom]
     * @return          [ChatRoomItem]
     */
    operator fun invoke(chatRoom: CombinedChatRoom): ChatRoomItem {
        val hasPermissions = chatRoom.ownPrivilege == ChatRoomPermission.Moderator
        val isLastMessageVoiceClip = chatRoom.lastMessageType == ChatRoomLastMessage.VoiceClip
        val highLight = chatRoom.unreadCount > 0
                || chatRoom.isCallInProgress
                || chatRoom.lastMessageType == ChatRoomLastMessage.CallStarted

        return when {
            chatRoom.isMeeting -> {
                ChatRoomItem.MeetingChatRoomItem(
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
                ChatRoomItem.GroupChatRoomItem(
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
                ChatRoomItem.IndividualChatRoomItem(
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