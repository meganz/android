package mega.privacy.android.app.presentation.chat.mapper

import mega.privacy.android.app.presentation.chat.model.ChatRoomUiState
import mega.privacy.android.domain.entity.chat.ChatRoom
import javax.inject.Inject

/**
 * Map the [ChatRoom] domain entity to [ChatRoomUiState]
 */
class ChatRoomUiMapper @Inject constructor() {

    /**
     * Invocation method
     *
     * @param chatRoom The [ChatRoom] domain entity that needs to be mapped
     * @return The mapped [ChatRoom]
     */
    operator fun invoke(chatRoom: ChatRoom) = ChatRoomUiState(
        chatId = chatRoom.chatId,
        ownPrivilege = chatRoom.ownPrivilege,
        numPreviewers = chatRoom.numPreviewers,
        peerPrivilegesByHandles = chatRoom.peerPrivilegesByHandles,
        peerCount = chatRoom.peerCount,
        peerHandlesList = chatRoom.peerHandlesList,
        peerPrivilegesList = chatRoom.peerPrivilegesList,
        isGroup = chatRoom.isGroup,
        isPublic = chatRoom.isPublic,
        isPreview = chatRoom.isPreview,
        authorizationToken = chatRoom.authorizationToken,
        title = chatRoom.title,
        hasCustomTitle = chatRoom.hasCustomTitle,
        unreadCount = chatRoom.unreadCount,
        userTyping = chatRoom.userTyping,
        userHandle = chatRoom.userHandle,
        isActive = chatRoom.isActive,
        isArchived = chatRoom.isArchived,
        retentionTime = chatRoom.retentionTime,
        creationTime = chatRoom.creationTime,
        isMeeting = chatRoom.isMeeting,
        isWaitingRoom = chatRoom.isWaitingRoom,
        isOpenInvite = chatRoom.isOpenInvite,
        isSpeakRequest = chatRoom.isSpeakRequest,
        changes = chatRoom.changes
    )
}
