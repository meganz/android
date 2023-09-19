package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.domain.entity.chat.ChatAvatarItem

/**
 * Data class defining the state of [WaitingRoomViewModel]
 *
 * @property chatId                     Chat id.
 * @property schedId                    Scheduled meeting id.
 * @property callId                     Chat call id.
 * @property chatLink                   Chat link.
 * @property guestMode                  Flag to check if it's guest mode.
 * @property callStarted                Flag to check if the meeting call has started.
 * @property title                      Scheduled meeting title.
 * @property formattedTimestamp         Scheduled meeting timestamp formatted.
 * @property avatar                     Current user Avatar
 * @property micEnabled                 Flag to check if mic is enabled
 * @property cameraEnabled              Flag to check if camera is enabled
 * @property speakerEnabled             Flag to check if speaker is enabled
 * @property guestFirstName             Guest user first name
 * @property guestLastName              Guest user last name
 * @property denyAccessDialog           Flag to show Guest Leave Dialog
 * @property inactiveHostDialog         Flag to show Inactive Host Dialog
 * @property joinCall                   Flag to open the screen and join the call.
 * @property finish                     Flag to finish current screen.
 **/
data class WaitingRoomState(
    val chatId: Long = -1L,
    val schedId: Long = -1L,
    val callId: Long = -1L,
    val chatLink: String? = null,
    val guestMode: Boolean = false,
    val callStarted: Boolean = false,
    val title: String? = null,
    val formattedTimestamp: String? = null,
    val avatar: ChatAvatarItem? = null,
    val micEnabled: Boolean = false,
    val cameraEnabled: Boolean = false,
    val speakerEnabled: Boolean = false,
    val guestFirstName: String? = null,
    val guestLastName: String? = null,
    val denyAccessDialog: Boolean = false,
    val inactiveHostDialog: Boolean = false,
    val joinCall: Boolean = false,
    val finish: Boolean = false,
)
