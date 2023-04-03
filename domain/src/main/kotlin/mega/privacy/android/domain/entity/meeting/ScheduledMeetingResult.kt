package mega.privacy.android.domain.entity.meeting

/**
 * Scheduled meeting result
 *
 * @property schedId
 * @property isRecurringDaily
 * @property isRecurringWeekly
 * @property isRecurringMonthly
 * @property isPending
 * @property scheduledStartTimestamp
 * @property scheduledEndTimestamp
 * @property scheduledMeetingStatus
 */
data class ScheduledMeetingResult(
    val schedId: Long? = null,
    val isRecurringDaily: Boolean = false,
    val isRecurringWeekly: Boolean = false,
    val isRecurringMonthly: Boolean = false,
    val isPending: Boolean = false,
    val scheduledStartTimestamp: Long? = null,
    val scheduledEndTimestamp: Long? = null,
    val scheduledMeetingStatus: ScheduledMeetingStatus? = null,
)
