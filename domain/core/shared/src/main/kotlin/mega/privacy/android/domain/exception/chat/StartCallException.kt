package mega.privacy.android.domain.exception.chat

/**
 * Start call exception when trying to open or start a call.
 */
class StartCallException(chatId: Long) : RuntimeException("Start call exception. Chat id $chatId")