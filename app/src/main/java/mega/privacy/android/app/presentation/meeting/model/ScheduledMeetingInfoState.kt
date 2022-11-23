package mega.privacy.android.app.presentation.meeting.model

import mega.privacy.android.app.meeting.list.ScheduledMeetingItem
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoViewModel
import mega.privacy.android.domain.entity.contacts.ContactItem

/**
 * Data class defining the state of [ScheduledMeetingInfoViewModel]
 *
 * @property buttons                                    List of available action buttons.
 * @property scheduledMeeting                           Current scheduled meeting item.
 * @property participantItemList                        List of [ContactItem].
 * @property seeMoreVisible                             True if see more option is visible, false otherwise.
 * @property enabledMeetingLinkOption                   True if is enabled the meeting link option, false otherwise.
 * @property enabledChatNotificationsOption             True if is enabled the chat notifications option, false otherwise.
 * @property enabledAllowNonHostAddParticipantsOption   True if is enabled the allow non-host participants option, false otherwise.
 * @property error                                      String resource id for showing an error.
 * @property result                                     Handle of the new chat conversation.
 */
data class ScheduledMeetingInfoState(
    val buttons: List<ScheduledMeetingInfoAction> = ScheduledMeetingInfoAction.values().asList(),
    val scheduledMeeting: ScheduledMeetingItem = ScheduledMeetingItem(),
    val participantItemList: List<ContactItem> = emptyList(),
    val seeMoreVisible: Boolean = true,
    val enabledMeetingLinkOption: Boolean = true,
    val enabledChatNotificationsOption: Boolean = true,
    val enabledAllowNonHostAddParticipantsOption: Boolean = true,
    val error: Int? = null,
    val result: Long? = null,
)