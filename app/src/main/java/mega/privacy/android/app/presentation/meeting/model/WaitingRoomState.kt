package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.presentation.meeting.RecurringMeetingInfoViewModel
import java.time.ZonedDateTime

/**
 * Data class defining the state of [RecurringMeetingInfoViewModel]
 *
 * @property finish                     True, if the activity is to be terminated.
 * @property chatId                     Chat id.
 * @property schedId                    Scheduled meeting id.
 * @property schedMeetTitle             Scheduled meeting title.
 * @property schedMeetDate              Scheduled meeting [ZonedDateTime].
 **/
data class WaitingRoomState(
    val finish: Boolean = false,
    val chatId: Long = -1L,
    val schedId: Long = -1L,
    val schedMeetTitle: String = "Book Club",
    val schedMeetDate: ZonedDateTime = ZonedDateTime.now(),
)