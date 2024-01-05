package mega.privacy.android.domain.exception.chat

/**
 * Meeting ended exception
 * @property link  Meeting link
 * @property chatId Chat id
 */
data class MeetingEndedException(
    val link: String,
    val chatId: Long,
) : RuntimeException("meeting has ended")