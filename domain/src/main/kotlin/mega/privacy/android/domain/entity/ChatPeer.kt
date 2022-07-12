package mega.privacy.android.domain.entity

/**
 * Chat peer.
 *
 * @property userHandle
 * @property userPrivilege
 */
data class ChatPeer(
    val userHandle: Long,
    val userPrivilege: Int,
)