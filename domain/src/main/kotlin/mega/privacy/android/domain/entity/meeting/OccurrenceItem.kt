package mega.privacy.android.domain.entity.meeting

import java.time.ZonedDateTime

/**
 * Occurrence item
 *
 * @property scheduledMeetingId     Scheduled meeting id
 * @property cancelled              if scheduled meeting occurrence is going to be cancelled
 * @property startDateTime          [ZonedDateTime]
 * @property endDateTime            [ZonedDateTime]
 * @property dateFormatted          Date of occurrence formatted
 * @property timeFormatted          Time of occurrence formatted
 */
data class OccurrenceItem constructor(
    val scheduledMeetingId: Long = -1,
    val cancelled: Int,
    val startDateTime: ZonedDateTime? = null,
    val endDateTime: ZonedDateTime? = null,
    val dateFormatted: String? = null,
    val timeFormatted: String? = null,
)