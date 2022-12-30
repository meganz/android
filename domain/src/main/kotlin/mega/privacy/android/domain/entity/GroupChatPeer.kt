package mega.privacy.android.domain.entity

/**
 * Group chat peer
 *
 * @property userHandle
 * @property userPermission
 * @property email
 * @property fullName
 */
data class GroupChatPeer constructor(
    val userHandle: Long,
    val userPermission: ChatRoomPermission,
    val email: String,
    val fullName: String,
)
