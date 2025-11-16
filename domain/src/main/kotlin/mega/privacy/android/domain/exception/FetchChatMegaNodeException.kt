package mega.privacy.android.domain.exception

/**
 * Exception thrown when chat MegaNode cannot be fetched from chat message IDs.
 *
 * @param chatId Chat room ID
 * @param messageId Message ID
 */
class FetchChatMegaNodeException(
    val chatId: Long,
    val messageId: Long,
) : RuntimeException(
    "Chat node not found for chatId: $chatId, messageId: $messageId"
)

