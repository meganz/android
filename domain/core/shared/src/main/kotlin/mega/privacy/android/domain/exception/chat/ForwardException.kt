package mega.privacy.android.domain.exception.chat

/**
 * Create chat exception when trying to create a chat.
 *
 * @property info
 */
class ForwardException(val info: String? = null) : RuntimeException("ForwardException : $info")