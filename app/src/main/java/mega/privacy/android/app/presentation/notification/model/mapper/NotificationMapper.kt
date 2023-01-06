package mega.privacy.android.app.presentation.notification.model.mapper

import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.model.extensions.backgroundColor
import mega.privacy.android.app.presentation.notification.model.extensions.dateText
import mega.privacy.android.app.presentation.notification.model.extensions.description
import mega.privacy.android.app.presentation.notification.model.extensions.descriptionMaxWidth
import mega.privacy.android.app.presentation.notification.model.extensions.onClick
import mega.privacy.android.app.presentation.notification.model.extensions.sectionColour
import mega.privacy.android.app.presentation.notification.model.extensions.sectionIcon
import mega.privacy.android.app.presentation.notification.model.extensions.sectionTitle
import mega.privacy.android.app.presentation.notification.model.extensions.separatorMargin
import mega.privacy.android.app.presentation.notification.model.extensions.title
import mega.privacy.android.app.presentation.notification.model.extensions.titleMaxWidth
import mega.privacy.android.app.presentation.notification.model.extensions.titleTextSize
import mega.privacy.android.domain.entity.UserAlert

/**
 * Mapper to convert [UserAlert] to [Notification].
 */
typealias NotificationMapper = (@JvmSuppressWildcards UserAlert) -> @JvmSuppressWildcards Notification

/**
 * Get notification
 *
 */
internal fun getNotification(alert: UserAlert) = Notification(
    sectionTitle = alert.sectionTitle(),
    sectionColour = alert.sectionColour(),
    sectionIcon = alert.sectionIcon(),
    title = alert.title(),
    titleTextSize = alert.titleTextSize(),
    titleMaxWidth = alert.titleMaxWidth(),
    description = alert.description(),
    descriptionMaxWidth = alert.descriptionMaxWidth(),
    dateText = alert.dateText(),
    isNew = !alert.seen,
    backgroundColor = alert.backgroundColor(),
    separatorMargin = alert.separatorMargin(),
    onClick = alert.onClick(),
)