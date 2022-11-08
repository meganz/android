package mega.privacy.android.domain.exception

/**
 * Exception when chat is in terminated status, a logout is in progress
 */
class ChatLoggingOutException : RuntimeException("ChatNotInitializedException")