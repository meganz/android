package mega.privacy.android.shared.original.core.ui.controls.banners

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor


@Composable
private fun InlineBaseBanner(
    title: String,
    message: String,
    actionButtonText: String,
    onActionButtonClick: () -> Unit,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onCloseClick: (() -> Unit)? = null,
    iconResId: Int = R.drawable.ic_info,
    backgroundColor: Color = MegaOriginalTheme.colors.notifications.notificationWarning,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = backgroundColor)
            .padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
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
                        end = 16.dp
                    )
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)

            ) {
                MegaText(
                    text = title,
                    textColor = TextColor.Primary,
                    style = MaterialTheme.typography.subtitle1medium
                )

                MegaText(
                    text = message,
                    textColor = TextColor.Primary,
                    style = MaterialTheme.typography.body2
                )

                TextButton(
                    modifier = modifier,
                    onClick = onActionButtonClick,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = MegaOriginalTheme.colors.text.accent,
                        disabledBackgroundColor = Color.Transparent,
                        disabledContentColor = MegaOriginalTheme.colors.text.disabled,
                    ),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = actionButtonText,
                        textDecoration = TextDecoration.Underline,
                        color = MegaOriginalTheme.colors.support.info,
                        style = MaterialTheme.typography.button.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            onCloseClick?.let {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onCloseClick() }
                        .testTag(INLINE_BANNER_CLOSE_TEST_TAG),
                    painter = painterResource(id = R.drawable.ic_universal_close),
                    tint = MegaOriginalTheme.colors.icon.primary,
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
    actionButtonText: String,
    onActionButtonClick: () -> Unit,
    onCloseClick: (() -> Unit),
    modifier: Modifier = Modifier,
) {
    InlineBaseBanner(
        title = title,
        message = message,
        actionButtonText = actionButtonText,
        onActionButtonClick = onActionButtonClick,
        onCloseClick = onCloseClick,
        modifier = modifier,
        iconResId = mega.privacy.android.icon.pack.R.drawable.ic_alert_circle_regular_medium_outline,
        iconColor = MegaOriginalTheme.colors.support.warning,
        backgroundColor = MegaOriginalTheme.colors.notifications.notificationWarning
    )
}

/**
 * Inline banner with error style
 */
@Composable
fun InlineErrorBanner(
    title: String,
    message: String,
    actionButtonText: String,
    onActionButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InlineBaseBanner(
        title = title,
        message = message,
        actionButtonText = actionButtonText,
        onActionButtonClick = onActionButtonClick,
        modifier = modifier,
        iconResId = mega.privacy.android.icon.pack.R.drawable.ic_warning_icon,
        iconColor = MegaOriginalTheme.colors.support.error,
        backgroundColor = MegaOriginalTheme.colors.notifications.notificationError
    )
}

@CombinedThemePreviews
@Composable
internal fun InlineErrorPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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
