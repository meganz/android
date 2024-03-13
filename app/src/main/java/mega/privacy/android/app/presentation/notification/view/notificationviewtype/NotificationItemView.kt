package mega.privacy.android.app.presentation.notification.view.notificationviewtype

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.view.components.GreenIconView
import mega.privacy.android.app.presentation.notification.view.components.NotificationDate
import mega.privacy.android.app.presentation.notification.view.components.NotificationSchedMeetingView
import mega.privacy.android.core.ui.controls.dividers.DividerType
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.grey_500_grey_400
import mega.privacy.android.core.ui.theme.extensions.grey_900_grey_100
import mega.privacy.android.legacy.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
internal fun NotificationItemView(
    modifier: Modifier,
    notification: Notification,
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
        .testTag(NOTIFICATION_TEST_TAG)
        .fillMaxWidth()
        .wrapContentHeight()) {

        Text(
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                .testTag(NOTIFICATION_SECTION_TITLE_TEST_TAG),
            color = colorResource(id = notification.sectionColour),
            text = notification.sectionTitle(LocalContext.current),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        val description = notification.description(LocalContext.current)

        NotificationTitleRow(
            notification.title,
            notification.titleTextSize,
            notification.isNew,
            !description.isNullOrBlank()
        )

        if (!description.isNullOrBlank()) {
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
                    .testTag(NOTIFICATION_DESCRIPTION_TEST_TAG)
            )
        }

        if (notification.schedMeetingNotification?.scheduledMeeting != null) {
            NotificationSchedMeetingView(notification.schedMeetingNotification)
        }
        NotificationDate(
            dateText = notification.dateText(LocalContext.current),
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, bottom = 12.dp)
                .testTag(NOTIFICATION_DATE_TEST_TAG)
        )
        MegaDivider(
            modifier = Modifier.testTag(NOTIFICATION_DIVIDER),
            dividerType = DividerType.FullSize
        )
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
            .testTag(NOTIFICATION_TITLE_ROW_TEST_TAG)
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
                .testTag(NOTIFICATION_TITLE_ROW_TITLE_TEST_TAG)
        )

        if (showNewIcon) {
            GreenIconView(
                greenIconLabelRes = R.string.new_label_notification_item,
                modifier = Modifier.testTag(NOTIFICATION_GREEN_ICON_TEST_TAG)
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun NotificationItemViewPreview() {
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
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        NotificationItemView(modifier = Modifier,
            notification, onClick = {})
    }
}

internal const val NOTIFICATION_DIVIDER = "notification_item_view:mega_divider"
internal const val NOTIFICATION_DESCRIPTION_TEST_TAG = "notification_item_view:description_text"
internal const val NOTIFICATION_TITLE_ROW_TITLE_TEST_TAG = "notification_title_row:title_text"
internal const val NOTIFICATION_TITLE_ROW_TEST_TAG = "notification_title_row"
internal const val NOTIFICATION_SECTION_TITLE_TEST_TAG =
    "notification_item_view:section_title"
internal const val NOTIFICATION_TEST_TAG = "notification_item_view"
internal const val NOTIFICATION_DATE_TEST_TAG =
    "notification_item_view:notification_date:date_text"
internal const val NOTIFICATION_GREEN_ICON_TEST_TAG =
    "notification_item_view:notification_title_row:new_text"
