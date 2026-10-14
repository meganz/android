package mega.privacy.android.domain.exception.chat

/**
 * Exception thrown when the node history of a chat cannot be loaded, for example because it has
 * to be fetched from the server before the client is logged in.
 */
class ChatNodeHistoryLoadException : RuntimeException("Chat node history could not be loaded")
