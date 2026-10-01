package mega.privacy.android.domain.entity.chat

/**
 * ChatConnectionState
 *
 * Domain model corresponds to OnChatConnectionStateUpdate of MegaChatListenerInterface
 * @param chatId chat id
 * @param chatConnectionStatus [ChatConnectionStatus]
 */
data class ChatConnectionState(val chatId: Long, val chatConnectionStatus: ChatConnectionStatus)
