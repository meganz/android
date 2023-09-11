package mega.privacy.android.app.presentation.notification.view

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.grey_500_grey_400
import mega.privacy.android.core.ui.theme.extensions.grey_900_grey_100
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.white_alpha_012
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.core.ui.theme.white_alpha_087
import mega.privacy.android.core.ui.utils.intToDp

@Composable
internal fun NotificationItemView(
    modifier: Modifier,
    notification: Notification,
    position: Int,
    notifications: List<Notification>,
    onClick: () -> Unit,
) {
    Column(modifier = modifier
        .clickable { onClick() }
        .background(
            color = Color(
                notification
                    .backgroundColor(LocalContext.current)
                    .toColorInt()
            )
        )
        .testTag("NotificationItemView")
        .fillMaxWidth()
        .wrapContentHeight()) {

        NotificationSectionRow(notification)

        val description = notification.description(LocalContext.current)

        NotificationTitleRow(
            notification.title,
            notification.titleTextSize,
            notification.isNew,
            !description.isNullOrBlank()
        )

        if (!description.isNullOrBlank()) {
            NotificationDescription(description)
        }

        if (notification.schedMeetingNotification?.scheduledMeeting != null) {
            NotificationSchedMeetingView(notification.schedMeetingNotification)
        }
        NotificationDate(notification)

        val horizontalPadding =
            getHorizontalPaddingForDivider(notification, position, notifications)
        NotificationDivider(horizontalPadding)
    }
}

@Composable
private fun getHorizontalPaddingForDivider(
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

@Composable
private fun NotificationSectionRow(
    notification: Notification,
) {
    Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
/*        notification.sectionIcon?.let {
            Icon(painter = painterResource(id = notification.sectionIcon),
                contentDescription = "Section icon",
                modifier = Modifier
                    .padding(end = 4.dp)
                    .testTag("SectionIcon"),
                tint = if (MaterialTheme.colors.isLight) orange_400 else orange_300)
        }*/
        Text(modifier = Modifier.testTag("SectionTitle"),
            color = colorResource(id = notification.sectionColour),
            text = notification.sectionTitle(LocalContext.current),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp)
    }
}

@Composable
private fun NotificationTitleRow(
    title: (Context) -> String,
    textSize: TextUnit,
    showNewIcon: Boolean,
    reducedLines: Boolean,
) {
    val titleText = title(LocalContext.current)
    val titleMaxLines = if (reducedLines) 1 else 3

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp)
    ) {
        MegaSpannedText(
            value = titleText,
            baseStyle = MaterialTheme.typography.subtitle1.copy(
                fontWeight = FontWeight.Medium,
                fontSize = textSize,
            ),
            styles = mapOf(
                SpanIndicator('A') to SpanStyle(color = MaterialTheme.colors.grey_900_grey_100),
                SpanIndicator('B') to SpanStyle(color = MaterialTheme.colors.grey_500_grey_400)
            ),
            color = MaterialTheme.colors.black_white,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 2.dp)
                .weight(1f)
                .testTag("Title")
        )

        if (showNewIcon) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .background(
                        color = MaterialTheme.colors.secondary,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Text(
                    text = stringResource(id = R.string.new_label_notification_item),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("IsNew"),
                    color = if (MaterialTheme.colors.isLight) white_alpha_087 else grey_alpha_087,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NotificationDescription(description: String) {
    MegaSpannedText(
        value = description,
        baseStyle = MaterialTheme.typography.subtitle2.copy(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        ),
        styles = mapOf(
            SpanIndicator('A') to SpanStyle(color = MaterialTheme.colors.grey_900_grey_100),
            SpanIndicator('B') to SpanStyle(color = MaterialTheme.colors.grey_500_grey_400)
        ),
        color = MaterialTheme.colors.black_white,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 3.dp)
            .testTag("Description")
    )
}

@Composable
private fun NotificationDate(
    notification: Notification,
) {
    Text(text = notification.dateText(LocalContext.current),
        modifier = Modifier
            .padding(start = 16.dp, top = 5.dp, bottom = 12.dp)
            .testTag("DateText"),
        color = if (MaterialTheme.colors.isLight) grey_alpha_054 else white_alpha_054,
        style = MaterialTheme.typography.caption,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis)
}

@Composable
private fun NotificationDivider(horizontalPadding: Int) {
    var modifier: Modifier = Modifier
    if (horizontalPadding != 0) {
        modifier = modifier.padding(horizontal = intToDp(px = horizontalPadding))
    }
    Divider(modifier = modifier,
        color = if (MaterialTheme.colors.isLight) grey_alpha_012 else white_alpha_012,
        thickness = 1.dp)
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES, name = "PreviewNotificationItemViewDark")
@Composable
private fun PreviewNotificationItemView() {
    val notification = Notification(
        sectionTitle = { "CONTACTS" },
        sectionColour = R.color.orange_400_orange_300,
        sectionIcon = null,
        title = { "New Contact" },
        titleTextSize = 16.sp,
        description = { "xyz@gmail.com is now a contact" },
        schedMeetingNotification = null,
        dateText = { "11 October 2022 6:46 pm" },
        isNew = true,
        backgroundColor = { "#D3D3D3" },
        separatorMargin = { 0 }
    ) {}
    NotificationItemView(modifier = Modifier,
        notification, position = 0, notifications = listOf(notification), onClick = {})

}