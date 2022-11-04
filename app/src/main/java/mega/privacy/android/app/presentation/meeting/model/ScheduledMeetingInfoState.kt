package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.meeting.list.MeetingItem
import mega.privacy.android.app.meeting.list.adapter.ScheduledMeetingItem
import mega.privacy.android.domain.entity.contacts.ContactItem

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoViewModel]
 *
 * @property buttons             List of available action buttons.
 * @property contactItemList     List of [ContactItem].
 * @property emptyViewVisible    True if the empty view is visible, false otherwise.
 * @property buttonsVisible      True if the action buttons should be visible because there is not a
 *                               search in progress, false otherwise.
 * @property error               String resource id for showing an error.
 * @property result              Handle of the new chat conversation.
 */
data class ScheduledMeetingInfoState(
    val buttons: List<ScheduledMeetingInfoAction> = ScheduledMeetingInfoAction.values().asList(),
    val scheduledMeeting: ScheduledMeetingItem? = ScheduledMeetingItem(),
    val contactItemList: List<ContactItem> = emptyList(),
    val emptyViewVisible: Boolean = true,
    val buttonsVisible: Boolean = true,
    val error: Int? = null,
    val result: Long? = null,
)