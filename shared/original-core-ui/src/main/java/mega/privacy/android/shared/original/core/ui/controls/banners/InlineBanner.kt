package mega.privacy.android.shared.original.core.ui.controls.banners

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme


@Composable
private fun InlineBaseBanner(
    title: String,
    message: String?,
    actionButtonText: String?,
    iconColor: Color,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.subtitle1,
    messageStyle: TextStyle = MaterialTheme.typography.body2,
    onActionButtonClick: (() -> Unit)? = null,
    onCloseClick: (() -> Unit)? = null,
    iconResId: Int = R.drawable.ic_info,
    backgroundColor: Color = DSTokens.colors.notifications.notificationWarning,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = backgroundColor)
            .padding(16.dp),
    ) {
        Row {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .testTag(INLINE_BANNER_HINT_ICON_TEST_TAG),
                painter = painterResource(id = iconResId),
                tint = iconColor,
                contentDescription = "Banner Icon"
            )

            Column(
                modifier = Modifier
                    .padding(
                        start = 8.dp,
                        top = 3.dp,
                        end = 16.dp
                    )
                    .weight(1f),
            ) {
                MegaText(
                    text = title,
                    textColor = TextColor.Primary,
                    style = titleStyle
                )

                Spacer(modifier = Modifier.size(8.dp))

                message?.let {
                    MegaText(
                        text = message,
                        textColor = TextColor.Primary,
                        style = messageStyle
                    )

                    Spacer(modifier = Modifier.size(4.dp))
                }

                actionButtonText?.let {
                    Box(
                        modifier = Modifier.clickable { onActionButtonClick?.invoke() }) {
                        Text(
                            modifier = Modifier.padding(
                                top = 4.dp,
                                bottom = 4.dp,
                                start = 0.dp,
                                end = 4.dp
                            ),
                            text = actionButtonText,
                            textDecoration = TextDecoration.Underline,
                            color = DSTokens.colors.support.info,
                            style = MaterialTheme.typography.button.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            onCloseClick?.let {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onCloseClick() }
                        .testTag(INLINE_BANNER_CLOSE_TEST_TAG),
                    painter = painterResource(id = R.drawable.ic_universal_close),
                    tint = DSTokens.colors.icon.primary,
                    contentDescription = "Banner Cancel"
                )
            }

        }
    }
}

/**
 * Inline banner with warning style
 */
@Composable
fun InlineWarningBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.subtitle1,
    messageStyle: TextStyle = MaterialTheme.typography.body2,
    actionButtonText: String? = null,
    onActionButtonClick: (() -> Unit)? = null,
    onCloseClick: (() -> Unit),
) {
    InlineBaseBanner(
        title = title,
        message = message,
        actionButtonText = actionButtonText,
        onActionButtonClick = onActionButtonClick,
        onCloseClick = onCloseClick,
        modifier = modifier,
        titleStyle = titleStyle,
        messageStyle = messageStyle,
        iconResId = mega.privacy.android.icon.pack.R.drawable.ic_alert_circle_medium_thin_outline,
        iconColor = DSTokens.colors.support.warning,
        backgroundColor = DSTokens.colors.notifications.notificationWarning
    )
}

/**
 * Inline banner with error style
 */
@Composable
fun InlineErrorBanner(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionButtonText: String? = null,
    onActionButtonClick: (() -> Unit)? = null,
    titleStyle: TextStyle = MaterialTheme.typography.subtitle1,
    messageStyle: TextStyle = MaterialTheme.typography.body2,
) {
    InlineBaseBanner(
        title = title,
        message = message,
        actionButtonText = actionButtonText,
        onActionButtonClick = onActionButtonClick,
        modifier = modifier,
        iconResId = mega.privacy.android.icon.pack.R.drawable.ic_warning_icon,
        iconColor = DSTokens.colors.support.error,
        backgroundColor = DSTokens.colors.notifications.notificationError,
        titleStyle = titleStyle,
        messageStyle = messageStyle,
    )
}

@CombinedThemePreviews
@Composable
internal fun InlineErrorPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        InlineErrorBanner(
            title = "This is title",
            message = "This is message for the banner",
            actionButtonText = "Action to click",
            onActionButtonClick = {},
            modifier = Modifier,
        )
    }
}

@CombinedThemePreviews
@Composable
internal fun InlineWarningPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        InlineWarningBanner(
            title = "Your storage is almost full",
            message = "To keep uploading data, upgrade to get more storage.",
            actionButtonText = "Upgrade now",
            onActionButtonClick = {},
            onCloseClick = {},
            modifier = Modifier,
        )
    }
}

const val INLINE_BANNER_HINT_ICON_TEST_TAG = "InlineBanner:icon_hint"
const val INLINE_BANNER_CLOSE_TEST_TAG = "InlineBanner:iconButton_close"
