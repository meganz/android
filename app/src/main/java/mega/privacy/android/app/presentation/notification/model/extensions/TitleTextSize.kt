package mega.privacy.android.app.presentation.notification.model.extensions

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import mega.privacy.android.domain.entity.CustomAlert
import mega.privacy.android.domain.entity.IncomingShareAlert
import mega.privacy.android.domain.entity.ScheduledMeetingAlert
import mega.privacy.android.domain.entity.UserAlert

/**
 * Title text size
 *
 */
internal fun UserAlert.titleTextSize(): TextUnit = when (this) {
    is CustomAlert -> 14.sp
    is IncomingShareAlert -> 14.sp
    is ScheduledMeetingAlert -> 14.sp
    else -> 16.sp
}

