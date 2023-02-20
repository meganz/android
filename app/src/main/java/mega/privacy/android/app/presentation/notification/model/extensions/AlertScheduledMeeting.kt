package mega.privacy.android.app.presentation.notification.model.extensions

import mega.privacy.android.domain.entity.ScheduledMeetingAlert
import mega.privacy.android.domain.entity.UserAlert
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting

/**
 * Recurring Chat Scheduled meeting for the User Alert
 *
 * @return  ChatScheduledMeeting
 */
internal fun UserAlert.recurringScheduledMeeting(): ChatScheduledMeeting? =
    if (this is ScheduledMeetingAlert && isRecurring) scheduledMeeting else null
