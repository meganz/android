package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.meeting.CallParticipantData

/**
 * Chat participant.
 *
 * @property handle                     Participant identifier.
 * @property data                       [ContactData] of the participant in the chat room.
 * @property callParticipantData        [CallParticipantData] of the participant in the call.
 * @property email                      Participant email.
 * @property isMe                       True, if it's me. False, if not.
 * @property privilege                  Participant privilege.
 * @property defaultAvatarColor         User default avatar color.
 * @property areCredentialsVerified     True if user credentials are verified, false otherwise.
 * @property status                     [UserChatStatus].
 * @property lastSeen                   User last seen.
 * @property avatarUpdateTimestamp      Timestamp for last avatar file update
 * @property privilegesUpdated          Check if privilege are updated.
 * @property isContact                  True, if it's my contact. False, if not.
 */
data class ChatParticipant(
    val handle: Long,
    val data: ContactData,
    val email: String? = null,
    val isMe: Boolean,
    val privilege: ChatRoomPermission,
    val defaultAvatarColor: Int,
    val areCredentialsVerified: Boolean = false,
    val status: UserChatStatus = UserChatStatus.Invalid,
    val lastSeen: Int? = null,
    val avatarUpdateTimestamp: Long? = null,
    val privilegesUpdated: Boolean = false,
    val callParticipantData: CallParticipantData = CallParticipantData(),
    val isContact: Boolean = false,
)