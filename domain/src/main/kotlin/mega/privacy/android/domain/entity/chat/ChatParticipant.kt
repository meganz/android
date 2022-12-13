package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserStatus

/**
 * Chat participant.
 *
 * @property handle                     Participant identifier.
 * @property data                       [ContactData] of the participant in the chat room.
 * @property email                      Participant email.
 * @property isMe                       True, if it's me. False, if not.
 * @property privilege                  Participant privilege.
 * @property defaultAvatarColor         User default avatar color.
 * @property areCredentialsVerified     True if user credentials are verified, false otherwise.
 * @property status                     [UserStatus].
 * @property lastSeen                   User last seen.
 */
data class ChatParticipant(
    val handle: Long,
    val data: ContactData,
    val email: String,
    val isMe: Boolean,
    val privilege: ChatRoomPermission,
    val defaultAvatarColor: String? = "AVATAR_PRIMARY_COLOR",
    val areCredentialsVerified: Boolean = false,
    val status: UserStatus,
    val lastSeen: Int? = null,
)