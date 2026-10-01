package mega.privacy.android.domain.entity.chat.messages.paging

import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatMessage

/**
 * Fetch message page response
 *
 * @property chatId
 * @property messages
 * @property loadResponse
 */
data class FetchMessagePageResponse(
    val chatId: Long,
    val messages: List<ChatMessage>,
    val loadResponse: ChatHistoryLoadStatus,
)