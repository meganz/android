package mega.privacy.android.app.presentation.notification.model.extensions

import mega.privacy.android.app.presentation.notification.model.SchedMeetingNotification
import mega.privacy.android.domain.entity.ScheduledMeetingAlert
import mega.privacy.android.domain.entity.UpdatedScheduledMeetingDateTimeAlert
import mega.privacy.android.domain.entity.UserAlert

/**
 * Scheduled meeting notification based on an User Alert
 *
 * @return SchedMeetingNotification
 */
internal fun UserAlert.schedMeetingNotification(): SchedMeetingNotification? =
    if (this is ScheduledMeetingAlert) {
        SchedMeetingNotification(
            scheduledMeeting = scheduledMeeting?.copy(
                startDateTime = startDate ?: scheduledMeeting?.startDateTime,
                endDateTime = endDate ?: scheduledMeeting?.endDateTime,
            ),
            hasTimeChanged = hasTimeChanged(),
            hasDateChanged = hasDateChanged(),
        )
    } else {
        null
    }

/**
 * Check if Scheduled meeting time has changed
 *
 * @return true if has changed, false otherwise
 */
internal fun UserAlert.hasTimeChanged(): Boolean =
    this is UpdatedScheduledMeetingDateTimeAlert && hasTimeChanged

/**
 * Check if Scheduled meeting date has changed
 *
 * @return true if has changed, false otherwise
 */
internal fun UserAlert.hasDateChanged(): Boolean =
    this is UpdatedScheduledMeetingDateTimeAlert && hasDateChanged
