package mega.privacy.android.app.presentation.notification.view

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.text.TextUtils
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.utils.StyleUtils.setTextStyle
import mega.privacy.android.presentation.theme.grey_alpha_012
import mega.privacy.android.presentation.theme.grey_alpha_054
import mega.privacy.android.presentation.theme.grey_alpha_087
import mega.privacy.android.presentation.theme.white_alpha_012
import mega.privacy.android.presentation.theme.white_alpha_054
import mega.privacy.android.presentation.theme.white_alpha_087

@Composable
internal fun NotificationItemView(modifier: Modifier, notification: Notification) {
    Column(modifier = modifier
        .testTag("NotificationItemView")
        .fillMaxWidth()
        .wrapContentHeight()) {

        NotificationSectionRow(notification)

        val showDescription = notification.description(LocalContext.current) != null
        NotificationTitleRow(showDescription, notification)

        if (showDescription) {
            NotificationDescription(notification)
        }

        NotificationDate(notification)
        NotificationDivider()
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
    showDescription: Boolean,
    notification: Notification,
) {
    val titleMaxLines = if (showDescription) 1 else 3
    val titleText = notification.title(LocalContext.current)

    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp)) {

        AndroidView(
            factory = { context ->
                TextView(context).apply {
                    maxLines = titleMaxLines
                    ellipsize = TextUtils.TruncateAt.END
                    setTextStyle(
                        textAppearance = R.style.TextAppearance_Mega_Subtitle1_Medium_Variant,
                    )
                    setTextSize(TypedValue.COMPLEX_UNIT_DIP, notification.titleTextSize)
                }
            },
            update = { it.text = titleText },
            modifier = Modifier
                .padding(top = 2.dp)
                .weight(1f)
                .testTag("Title")
        )

        if (notification.isNew) {
            Box(modifier = Modifier
                .padding(start = 12.dp)
                .background(color = MaterialTheme.colors.secondary,
                    shape = RoundedCornerShape(12.dp))) {
                Text(text = stringResource(id = R.string.new_label_notification_item),
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .testTag("IsNew"),
                    color = if (MaterialTheme.colors.isLight) white_alpha_087 else grey_alpha_087,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun NotificationDescription(
    notification: Notification,
) {
    val descriptionText = notification.description(LocalContext.current)
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                maxLines = 3
                ellipsize = TextUtils.TruncateAt.END
                setTextStyle(
                    textAppearance = R.style.TextAppearance_Mega_Subtitle2_Medium,
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            }
        },
        update = { it.text = descriptionText },
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
private fun NotificationDivider() {
    Divider(color = if (MaterialTheme.colors.isLight) grey_alpha_012 else white_alpha_012,
        thickness = 1.dp)
}


@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES, name = "PreviewNotificationItemViewDark")
@Composable
private fun PreviewNotificationItemView() {
    NotificationItemView(modifier = Modifier,
        notification = Notification(sectionTitle = { "CONTACTS" },
            sectionColour = R.color.orange_400_orange_300,
            sectionIcon = null,
            title = { "New Contact" },
            titleTextSize = 16f,
            description = { "xyz@gmail.com is now a contact" },
            dateText = { "11 October 2022 6:46 pm" },
            isNew = true,
            onClick = {}))

}