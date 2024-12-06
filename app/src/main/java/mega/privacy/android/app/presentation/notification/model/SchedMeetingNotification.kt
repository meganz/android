package mega.privacy.android.app.presentation.notification.model

import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ChatScheduledMeetingOccurr

/**
 * Sched meeting notification
 *
 * @property hasTimeChanged
 * @property hasDateChanged
 * @property scheduledMeeting
 * @property occurrenceChanged
 * @constructor Create empty Sched meeting notification
 */
data class SchedMeetingNotification(
    val scheduledMeeting: ChatScheduledMeeting?,
    val hasTimeChanged: Boolean = false,
    val hasDateChanged: Boolean = false,
    val occurrenceChanged: ChatScheduledMeetingOccurr? = null,
)
