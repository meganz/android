package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.presentation.meeting.RecurringMeetingInfoViewModel
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatVideoUpdate

/**
 * Data class defining the state of [RecurringMeetingInfoViewModel]
 *
 * @property chatId                     Chat id.
 * @property schedId                    Scheduled meeting id.
 * @property callStarted                Flag to check if the meeting call has started.
 * @property title                      Scheduled meeting title.
 * @property formattedTimestamp         Scheduled meeting timestamp formatted.
 * @property avatar                     Current user Avatar
 * @property videoUpdate                Current Chat Video updates
 * @property micEnabled                 Flag to check if mic is enabled
 * @property cameraEnabled              Flag to check if camera is enabled
 * @property speakerEnabled             Flag to check if speaker is enabled
 * @property guestMode                  Flag to check if Guest UI should be shown
 * @property guestFirstName             Guest user first name
 * @property guestLastName              Guest user last name
 * @property joinCall                   Flag to open the screen and join the call.
 * @property finish                     True, if the activity is to be terminated.
 **/
data class WaitingRoomState(
    val chatId: Long = -1L,
    val schedId: Long = -1L,
    val callStarted: Boolean = false,
    val title: String? = null,
    val formattedTimestamp: String? = null,
    val avatar: ChatAvatarItem? = null,
    val videoUpdate: ChatVideoUpdate? = null,
    val micEnabled: Boolean = false,
    val cameraEnabled: Boolean = false,
    val speakerEnabled: Boolean = false,
    val guestMode: Boolean = false,
    val guestFirstName: String? = null,
    val guestLastName: String? = null,
    val joinCall: Boolean = false,
    val finish: Boolean = false,
)
