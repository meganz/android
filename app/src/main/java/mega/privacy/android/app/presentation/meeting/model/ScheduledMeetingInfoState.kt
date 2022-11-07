package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.meeting.list.adapter.ScheduledMeetingItem
import mega.privacy.android.domain.entity.contacts.ContactItem

/**
 * Data class defining the state of [mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoViewModel]
 *
 * @property buttons                                    List of available action buttons.
 * @property scheduledMeeting                           Current scheduled meeting item.
 * @property contactItemList                            List of [ContactItem].
 * @property emptyViewVisible                           True if the empty view is visible, false otherwise.
 * @property enabledMeetingLinkOption                   True if is enabled the meeting link option, false otherwise.
 * @property enabledChatNotificationsOption             True if is enabled the chat notifications option, false otherwise.
 * @property enabledAllowNonHostAddParticipantsOption   True if is enabled the allow non-host participants option, false otherwise.
 * @property error                                      String resource id for showing an error.
 * @property result                                     Handle of the new chat conversation.
 */
data class ScheduledMeetingInfoState(
    val buttons: List<ScheduledMeetingInfoAction> = ScheduledMeetingInfoAction.values().asList(),
    val scheduledMeeting: ScheduledMeetingItem = ScheduledMeetingItem(),
    val contactItemList: List<ContactItem> = emptyList(),
    val emptyViewVisible: Boolean = true,
    val enabledMeetingLinkOption: Boolean = true,
    val enabledChatNotificationsOption: Boolean = true,
    val enabledAllowNonHostAddParticipantsOption: Boolean = true,
    val error: Int? = null,
    val result: Long? = null,
)