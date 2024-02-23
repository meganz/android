package mega.privacy.android.domain.entity.chat.messages

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import java.io.File

/**
 * Node attachment message
 * @param file
 */
data class PendingAttachmentMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isDeletable: Boolean,
    override val isEditable: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val fileType: FileTypeInfo,
    override val reactions: List<Reaction>,
    override val status: ChatMessageStatus,
    val file: File,
) : AttachmentMessage {
    override val isMine = true
    override val fileName = file.name ?: ""
    override val fileSize = file.length()
    override val duration = (fileType as? VideoFileTypeInfo)?.duration
}
