package mega.privacy.android.domain.entity.chat.messages

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.PlayableFileTypeInfo
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.uri.UriPath

/**
 * Pending attachment message
 * @property uriPath
 * @property transferUniqueId
 * @property state
 * @property nodeId
 */
sealed interface PendingAttachmentMessage : AttachmentMessage {
    val uriPath: UriPath?
    val transferUniqueId: Long?
    val state: PendingMessageState
    val nodeId: NodeId?
    override val isMine get() = true
    override val duration get() = (fileType as? PlayableFileTypeInfo)?.duration
    override val rowId get() = -1L
    override fun isNotSent() = true // pending messages are not sent by definition
}

/**
 * Pending attachment message for ordinary files
 * @property uriPath
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
    override val transferUniqueId: Long?,
    override val state: PendingMessageState,
    override val nodeId: NodeId?,
    override val fileName: String,
    override val fileSize: Long,
    override val uriPath: UriPath,
) : PendingAttachmentMessage


/**
 * Pending voice clip message
 * @property uriPath
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
    override val transferUniqueId: Long?,
    override val state: PendingMessageState,
    override val nodeId: NodeId?,
    override val fileName: String,
    override val uriPath: UriPath,
) : PendingAttachmentMessage {
    override val fileSize = 0L //we don't need it in voice clips
}
