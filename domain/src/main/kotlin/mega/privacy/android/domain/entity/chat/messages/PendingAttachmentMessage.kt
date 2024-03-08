package mega.privacy.android.domain.entity.chat.messages

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.PlayableFileTypeInfo
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import java.io.File

/**
 * Pending attachment message
 * @property file
 */
sealed interface PendingAttachmentMessage : AttachmentMessage {
    val file: File?
    override val isMine get() = true
    override val fileName get() = file?.name ?: ""
    override val fileSize get() = file?.length() ?: 0L
    override val duration get() = (fileType as? PlayableFileTypeInfo)?.duration
}

/**
 * Pending attachment message for ordinary files
 * @property filePath
 */
@Serializable
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
    val filePath: String,
) : PendingAttachmentMessage {
    @Transient
    override val file = File(filePath)
}


/**
 * Pending voice clip message
 */
@Serializable
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
) : PendingAttachmentMessage {
    @Transient
    override val file = null
}
