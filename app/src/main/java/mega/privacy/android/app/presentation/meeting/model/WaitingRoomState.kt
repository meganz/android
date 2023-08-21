package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.presentation.meeting.RecurringMeetingInfoViewModel
import java.time.ZonedDateTime

/**
 * Data class defining the state of [RecurringMeetingInfoViewModel]
 *
 * @property finish                     True, if the activity is to be terminated.
 * @property chatId                     Chat id.
 * @property schedId                    Scheduled meeting id.
 * @property title                      Scheduled meeting title.
 * @property formattedTimestamp         Scheduled meeting timestamp formatted.
 * @property micEnabled                 Flag to check if mic is enabled
 * @property cameraEnabled              Flag to check if camera is enabled
 * @property speakerEnabled             Flag to check if speaker is enabled
 **/
data class WaitingRoomState(
    val finish: Boolean = false,
    val chatId: Long = -1L,
    val schedId: Long = -1L,
    val title: String = "Book Club",
    val formattedTimestamp: String = "Monday, 30 May · 10:25-11:25",
    val micEnabled: Boolean = false,
    val cameraEnabled: Boolean = false,
    val speakerEnabled: Boolean = false,
)