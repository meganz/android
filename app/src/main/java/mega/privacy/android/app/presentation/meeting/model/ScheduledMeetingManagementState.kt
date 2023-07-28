package mega.privacy.android.app.presentation.meeting.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr

/**
 * Scheduled meeting management state
 * @property finish                     True, if the activity is to be terminated.
 * @property selectedOccurrence         Current selected [ChatScheduledMeetingOccurr]
 * @property isChatHistoryEmpty         True if chat history only has management messages or false otherwise
 * @property chatId                     Chat ID of the scheduled meeting
 * @property selectOccurrenceEvent      Select [ChatScheduledMeetingOccurr] event
 * @property chatRoom                   [ChatRoom] of the scheduled meeting
 * @property snackbarMessageContent     State to show snackbar message
 * @property cancelOccurrenceTapped     Indicates if cancel occurrence option was tapped
 * @property chatRoomItem               Selected [ChatRoomItem]
 * @constructor Create empty Scheduled meeting management state
 */
data class ScheduledMeetingManagementState constructor(
    val finish: Boolean = false,
    val selectedOccurrence: ChatScheduledMeetingOccurr? = null,
    val isChatHistoryEmpty: Boolean? = null,
    val chatId: Long? = null,
    val selectOccurrenceEvent: StateEvent = consumed,
    val chatRoom: ChatRoom? = null,
    val snackbarMessageContent: StateEventWithContent<String> = consumed(),
    val cancelOccurrenceTapped: Boolean = false,
    val chatRoomItem: ChatRoomItem? = null,
)