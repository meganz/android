package mega.privacy.android.app.presentation.notification.model.mapper

import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.model.extensions.dateText
import mega.privacy.android.app.presentation.notification.model.extensions.description
import mega.privacy.android.app.presentation.notification.model.extensions.onClick
import mega.privacy.android.app.presentation.notification.model.extensions.schedMeetingNotification
import mega.privacy.android.app.presentation.notification.model.extensions.sectionType
import mega.privacy.android.app.presentation.notification.model.extensions.sectionTitle
import mega.privacy.android.app.presentation.notification.model.extensions.title
import mega.privacy.android.app.presentation.notification.model.extensions.titleTextSize
import mega.privacy.android.domain.entity.UserAlert
import javax.inject.Inject

/**
 * Mapper to convert [UserAlert] to [Notification].
 */
class NotificationMapper @Inject constructor() {

    /**
     * Invoke
     * Convert [UserAlert] to [Notification]
     * @param alert [UserAlert]
     * @return [Notification]
     */
    operator fun invoke(alert: UserAlert) = Notification(
        sectionTitle = alert.sectionTitle(),
        sectionType = alert.sectionType(),
        title = alert.title(),
        titleTextSize = alert.titleTextSize(),
        description = alert.description(),
        schedMeetingNotification = alert.schedMeetingNotification(),
        dateText = alert.dateText(),
        isNew = !alert.seen,
        onClick = alert.onClick(),
    )
}