package mega.privacy.android.domain.exception.chat

/**
 * Exception when it tries to delete a non-deletable message.
 */
class MessageNonDeletableException : RuntimeException("Message is not deletable")