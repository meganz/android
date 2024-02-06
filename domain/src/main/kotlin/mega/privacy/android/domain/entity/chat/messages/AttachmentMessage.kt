package mega.privacy.android.domain.entity.chat.messages

import kotlin.time.Duration

/**
 * Common interface for typed messages with attachment (pending messages and node attachment messages)
 * @property fileSize in bytes
 * @property fileName
 * @property duration
 */
interface AttachmentMessage : TypedMessage {
    val fileSize: Long
    val fileName: String
    val duration: Duration?
}