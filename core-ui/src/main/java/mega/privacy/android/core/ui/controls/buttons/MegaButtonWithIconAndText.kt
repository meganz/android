package mega.privacy.android.core.ui.controls.buttons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Button with icon and text
 *
 * @param icon      Button icon
 * @param text      Button text
 * @param onClick   Action to perform tapping the button
 * @param modifier  Modifier
 * @param iconColor Icon color
 * @param textColor Text color
 * @param enabled   [Boolean] indicating if the button is enabled or not
 */
@Composable
fun MegaButtonWithIconAndText(
    @DrawableRes icon: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconColor: Color = MegaTheme.colors.icon.primary,
    textColor: Color = MegaTheme.colors.text.primary,
    enabled: Boolean = true,
) {
    Column(modifier.clickable(enabled = enabled) { onClick() }) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .height(16.dp)
                .width(16.dp)
                .align(Alignment.CenterHorizontally),
            colorFilter = ColorFilter.tint(color = if (enabled) iconColor else MegaTheme.colors.icon.disabled)
        )
        Text(
            text = text,
            Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.caption.copy(color = if (enabled) textColor else MegaTheme.colors.text.disabled)
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MegaButtonWithIconAndTextPreview(
    @PreviewParameter(BooleanProvider::class) enabled: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaButtonWithIconAndText(
            icon = R.drawable.ic_info,
            text = "Info",
            onClick = {},
            enabled = enabled,
        )
    }
}