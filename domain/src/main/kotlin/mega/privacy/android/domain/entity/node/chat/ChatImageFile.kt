package mega.privacy.android.domain.entity.node.chat

import mega.privacy.android.domain.entity.node.TypedImageNode

/**
 * Chat file representing an image
 * @property typedImageNode, the node containing the image node
 */
data class ChatImageFile(
    val typedImageNode: TypedImageNode,
    override val chatId: Long,
    override val messageId: Long,
    override val messageIndex: Int = 0,
) : TypedImageNode by typedImageNode, ChatFile