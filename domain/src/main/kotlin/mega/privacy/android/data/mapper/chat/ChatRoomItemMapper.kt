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
                    isPublic = chatRoom.isPublic,
                    chatId = chatRoom.chatId,
                    title = chatRoom.title,
                    isLastMessageVoiceClip = isLastMessageVoiceClip,
                    unreadCount = chatRoom.unreadCount,
                    hasPermissions = hasPermissions,
                    isActive = chatRoom.isActive,
                    isArchived = chatRoom.isArchived,
                    lastTimestamp = chatRoom.lastTimestamp,
                    highlight = highLight,
                )
            }

            chatRoom.isGroup -> {
                ChatRoomItem.GroupChatRoomItem(
                    isPublic = chatRoom.isPublic,
                    chatId = chatRoom.chatId,
                    title = chatRoom.title,
                    isLastMessageVoiceClip = isLastMessageVoiceClip,
                    unreadCount = chatRoom.unreadCount,
                    hasPermissions = hasPermissions,
                    isActive = chatRoom.isActive,
                    isArchived = chatRoom.isArchived,
                    lastTimestamp = chatRoom.lastTimestamp,
                    highlight = highLight,
                )
            }

            else -> {
                ChatRoomItem.IndividualChatRoomItem(
                    peerHandle = chatRoom.peerHandle,
                    chatId = chatRoom.chatId,
                    title = chatRoom.title,
                    isLastMessageVoiceClip = isLastMessageVoiceClip,
                    unreadCount = chatRoom.unreadCount,
                    hasPermissions = hasPermissions,
                    isActive = chatRoom.isActive,
                    isArchived = chatRoom.isArchived,
                    lastTimestamp = chatRoom.lastTimestamp,
                    highlight = highLight,
                )
            }
        }
    }
}