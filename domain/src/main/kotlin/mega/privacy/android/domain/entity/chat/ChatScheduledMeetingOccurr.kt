package mega.privacy.android.domain.entity.chat

/**
 * Chat scheduled meeting occurrence
 *
 * @property schedId
 * @property cancelled
 * @property timezone
 * @property startDateTime
 * @property endDateTime
 */
data class ChatScheduledMeetingOccurr(
    val schedId: Long,
    val cancelled: Int,
    val timezone: String,
    val startDateTime: String,
    val endDateTime: String,
)
