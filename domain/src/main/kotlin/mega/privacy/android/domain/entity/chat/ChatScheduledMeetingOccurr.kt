package mega.privacy.android.domain.entity.chat

/**
 * Chat scheduled meeting occurrence
 *
 * @property schedId
 * @property parentSchedId
 * @property cancelled
 * @property timezone
 * @property startDateTime
 * @property endDateTime
 * @property overrides
 */
data class ChatScheduledMeetingOccurr constructor(
    val schedId: Long,
    val parentSchedId: Long = -1,
    val cancelled: Int? = null,
    val timezone: String? = null,
    val startDateTime: Long? = null,
    val endDateTime: Long? = null,
    val overrides: Long? = null,
)
