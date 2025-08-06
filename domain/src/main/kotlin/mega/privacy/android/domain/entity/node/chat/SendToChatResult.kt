package mega.privacy.android.domain.entity.node.chat

/**
 * Send to chat result
 *
 * @property nodeIds [LongArray]
 * @property chatIds [LongArray]
 * @property userHandles [LongArray]
 */
data class SendToChatResult(
    val nodeIds: LongArray,
    val chatIds: LongArray,
    val userHandles: LongArray,
)