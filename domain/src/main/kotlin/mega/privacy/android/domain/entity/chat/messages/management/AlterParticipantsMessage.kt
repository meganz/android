package mega.privacy.android.domain.entity.chat.messages.management

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Alter participants message
 * @property privilege The privilege of the participant
 * @property handleOfAction The handle of the participant
 */
@Serializable
data class AlterParticipantsMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val reactions: List<Reaction>,
    val privilege: ChatRoomPermission,
    val handleOfAction: Long,
) : ManagementMessage