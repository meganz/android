package mega.privacy.android.domain.entity.meeting

/**
 * Chat session updated result
 *
 * @property session [ChatSession].
 * @property chatId Chat Id.
 * @property callId Call Id.
 */
data class ChatSessionUpdatesResult(
    val session: ChatSession?,
    val chatId: Long,
    val callId: Long,
)