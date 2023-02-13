package mega.privacy.android.domain.entity.chat

/**
 * Chat scheduled meeting occurrence
 *
 * @property schedId            Scheduled meeting id
 * @property cancelled          if scheduled meeting occurrence is going to be cancelled
 * @property timezone           Time zone
 * @property startDateTime      Timestamp of start date time
 * @property endDateTime        Timestamp of end date time
 */
data class ChatScheduledMeetingOccurr(
    val schedId: Long = -1,
    val cancelled: Int,
    val timezone: String? = null,
    val startDateTime: Long? = null,
    val endDateTime: Long? = null,
)
