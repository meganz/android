package mega.privacy.android.domain.entity.chat

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

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
    val attributes: String?,
    val overrides: Long? = null,
    val flags: ChatScheduledFlags? = null,
    val rules: ChatScheduledRules? = null,
    val changes: ScheduledMeetingChanges? = null,
    val isCanceled: Boolean = false,
) {

    /**
     * Check if Meeting is pending to be started or finished
     *
     * @return  true if it's pending, false otherwise
     */
    fun isPending(): Boolean {
        val now = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
        return startDateTime?.toZonedDateTime()?.isAfter(now) == true
                || endDateTime?.toZonedDateTime()?.isAfter(now) == true
                || (rules != null && (rules.until == 0L || rules.until.toZonedDateTime()
            .isAfter(now)))
    }

    private fun Long.toZonedDateTime(): ZonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneOffset.UTC)
}
