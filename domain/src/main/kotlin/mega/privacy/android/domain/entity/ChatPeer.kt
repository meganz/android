package mega.privacy.android.domain.entity

/**
 * Chat peer.
 *
 * @property userHandle
 * @property userPermission
 */
data class ChatPeer(
    val userHandle: Long,
    val userPermission: ChatRoomPermission,
)