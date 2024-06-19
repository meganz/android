package mega.privacy.android.domain.exception.chat

/**
 * Exception when it tries to retry a chat upload without success.
 */
class ChatUploadNotRetriedException : RuntimeException("Chat Upload not retried")