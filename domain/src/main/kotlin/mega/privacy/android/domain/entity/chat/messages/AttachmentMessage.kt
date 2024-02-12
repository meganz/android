package mega.privacy.android.domain.entity.chat.messages

import kotlinx.serialization.Polymorphic
import mega.privacy.android.domain.entity.FileTypeInfo

import kotlin.time.Duration

/**
 * Common interface for typed messages with attachment (pending messages and node attachment messages)
 * @property fileSize in bytes
 * @property fileName
 * @property duration
 * @property fileType
 */
@Polymorphic
interface AttachmentMessage : TypedMessage {
    val fileSize: Long
    val fileName: String
    val duration: Duration?
    val fileType: FileTypeInfo
}