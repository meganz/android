package mega.privacy.android.domain.entity.chat.messages

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.entity.node.chat.ChatFile

/**
 * Node attachment message
 * @property fileNode The attached node
 * @property exists Whether the node exists
 */
@Serializable
data class NodeAttachmentMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isDeletable: Boolean,
    override val isEditable: Boolean,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val reactions: List<Reaction>,
    override val status: ChatMessageStatus,
    override val content: String?,
    override val exists: Boolean,
    override val rowId: Long,
    val fileNode: ChatFile,
) : AttachmentMessage {
    override val fileSize = fileNode.size
    override val fileName = fileNode.name
    override val duration = (fileNode.type as? VideoFileTypeInfo)?.duration
    override val fileType = fileNode.type
}
