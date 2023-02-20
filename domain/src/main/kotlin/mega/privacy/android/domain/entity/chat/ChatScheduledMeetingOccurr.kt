package mega.privacy.android.domain.entity.chat

/**
 * Chat scheduled meeting occurrence
 *
 * @property schedId
 * @property parentSchedId
 * @property isCancelled
 * @property timezone
 * @property startDateTime
 * @property endDateTime
 * @property overrides
 */
data class ChatScheduledMeetingOccurr constructor(
    val schedId: Long,
    val parentSchedId: Long = -1,
    val isCancelled: Boolean = false,
    val timezone: String? = null,
    val startDateTime: Long? = null,
    val endDateTime: Long? = null,
    val overrides: Long? = null,
)
