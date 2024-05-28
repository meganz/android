package mega.privacy.android.shared.original.core.ui.controls.buttons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

/**
 * Button with icon
 *
 * @param icon      Button icon
 * @param onClick   Action to perform tapping the button
 * @param modifier  Modifier
 * @param iconColor Icon color
 * @param enabled   [Boolean] indicating if the button is enabled or not
 */
@Composable
fun MegaButtonWithIcon(
    modifier: Modifier = Modifier,
    iconColor: Color,
    @DrawableRes icon: Int? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Column(
        modifier
            .testTag(TEST_TAG_MEGA_BUTTON_WITH_ICON)
            .clickable(enabled = enabled) { onClick() }) {
        icon?.let {
            Image(
                painter = painterResource(it),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                colorFilter = ColorFilter.tint(color = if (enabled) iconColor else MegaOriginalTheme.colors.icon.disabled)
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun MegaButtonWithIconPreview(
    @PreviewParameter(BooleanProvider::class) enabled: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MegaButtonWithIcon(
            iconColor = Color.Cyan,
            icon = R.drawable.ic_info,
            onClick = {},
            enabled = enabled,
        )
    }
}

internal const val TEST_TAG_MEGA_BUTTON_WITH_ICON =
    "mega_button_with_icon"

