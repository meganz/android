package mega.privacy.android.core.ui.controls.banners

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Warning banner compose view
 * @param textString the banner text
 * @param onCloseClick lambda to be called when the close button is clicked, if null the close button won't be showed
 */
@Composable
fun WarningBanner(
    textString: String,
    onCloseClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) = WarningBanner(
    textComponent = {
        Text(text = textString)
    },
    onCloseClick = onCloseClick,
    modifier = modifier,
)

/**
 * Warning banner compose view
 * @param textComponent configurable text to use other components like MegaSpannedClickableText to render the banner text
 * @param onCloseClick lambda to be called when the close button is clicked, if null the close button won't be showed
 */
@Composable
fun WarningBanner(
    textComponent: @Composable () -> Unit,
    onCloseClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val textColour = MegaTheme.colors.textPrimary
    val buttonColor = MegaTheme.colors.iconPrimary
    ProvideTextStyle(
        MaterialTheme.typography.caption.copy(color = textColour)
    ) {
        if (onCloseClick == null) {
            BannerContent(null, buttonColor, textComponent, modifier)
        } else {
            //close button vertical position depends on banner size
            SubComposeBannerContent(
                textComponent = textComponent,
                onCloseButtonColor = buttonColor,
                onCloseClick = onCloseClick,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun SubComposeBannerContent(
    textComponent: @Composable () -> Unit,
    onCloseButtonColor: Color,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier) { constraints ->
        var placeable = subcompose("align-top") {
            BannerContent(
                onCloseClick = onCloseClick,
                tintColor = onCloseButtonColor,
                textComponent = textComponent,
                verticalAlignment = Alignment.Top
            )
        }.first().measure(constraints)
        if (placeable.height < 48.dp.toPx()) {
            // if the banner is small enough (typically one line) the close button should be vertically centered
            placeable = subcompose("align-center") {
                BannerContent(onCloseClick, onCloseButtonColor, textComponent)
            }.first().measure(constraints)
        }
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}

@Composable
private fun BannerContent(
    onCloseClick: (() -> Unit)?,
    tintColor: Color,
    textComponent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
) {
    Row(
        modifier = modifier
            .background(MegaTheme.colors.notificationWarning),
        verticalAlignment = verticalAlignment
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = HORIZONTAL_PADDING.dp,
                    bottom = VERTICAL_PADDING.dp,
                    top = VERTICAL_PADDING.dp
                )
        ) {
            textComponent()
        }

        if (onCloseClick == null) {
            Spacer(modifier = Modifier.width(HORIZONTAL_PADDING.dp))
        } else {
            Icon(
                modifier = Modifier
                    .testTag(TEST_TAG_WARNING_BANNER_CLOSE)
                    .clickable(onClick = onCloseClick)
                    .padding(horizontal = HORIZONTAL_PADDING.dp, vertical = VERTICAL_PADDING.dp),
                painter = painterResource(id = R.drawable.ic_remove_warning),
                tint = tintColor,
                contentDescription = "Close"
            )
        }
    }
}


@CombinedTextAndThemePreviews
@Composable
private fun WarningBannerPreview(
    @PreviewParameter(BooleanProvider::class) showCloseButton: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        WarningBanner(
            textString = "This is a warning banner",
            onCloseClick = if (showCloseButton) {
                {}
            } else null
        )
    }
}

@CombinedTextAndThemePreviews
@Composable
private fun WarningBannerLongTextPreview(
    @PreviewParameter(BooleanProvider::class) showCloseButton: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        WarningBanner(
            textString = "This is a warning banner with a very long text to preview multiline behaviour, still more text needed",
            onCloseClick = if (showCloseButton) {
                {}
            } else null
        )
    }
}

/**
 * test tag for close button
 */
const val TEST_TAG_WARNING_BANNER_CLOSE = "WarningBanner:iconButton_close"
private const val HORIZONTAL_PADDING = 16
private const val VERTICAL_PADDING = 14
