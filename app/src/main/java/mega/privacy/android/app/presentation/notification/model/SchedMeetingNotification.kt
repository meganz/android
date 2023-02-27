package mega.privacy.android.app.presentation.notification.model

import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting

/**
 * Sched meeting notification
 *
 * @property hasTimeChanged
 * @property hasDateChanged
 * @property scheduledMeeting
 * @constructor Create empty Sched meeting notification
 */
data class SchedMeetingNotification constructor(
    val scheduledMeeting: ChatScheduledMeeting?,
    val hasTimeChanged: Boolean = false,
    val hasDateChanged: Boolean = false,
)
