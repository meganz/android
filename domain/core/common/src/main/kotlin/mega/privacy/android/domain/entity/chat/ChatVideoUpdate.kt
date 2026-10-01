package mega.privacy.android.domain.entity.chat

/**
 * Chat video update
 *
 * @property width
 * @property height
 * @property byteBuffer
 * @constructor Create empty Chat video update
 */
class ChatVideoUpdate(
    val width: Int,
    val height: Int,
    val byteBuffer: ByteArray,
)
