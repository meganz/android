package mega.privacy.android.domain.entity.chat.messages.meta

/**
 * Location message
 */
data class LocationMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
) : MetaMessage