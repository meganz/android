package mega.privacy.android.domain.entity.chat.messages.normal

/**
 * Contact link message
 * @property contactLink Contact link
 * @property content Link
 */
data class ContactLinkMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val tempId: Long,
    val contactLink: String,
    val content: String,
) : NormalMessage