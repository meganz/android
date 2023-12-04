package mega.privacy.android.domain.entity.chat.messages.management

/**
 * Alter participants message
 */
data class AlterParticipantsMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
) : ManagementMessage