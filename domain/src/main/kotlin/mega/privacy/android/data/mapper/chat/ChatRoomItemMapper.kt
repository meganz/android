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

        val highLight = chatRoom.unreadCount > 0
                || chatRoom.isCallInProgress
                || chatRoom.lastMessageType == ChatRoomLastMessage.CallStarted

        return when {
            chatRoom.isMeeting -> {
                ChatRoomItem.MeetingChatRoomItem(
                    isPublic = chatRoom.isPublic,
                    chatId = chatRoom.chatId,
                    title = chatRoom.title,
                    lastMessage = chatRoom.lastMessage,
                    lastMessageType = chatRoom.lastMessageType,
                    unreadCount = chatRoom.unreadCount,
                    hasPermissions = hasPermissions,
                    isWaitingRoom = chatRoom.isWaitingRoom,
                    isActive = chatRoom.isActive,
                    isArchived = chatRoom.isArchived,
                    lastTimestamp = chatRoom.lastTimestamp,
                    highlight = highLight,
                    peers = chatRoom.peers
                )
            }

            chatRoom.isGroup -> {
                ChatRoomItem.GroupChatRoomItem(
                    isPublic = chatRoom.isPublic,
                    chatId = chatRoom.chatId,
                    title = chatRoom.title,
                    lastMessage = chatRoom.lastMessage,
                    lastMessageType = chatRoom.lastMessageType,
                    unreadCount = chatRoom.unreadCount,
                    hasPermissions = hasPermissions,
                    isActive = chatRoom.isActive,
                    isArchived = chatRoom.isArchived,
                    lastTimestamp = chatRoom.lastTimestamp,
                    highlight = highLight,
                    peers = chatRoom.peers
                )
            }

            chatRoom.isNoteToSelf -> {
                ChatRoomItem.NoteToSelfChatRoomItem(
                    peerHandle = chatRoom.peerHandle,
                    chatId = chatRoom.chatId,
                    title = chatRoom.title,
                    lastMessage = chatRoom.lastMessage,
                    lastMessageType = chatRoom.lastMessageType,
                    hasPermissions = hasPermissions,
                    isActive = chatRoom.isActive,
                    isArchived = chatRoom.isArchived,
                    lastTimestamp = chatRoom.lastTimestamp,
                    highlight = highLight,
                    peers = chatRoom.peers
                )
            }

            else -> {
                ChatRoomItem.IndividualChatRoomItem(
                    peerHandle = chatRoom.peerHandle,
                    chatId = chatRoom.chatId,
                    title = chatRoom.title,
                    lastMessage = chatRoom.lastMessage,
                    lastMessageType = chatRoom.lastMessageType,
                    unreadCount = chatRoom.unreadCount,
                    hasPermissions = hasPermissions,
                    isActive = chatRoom.isActive,
                    isArchived = chatRoom.isArchived,
                    lastTimestamp = chatRoom.lastTimestamp,
                    highlight = highLight,
                    peers = chatRoom.peers
                )
            }
        }
    }
}
