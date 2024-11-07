package mega.privacy.android.app.presentation.notification.model

import android.content.Context
import androidx.compose.ui.unit.TextUnit
import mega.privacy.android.shared.original.core.ui.controls.notifications.NotificationItemType

/**
 * Notification
 *
 * @property sectionTitle
 * @property sectionType
 * @property title
 * @property titleTextSize
 * @property description
 * @property schedMeetingNotification
 * @property dateText
 * @property isNew
 * @property onClick
 * @constructor Create empty Notification
 */
data class Notification(
    val sectionTitle: (Context) -> String,
    val sectionType: NotificationItemType,
    val title: (Context) -> String,
    val titleTextSize: TextUnit,
    val description: (Context) -> String?,
    val schedMeetingNotification: SchedMeetingNotification?,
    val dateText: (Context) -> String,
    val isNew: Boolean,
    val onClick: (NotificationNavigationHandler) -> Unit,
)
