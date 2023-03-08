package mega.privacy.android.app.presentation.chat.model

import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus

/**
 * Chat UI state
 *
 * @property chatId                     Chat Id.
 * @property schedId                    Scheduled meeting Id.
 * @property error                      String resource id for showing an error.
 * @property isCallAnswered             Handle when a call is answered.
 * @property isChatInitialised          True, if the chat is initialised. False, if not.
 * @property currentCallChatId          Chat id of the call.
 * @property scheduledMeetingStatus     [ScheduledMeetingStatus]
 * @property schedIsPending             True, if scheduled meeting is pending. False, if not.
 */
data class ChatState(
    val chatId: Long = -1L,
    val schedId: Long? = null,
    val error: Int? = null,
    val isCallAnswered: Boolean = false,
    val isChatInitialised: Boolean = false,
    val currentCallChatId: Long = -1L,
    val scheduledMeetingStatus: ScheduledMeetingStatus? = null,
    val schedIsPending: Boolean = false,
)