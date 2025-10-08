package mega.privacy.android.app.presentation.notification.model

import android.content.Context
import androidx.compose.ui.unit.TextUnit
import androidx.navigation3.runtime.NavKey

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
 * @property isUnread
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
    val isUnread: Boolean,
    val onClick: (NotificationNavigationHandler) -> Unit,
    val destination: NavKey?,
)
