package mega.privacy.android.app.presentation.notification.view.notificationviewtype

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.notification.model.Notification
import mega.privacy.android.app.presentation.notification.view.components.GreenIconView
import mega.privacy.android.app.presentation.notification.view.components.NotificationDivider
import mega.privacy.android.app.presentation.notification.view.components.getHorizontalPaddingForDivider
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.drawableId
import mega.privacy.android.core.ui.controls.text.LongTextBehaviour
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.body2medium
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
internal fun PromoNotificationItemView(
    notification: Notification,
    position: Int,
    hasBanner: Boolean,
    notifications: List<Notification>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        .fillMaxWidth()
        .wrapContentHeight()) {

        MegaText(
            text = stringResource(id = R.string.notifications_screen_notification_section_title),
            textColor = TextColor.Accent,
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, bottom = 16.dp)
                .testTag(PROMO_NOTIFICATION_SECTION_TITLE_TEST_TAG)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 16.dp, end = 60.dp)
            ) {
                val description = notification.description(LocalContext.current)

                val title = notification.title(LocalContext.current)
                MegaText(
                    text = title,
                    textColor = TextColor.Primary,
                    style = MaterialTheme.typography.body2medium,
                    modifier = Modifier.testTag(PROMO_NOTIFICATION_TITLE_TEST_TAG)
                )

                if (!description.isNullOrBlank()) {
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
                if (!hasBanner) {
                    PromoNotificationExpiryDate(
                        endDate = notification.dateText(LocalContext.current),
                        modifier = Modifier
                            .padding(top = 5.dp, bottom = 12.dp)
                            .testTag(
                                PROMO_NOTIFICATION_EXPIRY_DATE_WITH_NO_BANNER_TEST_TAG
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
                if (!hasBanner) {
                    Spacer(modifier = Modifier.padding(4.dp))
                    Image(painter = painterResource(id = R.drawable.ic_promo_notification),
                        contentDescription = "Promo notification Icon",
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { drawableId = R.drawable.ic_promo_notification }
                            .testTag(PROMO_NOTIFICATION_ICON_TEST_TAG)
                    )
                }
            }
        }

        if (hasBanner) {
            Image(painter = painterResource(id = R.drawable.storage_full_xl),
                contentDescription = "Promo banner Image",
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .semantics { drawableId = R.drawable.storage_full_xl }
                    .testTag(PROMO_BANNER_TEST_TAG)
            )

            PromoNotificationExpiryDate(
                endDate = notification.dateText(LocalContext.current),
                modifier = Modifier
                    .padding(start = 16.dp, top = 5.dp, bottom = 12.dp)
                    .testTag(
                        PROMO_NOTIFICATION_EXPIRY_DATE_WITH_BANNER_TEST_TAG
                    )
            )
        }

        val horizontalPadding =
            getHorizontalPaddingForDivider(notification, position, notifications)
        NotificationDivider(
            horizontalPadding = horizontalPadding
        )
    }
}

@Composable
private fun PromoNotificationExpiryDate(endDate: String, modifier: Modifier) {
    MegaText(
        text = endDate,
        textColor = TextColor.Secondary,
        overflow = LongTextBehaviour.Clip(),
        style = MaterialTheme.typography.caption,
        modifier = modifier
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewPromoNotificationItemView(
    @PreviewParameter(BooleanProvider::class) booleanParameter: Boolean,
) {
    val notification = Notification(
        sectionTitle = { "Limited time offer" },
        sectionColour = R.color.jade_600_jade_300,
        sectionIcon = null,
        title = { "Get Pro 1 for 50% off" },
        titleTextSize = 16.sp,
        description = { "Tap here to get 1 year of Pro 1 for 50% off" },
        schedMeetingNotification = null,
        dateText = { "11 October 2022 6:46 pm" },
        isNew = true,
        backgroundColor = { "#D3D3D3" },
        separatorMargin = { 0 }
    ) {}
    MegaAppTheme(isDark = booleanParameter) {
        PromoNotificationItemView(
            notification,
            position = 0,
            hasBanner = booleanParameter,
            notifications = listOf(notification),
            onClick = {})
    }
}

internal const val PROMO_NOTIFICATION_GREEN_ICON_TEST_TAG = "promo_notification_item_view:promo_tag"
internal const val PROMO_NOTIFICATION_TITLE_TEST_TAG =
    "promo_notification_item_view:title"
internal const val PROMO_NOTIFICATION_DESCRIPTION_TEST_TAG =
    "promo_notification_item_view:description"
internal const val PROMO_NOTIFICATION_SECTION_TITLE_TEST_TAG =
    "promo_notification_item_view:section_title"
internal const val PROMO_NOTIFICATION_EXPIRY_DATE_WITH_NO_BANNER_TEST_TAG =
    "promo_notification_item_view:promo_notification_expiry_date_with_no_banner:expiry_date_text"
internal const val PROMO_NOTIFICATION_EXPIRY_DATE_WITH_BANNER_TEST_TAG =
    "promo_notification_item_view:promo_notification_expiry_date_with_banner:expiry_date_text"
internal const val PROMO_NOTIFICATION_ICON_TEST_TAG = "promo_notification_item_view:icon_promo"
internal const val PROMO_BANNER_TEST_TAG = "promo_notification_item_view:banner"