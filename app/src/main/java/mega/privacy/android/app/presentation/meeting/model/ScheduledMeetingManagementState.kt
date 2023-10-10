package mega.privacy.android.app.presentation.meeting.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.extensions.getDateFormatted
import mega.privacy.android.app.presentation.extensions.getEndTimeFormatted
import mega.privacy.android.app.presentation.extensions.getStartTimeFormatted
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr
import mega.privacy.android.domain.entity.meeting.WaitingRoomReminders
import java.time.ZonedDateTime

/**
 * Scheduled meeting management state
 * @property finish                             True, if the activity is to be terminated.
 * @property selectedOccurrence                 Current selected [ChatScheduledMeetingOccurr]
 * @property isChatHistoryEmpty                 True if chat history only has management messages or false otherwise
 * @property chatId                             Chat ID of the scheduled meeting
 * @property selectOccurrenceEvent              Select [ChatScheduledMeetingOccurr] event
 * @property chatRoom                           [ChatRoom] of the scheduled meeting
 * @property snackbarMessageContent             State to show snackbar message
 * @property displayDialog                      Indicates if display confirm dialog or not
 * @property enabledMeetingLinkOption           True if is enabled the meeting link option, false otherwise.
 * @property meetingLink                        Meeting link.
 * @property cancelOccurrenceTapped             Indicates if cancel occurrence option was tapped
 * @property editOccurrenceTapped               Indicates if edit occurrence option was tapped
 * @property chatRoomItem                       Selected [ChatRoomItem]
 * @property editedOccurrence                   Edited [ChatScheduledMeetingOccurr]
 * @property editedOccurrenceDate               [ZonedDateTime]
 * @property chatRoomItem                       Selected [ChatRoomItem]
 * @property waitingRoomReminder                [WaitingRoomReminders]
 * @property isCallInProgress                   True, if there is a call in progress. False, if not.
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
    val displayDialog: Boolean = false,
    val enabledMeetingLinkOption: Boolean = true,
    val meetingLink: String? = null,
    val cancelOccurrenceTapped: Boolean = false,
    val editOccurrenceTapped: Boolean = false,
    val chatRoomItem: ChatRoomItem? = null,
    val editedOccurrence: ChatScheduledMeetingOccurr? = null,
    val editedOccurrenceDate: ZonedDateTime? = null,
    val waitingRoomReminder: WaitingRoomReminders = WaitingRoomReminders.Enabled,
    val isCallInProgress: Boolean = false,
) {

    /**
     * Check if is valid the edition
     *
     * @return  true if its valid, false if not
     */
    fun isEditionValid(): Boolean =
        selectedOccurrence != editedOccurrence

    /**
     * Check if is date edited
     *
     * @return  true if its valid, false if not
     */
    fun isDateEdited(): Boolean =
        selectedOccurrence?.getDateFormatted() != editedOccurrence?.getDateFormatted()

    /**
     * Check if is start time edited
     *
     * @return  true if its valid, false if not
     */
    fun isStartTimeEdited(is24HourFormat: Boolean): Boolean =
        selectedOccurrence?.getStartTimeFormatted(is24HourFormat) != editedOccurrence?.getStartTimeFormatted(
            is24HourFormat
        )

    /**
     * Check if is end time edited
     *
     * @return  true if its valid, false if not
     */
    fun isEndTimeEdited(is24HourFormat: Boolean): Boolean =
        selectedOccurrence?.getEndTimeFormatted(is24HourFormat) != editedOccurrence?.getEndTimeFormatted(
            is24HourFormat
        )
}