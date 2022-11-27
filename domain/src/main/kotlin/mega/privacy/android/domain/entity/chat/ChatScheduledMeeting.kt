package mega.privacy.android.domain.entity.chat

/**
 * Chat scheduled meeting
 *
 * @property chatId
 * @property schedId
 * @property parentSchedId
 * @property organizerUserId
 * @property timezone
 * @property startDateTime
 * @property endDateTime
 * @property title
 * @property description
 * @property attributes
 * @property overrides
 * @property flags
 * @property rules
 * @property changes            Changes [ScheduledMeetingChanges].
 */
data class ChatScheduledMeeting(
    val chatId: Long,
    val schedId: Long,
    val parentSchedId: Long,
    val organizerUserId: Long,
    val timezone: String,
    val startDateTime: String,
    val endDateTime: String,
    val title: String,
    val description: String,
    val attributes: String,
    val overrides: String,
    val flags: ChatScheduledFlags?,
    val rules: ChatScheduledRules?,
    val changes: ScheduledMeetingChanges? = null
)
