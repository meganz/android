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
 * @property changes            List of [ScheduledMeetingChanges].
 * @property isCanceled
 */
data class ChatScheduledMeeting constructor(
    val chatId: Long = -1,
    val schedId: Long = -1,
    val parentSchedId: Long? = null,
    val organizerUserId: Long? = null,
    val timezone: String? = null,
    val startDateTime: Long? = null,
    val endDateTime: Long? = null,
    val title: String? = "",
    val description: String? = "",
    val attributes: String? = "",
    val overrides: Long? = null,
    val flags: ChatScheduledFlags? = null,
    val rules: ChatScheduledRules? = null,
    val changes: List<ScheduledMeetingChanges>? = null,
    val isCanceled: Boolean = false,
)
