package mega.privacy.android.domain.entity.chat.messages

import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.PlayableFileTypeInfo
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import java.io.File

/**
 * Pending attachment message
 * @property file
 * @property isError
 */
sealed interface PendingAttachmentMessage : AttachmentMessage {
    val file: File?
    val isError: Boolean
    override val isMine get() = true
    override val fileName get() = file?.name ?: ""
    override val fileSize get() = file?.length() ?: 0L
    override val duration get() = (fileType as? PlayableFileTypeInfo)?.duration
}

/**
 * Pending attachment message for ordinary files
 */
data class PendingFileAttachmentMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isDeletable: Boolean,
    override val isEditable: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val fileType: FileTypeInfo,
    override val reactions: List<Reaction>,
    override val status: ChatMessageStatus,
    override val content: String?,
    override val file: File,
    override val isError: Boolean,
) : PendingAttachmentMessage


/**
 * Pending voice clip message
 */
data class PendingVoiceClipMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isDeletable: Boolean,
    override val isEditable: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val fileType: FileTypeInfo,
    override val reactions: List<Reaction>,
    override val status: ChatMessageStatus,
    override val content: String?,
    override val isError: Boolean,
) : PendingAttachmentMessage {
    override val file = null
}
