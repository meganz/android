package mega.privacy.android.domain.entity.chat.messages.management

/**
 * Scheduled meeting created message
 */
data class ScheduledMeetingUpdatedMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
) : ManagementMessage