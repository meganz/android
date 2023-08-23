package mega.privacy.android.core.ui.controls.buttons

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Mega custom implementation of [IconToggleButton]
 *
 * @param modifier          [Modifier]
 * @param checked           Whether or not the Button is currently checked
 * @param title             String title to show under the Button
 * @param enabledIcon       Icon to show when the button is not checked
 * @param disabledIcon      Icon to show when the button is checked
 * @param enabled           Whether or not the Button will handle input events and appear enabled
 * @param onCheckedChange   Callback to be invoked when this button is selected
 */
@Composable
fun ToggleMegaButton(
    modifier: Modifier,
    checked: Boolean,
    title: String,
    @DrawableRes enabledIcon: Int,
    @DrawableRes disabledIcon: Int,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val iconRes: Int
    val tintColor: Color
    val backgroundColor: Color
    if (checked) {
        iconRes = disabledIcon
        tintColor = MaterialTheme.colors.surface
        backgroundColor = MaterialTheme.colors.onSurface
    } else {
        iconRes = enabledIcon
        tintColor = MaterialTheme.colors.onSurface
        backgroundColor = MaterialTheme.colors.surface
    }
    val animatedTintColor by animateColorAsState(targetValue = tintColor, label = "tint")
    val animatedBackgroundColor by animateColorAsState(targetValue = backgroundColor, label = "bg")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconToggleButton(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { enabled ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(enabled)
            },
            modifier = modifier
                .size(48.dp)
                .border(1.dp, MaterialTheme.colors.onSurface, CircleShape)
                .background(animatedBackgroundColor, CircleShape)
                .testTag("toggle_mega_button:toggle")
        ) {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .testTag("toggle_mega_button:icon"),
                imageVector = ImageVector.vectorResource(iconRes),
                contentDescription = title,
                tint = animatedTintColor,
            )
        }

        Text(
            modifier = Modifier
                .padding(top = 4.dp)
                .testTag("toggle_mega_button:text"),
            text = title,
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.subtitle2.copy(
                fontSize = 10.sp
            ),
        )
    }
}

/**
 * Preview [ToggleMegaButton]
 */
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewEnabledToggleMegaButton(
    @PreviewParameter(BooleanProvider::class) isEnabled: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ToggleMegaButton(
            modifier = Modifier,
            checked = isEnabled,
            title = "Camera",
            enabledIcon = R.drawable.ic_universal_video_on,
            disabledIcon = R.drawable.ic_universal_video_off,
        ) {}
    }
}
