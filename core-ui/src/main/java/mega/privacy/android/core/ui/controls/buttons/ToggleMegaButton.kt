package mega.privacy.android.core.ui.controls.buttons

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.core.R

/**
 * Mega custom implementation of [IconToggleButton]
 *
 * @param modifier
 * @param title
 * @param enable
 * @param enabledIcon
 * @param disabledIcon
 * @param onCheckedChange
 */
@Composable
fun ToggleMegaButton(
    modifier: Modifier,
    title: String,
    enable: Boolean,
    @DrawableRes enabledIcon: Int,
    @DrawableRes disabledIcon: Int,
    onCheckedChange: (Boolean) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val iconRes: Int
    val tintColor: Color
    val backgroundColor: Color
    if (enable) {
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
            checked = enable,
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

@Preview
@Composable
internal fun PreviewEnabledToggleMegaButton() {
    ToggleMegaButton(
        modifier = Modifier,
        title = "Mic",
        enable = true,
        enabledIcon = R.drawable.ic_waiting_room_mic_on,
        disabledIcon = R.drawable.ic_waiting_room_mic_off,
    ) {}
}

@Preview
@Composable
internal fun PreviewDisabledToggleMegaButton() {
    ToggleMegaButton(
        modifier = Modifier,
        title = "Mic",
        enable = false,
        enabledIcon = R.drawable.ic_waiting_room_mic_on,
        disabledIcon = R.drawable.ic_waiting_room_mic_off,
    ) {}
}
