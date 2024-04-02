package mega.privacy.android.app.presentation.notification.view.notificationviewtype

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.notification.view.components.GreenIconView
import mega.privacy.android.app.presentation.notification.view.components.NotificationDate
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.core.ui.controls.dividers.DividerType
import mega.privacy.android.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.body2medium
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_800
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.domain.entity.notifications.PromoNotification
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
internal fun PromoNotificationItemView(
    modifier: Modifier,
    notification: PromoNotification,
    onClick: () -> Unit,
) {
    val hasPreview = notification.imageURL.isNotBlank()
    val hasIcon = notification.iconURL.isNotBlank()
    val description = notification.description
    val timeText = TimeUtils.formatTime(notification.endTimeStamp)
    val dateText = TimeUtils.formatDate(
        notification.endTimeStamp,
        TimeUtils.DATE_MM_DD_YYYY_FORMAT,
        LocalContext.current,
    )
    val dateAndTimeString = stringResource(
        id = R.string.notifications_screen_notification_promo_expiration_time,
        dateText,
        timeText
    )

    Column(modifier = modifier
        .clickable { onClick() }
        .background(
            color = MaterialTheme.colors.grey_020_grey_800
        )
        .testTag(PROMO_NOTIFICATION_TEST_TAG)
        .fillMaxWidth()
        .wrapContentHeight()) {

        MegaText(
            text = stringResource(id = R.string.notifications_screen_notification_section_title),
            textColor = TextColor.Accent,
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                .testTag(PROMO_NOTIFICATION_SECTION_TITLE_TEST_TAG)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 16.dp, end = 60.dp)
            ) {

                MegaText(
                    text = notification.title,
                    textColor = TextColor.Primary,
                    style = MaterialTheme.typography.body2medium,
                    modifier = Modifier.testTag(PROMO_NOTIFICATION_TITLE_TEST_TAG)
                )
                if (description.isNotBlank()) {
                    MegaText(
                        text = description,
                        textColor = TextColor.Secondary,
                        style = MaterialTheme.typography.body2medium,
                        overflow = LongTextBehaviour.Clip(),
                        modifier = Modifier
                            .padding(end = 16.dp, top = 4.dp)
                            .testTag(
                                PROMO_NOTIFICATION_DESCRIPTION_TEST_TAG
                            )
                    )
                }
                if (!hasPreview) {
                    NotificationDate(
                        dateText = dateAndTimeString,
                        modifier = Modifier
                            .padding(top = 5.dp, bottom = 12.dp)
                            .testTag(
                                PROMO_NOTIFICATION_DATE_WITH_NO_PREVIEW_TEST_TAG
                            )
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(end = 16.dp)
            ) {
                GreenIconView(
                    greenIconLabelRes = R.string.notifications_screen_notification_label_promo,
                    modifier = Modifier.testTag(PROMO_NOTIFICATION_GREEN_ICON_TEST_TAG)
                )
                if (!hasPreview) {
                    Spacer(modifier = Modifier.padding(4.dp))
                    if (hasIcon) {
                        Image(
                            painter = rememberAsyncImagePainter(model = notification.iconURL),
                            contentDescription = "Promo notification Icon",
                            modifier = Modifier
                                .size(48.dp)
                                .testTag(PROMO_NOTIFICATION_ICON_TEST_TAG)
                        )
                    }
                }
            }
        }

        if (hasPreview) {
            Image(
                painter = rememberAsyncImagePainter(model = notification.imageURL),
                contentDescription = "Promo preview Image",
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .height(115.dp)
                    .testTag(PROMO_PREVIEW_TEST_TAG),
                contentScale = ContentScale.Fit,
            )
            NotificationDate(
                dateText = dateAndTimeString,
                modifier = Modifier
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp)
                    .testTag(
                        PROMO_NOTIFICATION_DATE_WITH_PREVIEW_TEST_TAG
                    )
            )
        }
        MegaDivider(
            modifier = Modifier.testTag(PROMO_NOTIFICATION_DIVIDER),
            dividerType = DividerType.FullSize
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PromoNotificationItemPreview(
    @PreviewParameter(BooleanProvider::class) hasPreview: Boolean,
) {
    val notification = PromoNotification(
        promoID = 1,
        title = "Title",
        description = "Description",
        iconURL = "https://www.pngkey.com/png/detail/137-1377870_canvas-sample-sample-image-url.png",
        imageURL = if (hasPreview) "https://www.pngkey.com/png/detail/137-1377870_canvas-sample-sample-image-url.png" else "",
        startTimeStamp = 1,
        endTimeStamp = if (hasPreview) 1712053179 else 1743668379,
        actionName = "Action name",
        actionURL = "https://www.mega.co.nz",
        isNew = hasPreview
    )
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        PromoNotificationItemView(
            modifier = Modifier,
            notification = notification,
        ) {}
    }
}

internal const val PROMO_NOTIFICATION_DIVIDER = "promo_notification_item_view:mega_divider"
internal const val PROMO_NOTIFICATION_TEST_TAG = "promo_notification_item_view"
internal const val PROMO_NOTIFICATION_GREEN_ICON_TEST_TAG = "promo_notification_item_view:promo_tag"
internal const val PROMO_NOTIFICATION_TITLE_TEST_TAG =
    "promo_notification_item_view:title"
internal const val PROMO_NOTIFICATION_DESCRIPTION_TEST_TAG =
    "promo_notification_item_view:description"
internal const val PROMO_NOTIFICATION_SECTION_TITLE_TEST_TAG =
    "promo_notification_item_view:section_title"
internal const val PROMO_NOTIFICATION_DATE_WITH_NO_PREVIEW_TEST_TAG =
    "promo_notification_item_view:promo_notification_date_with_no_preview:date_text"
internal const val PROMO_NOTIFICATION_DATE_WITH_PREVIEW_TEST_TAG =
    "promo_notification_item_view:promo_notification_date_with_preview:date_text"
internal const val PROMO_NOTIFICATION_ICON_TEST_TAG = "promo_notification_item_view:icon_promo"
internal const val PROMO_PREVIEW_TEST_TAG = "promo_notification_item_view:preview"