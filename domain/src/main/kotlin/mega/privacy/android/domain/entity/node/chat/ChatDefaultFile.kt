package mega.privacy.android.domain.entity.node.chat

import mega.privacy.android.domain.entity.node.TypedFileNode

/**
 * Chat file with no specific file type
 */
data class ChatDefaultFile(
    private val typedFileNode: TypedFileNode,
    override val chatId: Long,
    override val messageId: Long,
    override val messageIndex: Int = 0,
) : TypedFileNode by typedFileNode, ChatFile