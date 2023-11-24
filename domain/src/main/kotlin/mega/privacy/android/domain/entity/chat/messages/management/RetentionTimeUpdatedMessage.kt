package mega.privacy.android.domain.entity.chat.messages.management

/**
 * Retention time updated message
 */
data class RetentionTimeUpdatedMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
) : ManagementMessage