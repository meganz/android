package mega.privacy.android.app.presentation.notification.view.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_012
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_012
import mega.privacy.android.shared.original.core.ui.utils.intToDp

@Composable
internal fun NotificationDivider(horizontalPadding: Int) {
    val modifier = Modifier
    if (horizontalPadding != 0) {
        modifier.padding(horizontal = intToDp(px = horizontalPadding))
    }
    Divider(
        modifier = modifier.testTag(DIVIDER_TEST_TAG),
        color = if (MaterialTheme.colors.isLight) grey_alpha_012 else white_alpha_012,
        thickness = 1.dp
    )
}

@Composable
internal fun getHorizontalPaddingForDivider(
    notification: Notification,
    position: Int,
    notifications: List<Notification>,
): Int {
    return if (notification.isNew) {
        if (position < notifications.size - 1) {
            val nextNotification = notifications[position + 1]
            if (nextNotification.isNew) notification.separatorMargin(LocalContext.current) else 0
        } else {
            0
        }
    } else {
        notification.separatorMargin(LocalContext.current)
    }
}

internal const val DIVIDER_TEST_TAG = "notification_divider:divider"

