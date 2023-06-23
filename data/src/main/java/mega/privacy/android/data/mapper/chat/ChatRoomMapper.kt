package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.ChatRoomPermission
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
        chatId = megaChatRoom.chatId,
        ownPrivilege = chatPermissionsMapper(megaChatRoom.ownPrivilege),
        numPreviewers = megaChatRoom.numPreviewers,
        peerPrivilegesByHandles = megaChatRoom.toPeerPrivilegesByHandles(),
        peerCount = megaChatRoom.peerCount,
        peerHandlesList = megaChatRoom.toPeerHandlesList(),
        peerPrivilegesList = megaChatRoom.toPeerPrivilegesList(),
        isGroup = megaChatRoom.isGroup,
        isPublic = megaChatRoom.isPublic,
        isPreview = megaChatRoom.isPreview,
        authorizationToken = megaChatRoom.authorizationToken,
        title = megaChatRoom.title,
        hasCustomTitle = megaChatRoom.hasCustomTitle(),
        unreadCount = megaChatRoom.unreadCount,
        userTyping = megaChatRoom.userTyping,
        userHandle = megaChatRoom.userHandle,
        isActive = megaChatRoom.isActive,
        isArchived = megaChatRoom.isArchived,
        retentionTime = megaChatRoom.retentionTime,
        creationTime = megaChatRoom.creationTs,
        isMeeting = megaChatRoom.isMeeting,
        isWaitingRoom = megaChatRoom.isWaitingRoom,
        isOpenInvite = megaChatRoom.isOpenInvite,
        isSpeakRequest = megaChatRoom.isSpeakRequest,
        changes = chatRoomChangesMapper(megaChatRoom.changes),
    )

    private fun MegaChatRoom.toPeerPrivilegesByHandles(): Map<Long, ChatRoomPermission> =
        (0 until peerCount).associate { peerHandle ->
            getPeerHandle(peerHandle).let { it to chatPermissionsMapper(getPeerPrivilegeByHandle(it)) }
        }

    private fun MegaChatRoom.toPeerHandlesList(): List<Long> =
        (0 until peerCount).map { getPeerHandle(it) }

    private fun MegaChatRoom.toPeerPrivilegesList(): List<ChatRoomPermission> =
        (0 until peerCount).map { chatPermissionsMapper(getPeerPrivilege(it)) }
}