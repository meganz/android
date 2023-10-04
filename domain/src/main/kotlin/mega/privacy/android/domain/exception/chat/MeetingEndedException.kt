package mega.privacy.android.domain.exception.chat

/**
 * Meeting ended exception
 * @property link  Meeting link
 */
data class MeetingEndedException(val link: String) : RuntimeException("meeting has ended")