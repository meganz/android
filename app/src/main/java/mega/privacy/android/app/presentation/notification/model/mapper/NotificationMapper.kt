package mega.privacy.android.app.presentation.notification.model.mapper

import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.model.extensions.backgroundColor
import mega.privacy.android.app.presentation.notification.model.extensions.dateText
import mega.privacy.android.app.presentation.notification.model.extensions.description
import mega.privacy.android.app.presentation.notification.model.extensions.onClick
import mega.privacy.android.app.presentation.notification.model.extensions.schedMeetingNotification
import mega.privacy.android.app.presentation.notification.model.extensions.sectionColour
import mega.privacy.android.app.presentation.notification.model.extensions.sectionIcon
import mega.privacy.android.app.presentation.notification.model.extensions.sectionTitle
import mega.privacy.android.app.presentation.notification.model.extensions.separatorMargin
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
        sectionColour = alert.sectionColour(),
        sectionIcon = alert.sectionIcon(),
        title = alert.title(),
        titleTextSize = alert.titleTextSize(),
        description = alert.description(),
        schedMeetingNotification = alert.schedMeetingNotification(),
        dateText = alert.dateText(),
        isNew = !alert.seen,
        backgroundColor = alert.backgroundColor(),
        separatorMargin = alert.separatorMargin(),
        onClick = alert.onClick(),
    )
}