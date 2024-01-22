package mega.privacy.android.domain.entity.chat

/**
 * Chat room preference
 *
 * @property chatId Chat identifier
 * @property draftMessage Draft message
 */
data class ChatRoomPreference(
    val chatId: Long,
    val draftMessage: String = "",
)
