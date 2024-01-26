package mega.privacy.android.domain.entity.chat.messages.management

/**
 * Retention time updated message
 *
 * @param retentionTime The retention time.
 */
data class RetentionTimeUpdatedMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val shouldShowDate: Boolean,
    val retentionTime: Long,
) : ManagementMessage