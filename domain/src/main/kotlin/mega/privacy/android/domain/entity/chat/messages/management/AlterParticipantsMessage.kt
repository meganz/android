package mega.privacy.android.domain.entity.chat.messages.management

import mega.privacy.android.domain.entity.ChatRoomPermission

/**
 * Alter participants message
 * @property privilege The privilege of the participant
 * @property handleOfAction The handle of the participant
 */
data class AlterParticipantsMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    val privilege: ChatRoomPermission,
    val handleOfAction: Long,
) : ManagementMessage