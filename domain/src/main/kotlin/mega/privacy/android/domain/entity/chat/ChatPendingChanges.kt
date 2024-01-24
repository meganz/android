package mega.privacy.android.domain.entity.chat

/**
 * Chat pending changes
 *
 * @property chatId Chat identifier
 * @property draftMessage Draft message
 */
data class ChatPendingChanges(
    val chatId: Long,
    val draftMessage: String = "",
)
