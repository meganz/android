package mega.privacy.android.domain.entity.node.chat

import mega.privacy.android.domain.entity.node.TypedFileNode

/**
 * Chat file
 *
 * @property chatId: The id of the chat that contains this file
 * @property messageId: The id of the message that contains this file
 * @property messageIndex: The index of the file in message attachments, usually 0 since there's usually only one file. Keeping this because the SDK, in theory, allows for multiple files per message.
 */
sealed interface ChatFile : TypedFileNode {
    val chatId: Long
    val messageId: Long
    val messageIndex: Int
}