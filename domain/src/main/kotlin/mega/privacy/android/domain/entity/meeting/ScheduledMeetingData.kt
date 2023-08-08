package mega.privacy.android.domain.entity.meeting

/**
 * Scheduled meeting data
 *
 * @property schedId
 * @property title
 * @property isRecurringDaily
 * @property isRecurringWeekly
 * @property isRecurringMonthly
 * @property isPending
 * @property scheduledStartTimestamp
 * @property scheduledEndTimestamp
 * @property scheduledTimestampFormatted
 */
data class ScheduledMeetingData constructor(
    val schedId: Long,
    val title: String?,
    val isRecurringDaily: Boolean = false,
    val isRecurringWeekly: Boolean = false,
    val isRecurringMonthly: Boolean = false,
    val isPending: Boolean = false,
    val scheduledStartTimestamp: Long? = null,
    val scheduledEndTimestamp: Long? = null,
    val scheduledTimestampFormatted: String? = null,
)
