package mega.privacy.android.app.presentation.notification.view

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.presentation.meeting.view.getRecurringMeetingDateTime
import mega.privacy.android.app.presentation.notification.model.SchedMeetingNotification
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.white_alpha_087

/**
 * Scheduled Meeting notification meeting view
 *
 * @param notification
 */
@Composable
fun NotificationSchedMeetingView(notification: SchedMeetingNotification) {
    val dateText = getRecurringMeetingDateTime(
        scheduledMeeting = notification.scheduledMeeting!!,
        is24HourFormat = DateFormat.is24HourFormat(LocalContext.current),
        highLightTime = notification.hasTimeChanged,
        highLightDate = notification.hasDateChanged,
    )

    Text(
        text = dateText,
        modifier = Modifier
            .padding(start = 16.dp, top = 2.dp, end = 16.dp)
            .testTag("SchedMeetingTime"),
        color = if (MaterialTheme.colors.isLight) grey_alpha_087 else white_alpha_087,
        style = MaterialTheme.typography.caption,
        fontSize = 12.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "PreviewNotificationSchedMeetingTime")
@Composable
private fun PreviewNotificationSchedMeetingView() {
    NotificationSchedMeetingView(
        notification = SchedMeetingNotification(null)
    )
}
