package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.contacts.ContactItem

/**
 * Chat participant.
 *
 * @property participantId  Participant identifier.
 * @property privilege      [ChatRoomPermission] of the participant in the chat room
 * @property nonContact     [NoContactParticipant] The participant is not my contact
 * @property contact        [ContactItem] The participant is my contact
 */
data class ChatParticipant(
    val participantId: Long,
    val privilege: ChatRoomPermission,
    val nonContact: NoContactParticipant? = null,
    val contact: ContactItem? = null,
)