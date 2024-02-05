package mega.privacy.android.domain.entity.chat.messages

/**
 * Common interface for typed messages with attachment (pending messages and node attachment messages)
 * @property fileSize in bytes
 * @property fileName
 */
interface AttachmentMessage : TypedMessage {
    val fileSize: Long
    val fileName: String
}