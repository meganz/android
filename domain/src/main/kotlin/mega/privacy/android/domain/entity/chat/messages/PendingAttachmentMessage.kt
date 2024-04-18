package mega.privacy.android.domain.entity.chat.messages

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.PlayableFileTypeInfo
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.entity.node.NodeId
import java.io.File

/**
 * Pending attachment message
 * @property file
 * @property transferTag
 * @property state
 * @property nodeId
 */
sealed interface PendingAttachmentMessage : AttachmentMessage {
    val file: File?
    val transferTag: Int?
    val state: PendingMessageState
    val nodeId: NodeId?
    override val isMine get() = true
    override val duration get() = (fileType as? PlayableFileTypeInfo)?.duration
    override val rowId get() = -1L
    override fun isNotSent() = true // pending messages are not sent by definition
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
    override val fileType: FileTypeInfo,
    override val reactions: List<Reaction>,
    override val status: ChatMessageStatus,
    override val content: String?,
    override val transferTag: Int?,
    override val state: PendingMessageState,
    override val nodeId: NodeId?,
    override val fileName: String,
    override val fileSize: Long,
    val filePath: String,
) : PendingAttachmentMessage {
    @Transient
    override val file = File(filePath)
}


/**
 * Pending voice clip message
 * @property filePath
 */
@Serializable
data class PendingVoiceClipMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isDeletable: Boolean,
    override val isEditable: Boolean,
    override val userHandle: Long,
    override val fileType: FileTypeInfo,
    override val reactions: List<Reaction>,
    override val status: ChatMessageStatus,
    override val content: String?,
    override val transferTag: Int?,
    override val state: PendingMessageState,
    override val nodeId: NodeId?,
    override val fileName: String,
    val filePath: String,
) : PendingAttachmentMessage {
    @Transient
    override val file = File(filePath)
    override val fileSize = 0L //we don't need it in voice clips
}
