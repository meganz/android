package mega.privacy.android.domain.entity.chat

import java.time.ZonedDateTime

/**
 * Meeting item
 *
 * @property chatId                     Chat id
 * @property scheduledMeetingId         Scheduled meeting id
 * @property title                      Scheduled meeting title
 * @property description                Scheduled meeting description
 * @property date                       Scheduled meeting date
 * @property startDate                  [ZonedDateTime]
 * @property endDate                    [ZonedDateTime]
 */
data class ScheduledMeetingItem constructor(
    val chatId: Long,
    val scheduledMeetingId: Long,
    val title: String? = "",
    val description: String? = null,
    val date: String? = "",
    val startDate:ZonedDateTime? = null,
    val endDate:ZonedDateTime? = null,
)