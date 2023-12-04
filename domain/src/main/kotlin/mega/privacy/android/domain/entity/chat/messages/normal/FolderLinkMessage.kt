package mega.privacy.android.domain.entity.chat.messages.normal

/**
 * Folder link message
 */
data class FolderLinkMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
) : NormalMessage