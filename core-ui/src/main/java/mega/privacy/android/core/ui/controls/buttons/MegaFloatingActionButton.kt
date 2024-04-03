package mega.privacy.android.core.ui.controls.buttons

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * MegaFloatingActionButton
 *
 * @param onClick click listener
 * @param modifier modifier
 * @param style style of the button
 * @param enabled whether the button is enabled
 * @param content content of the button
 */
@Composable
fun MegaFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FloatingActionButtonStyle = FloatingActionButtonStyle.Big,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    MegaFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        style = style,
        enabled = enabled,
        backgroundColor = MegaTheme.colors.button.primary,
        content = content,
    )
}

@Composable
internal fun MegaFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FloatingActionButtonStyle = FloatingActionButtonStyle.Big,
    enabled: Boolean = true,
    backgroundColor: Color = MegaTheme.colors.button.primary,
    iconTintColor: Color = MegaTheme.colors.icon.inverse,
    backgroundColorDisabled: Color = MegaTheme.colors.button.disabled,
    iconTintColorDisabled: Color = MegaTheme.colors.text.onColorDisabled,
    content: @Composable () -> Unit,
) {
    FloatingActionButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier.size(style.size),
        backgroundColor = if (enabled) backgroundColor else backgroundColorDisabled,
        shape = CircleShape,
        elevation = if (style.elevation && enabled) {
            FloatingActionButtonDefaults.elevation()
        } else {
            FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 6.dp,
                hoveredElevation = 4.dp,
                focusedElevation = 4.dp,
            )
        }
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) iconTintColor else iconTintColorDisabled,
            LocalContentAlpha provides 1f,
        ) {
            content()
        }
    }
}

/**
 * Style of the floating action button
 */
enum class FloatingActionButtonStyle(
    internal val size: Dp,
    internal val elevation: Boolean = true,
) {
    /**
     * Big floating action button
     */
    Big(56.dp),

    /**
     * Medium floating action button
     */
    Medium(48.dp),

    /**
     * Small floating action button
     */
    Small(40.dp),

    /**
     * Small floating action button without elevation
     */
    SmallWithoutElevation(40.dp, false)
}

@CombinedThemePreviews
@Composable
private fun MegaFloatingActionButtonPreview(
    @PreviewParameter(FloatingActionButtonPreviewProvider::class) styleEnabled: Pair<FloatingActionButtonStyle, Boolean>,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaFloatingActionButton(
            modifier = Modifier.padding(16.dp),
            style = styleEnabled.first,
            enabled = styleEnabled.second,
            onClick = {}) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.Default.Add,
                contentDescription = "",
            )
        }
    }
}

private class FloatingActionButtonPreviewProvider :
    PreviewParameterProvider<Pair<FloatingActionButtonStyle, Boolean>> {
    override val values =
        FloatingActionButtonStyle.entries.asSequence().flatMap { style ->
            sequenceOf(style to true, style to false)
        }
}

