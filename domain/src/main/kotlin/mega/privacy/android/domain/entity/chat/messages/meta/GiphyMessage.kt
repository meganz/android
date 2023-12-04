package mega.privacy.android.domain.entity.chat.messages.meta

/**
 * Giphy message
 */
data class GiphyMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
) : MetaMessage