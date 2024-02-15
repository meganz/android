package mega.privacy.android.app.presentation.meeting.chat.model

/**
 * Different results of forwarding messages.
 */
sealed interface ForwardMessagesToChatsResult {

    /**
     * All the messages were forwarded with success to all chants.
     *
     * @property chatId Chat id where the messages were forwarded. Null in case of multiple chats.
     * @property messagesCount Number of messages that were forwarded.
     */
    data class AllSucceeded(val chatId: Long?, val messagesCount: Int) :
        ForwardMessagesToChatsResult

    /**
     * All messages were not forwarded to some chats because files are not available.
     *
     * @property messagesCount Number of messages that were not forwarded.
     */
    data class AllNotAvailable(val messagesCount: Int) : ForwardMessagesToChatsResult

    /**
     * Some messages were not forwarded to some chats because files are not available.
     *
     * @property chatId Chat id where the messages were forwarded. Null in case of multiple chats.
     * @property failuresCount Number of messages that were not forwarded.
     */
    data class SomeNotAvailable(val chatId: Long?, val failuresCount: Int) :
        ForwardMessagesToChatsResult

    /**
     * All messages were not forwarded to some chats.
     *
     * @property messagesCount Number of messages that were not forwarded.
     */
    data class AllFailed(val messagesCount: Int) : ForwardMessagesToChatsResult

    /**
     * Some messages were not forwarded to some chats.
     *
     * @property chatId Chat id where the messages were forwarded. Null in case of multiple chats.
     * @property failuresCount Number of messages that were not forwarded.
     */
    data class SomeFailed(val chatId: Long?, val failuresCount: Int) :
        ForwardMessagesToChatsResult
}