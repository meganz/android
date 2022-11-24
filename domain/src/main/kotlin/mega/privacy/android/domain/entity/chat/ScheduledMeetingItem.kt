package mega.privacy.android.domain.entity.chat

/**
 * Meeting item
 *
 * @property chatId                     Chat id
 * @property scheduledMeetingId         Scheduled meeting id
 * @property title                      Scheduled meeting title
 * @property description                Scheduled meeting description
 * @property date                       Scheduled meeting date
 */
data class ScheduledMeetingItem constructor(
    val chatId: Long,
    val scheduledMeetingId: Long,
    val title: String = "",
    val description: String?,
    val date: String,
)