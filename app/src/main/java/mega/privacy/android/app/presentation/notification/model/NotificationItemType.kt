package mega.privacy.android.app.presentation.notification.model

import androidx.compose.runtime.Composable
import mega.android.core.ui.theme.values.TextColor

/**
 * Type of the notification
 */
enum class NotificationItemType {
    Contacts,
    IncomingShares,
    ScheduledMeetings,
    Custom,
    Others;

    @Composable
    internal fun titleColor() = when (this) {
        ScheduledMeetings -> TextColor.Error
        IncomingShares -> TextColor.Warning
        Contacts -> TextColor.Accent
        Custom -> TextColor.Error
        Others -> TextColor.Warning
    }
}