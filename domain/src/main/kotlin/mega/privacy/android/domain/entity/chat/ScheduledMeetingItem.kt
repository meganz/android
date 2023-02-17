package mega.privacy.android.domain.entity.chat

/**
 * Meeting item
 *
 * @property chatId                     Chat id
 * @property scheduledMeetingId         Scheduled meeting id
 * @property title                      Scheduled meeting title
 * @property description                Scheduled meeting description
 * @property startDateTime              TimeStamp of Start date time
 * @property endDateTime                TimeStamp of End date time
 * @property rules                      [ChatScheduledRules]
 */
data class ScheduledMeetingItem constructor(
    val chatId: Long = -1,
    val scheduledMeetingId: Long = -1,
    val title: String? = "",
    val description: String? = "",
    val startDateTime: Long? = null,
    val endDateTime: Long? = null,
    val rules: ChatScheduledRules? = null,
)