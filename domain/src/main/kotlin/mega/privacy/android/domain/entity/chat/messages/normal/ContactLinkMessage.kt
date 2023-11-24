package mega.privacy.android.domain.entity.chat.messages.normal

/**
 * Contact link message
 */
data class ContactLinkMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
) : NormalMessage