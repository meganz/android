package mega.privacy.android.app.presentation.notification.model.extensions

import mega.privacy.android.app.presentation.notification.model.NotificationItemType
import mega.privacy.android.domain.entity.ContactAlert
import mega.privacy.android.domain.entity.CustomAlert
import mega.privacy.android.domain.entity.IncomingShareAlert
import mega.privacy.android.domain.entity.ScheduledMeetingAlert
import mega.privacy.android.domain.entity.UserAlert

internal fun UserAlert.sectionType() = when (this) {
    is ContactAlert -> NotificationItemType.Contacts
    is IncomingShareAlert -> NotificationItemType.IncomingShares
    is ScheduledMeetingAlert -> NotificationItemType.ScheduledMeetings
    is CustomAlert -> NotificationItemType.Custom
    else -> NotificationItemType.Others
}