package mega.privacy.android.app.main.model

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