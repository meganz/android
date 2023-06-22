package mega.privacy.android.domain.entity.meeting

/**
 * Scheduled meeting data
 *
 * @property schedId
 * @property isRecurringDaily
 * @property isRecurringWeekly
 * @property isRecurringMonthly
 * @property isPending
 * @property scheduledStartTimestamp
 * @property scheduledEndTimestamp
 * @property scheduledTimestampFormatted
 */
data class ScheduledMeetingData(
    val schedId: Long,
    val isRecurringDaily: Boolean = false,
    val isRecurringWeekly: Boolean = false,
    val isRecurringMonthly: Boolean = false,
    val isPending: Boolean = false,
    val scheduledStartTimestamp: Long? = null,
    val scheduledEndTimestamp: Long? = null,
    val scheduledTimestampFormatted: String? = null,
)
