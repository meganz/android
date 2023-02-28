package mega.privacy.android.domain.exception

/**
 * Chat not correctly initialized exception.
 */
sealed class ChatNotInitializedException : RuntimeException("ChatNotInitializedException")

/**
 * Chat not initialized with error status.
 */
class ChatNotInitializedErrorStatus : ChatNotInitializedException()

/**
 * Chat not initialized with unknown status.
 */
class ChatNotInitializedUnknownStatus : ChatNotInitializedException()