package mega.privacy.android.domain.entity.chat.messages.management

/**
 * Permission change message
 */
data class PermissionChangeMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
) : ManagementMessage