package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.presentation.meeting.RecurringMeetingInfoViewModel
import mega.privacy.android.domain.entity.chat.ChatAvatarItem

/**
 * Data class defining the state of [RecurringMeetingInfoViewModel]
 *
 * @property chatId                     Chat id.
 * @property schedId                    Scheduled meeting id.
 * @property hasStarted                 Flag to check if the meeting has started.
 * @property title                      Scheduled meeting title.
 * @property formattedTimestamp         Scheduled meeting timestamp formatted.
 * @property avatar                     Current user Avatar
 * @property micEnabled                 Flag to check if mic is enabled
 * @property cameraEnabled              Flag to check if camera is enabled
 * @property speakerEnabled             Flag to check if speaker is enabled
 * @property speakerEnabled             Flag to check if speaker is enabled
 * @property finish                     True, if the activity is to be terminated.
 **/
data class WaitingRoomState(
    val chatId: Long = -1L,
    val schedId: Long = -1L,
    val hasStarted: Boolean = false,
    val title: String? = null,
    val formattedTimestamp: String? = null,
    val avatar: ChatAvatarItem? = null,
    val micEnabled: Boolean = false,
    val cameraEnabled: Boolean = false,
    val speakerEnabled: Boolean = false,
    val finish: Boolean = false,
)
