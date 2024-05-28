package mega.privacy.android.shared.original.core.ui.controls.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.conditional

/**
 * Mega checkbox
 *
 * @param checked
 * @param onCheckedChange
 * @param modifier
 * @param enabled
 */
@Composable
fun MegaCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    rounded: Boolean = true,
) {
    val imageVector = ImageVector.vectorResource(R.drawable.check)
    val tint = if (checked) MegaOriginalTheme.colors.icon.inverse else Color.Transparent
    val background =
        if (checked) MegaOriginalTheme.colors.components.selectionControl else Color.Transparent
    val borderColor =
        if (enabled) MegaOriginalTheme.colors.icon.secondary else MegaOriginalTheme.colors.border.disabled
    val shape = if (rounded) CircleShape else RoundedCornerShape(2.dp)

    IconButton(
        onClick = { onCheckedChange(!checked) },
        modifier = modifier
            .clip(shape)
            .size(32.dp),
        enabled = enabled
    ) {
        Icon(
            imageVector = imageVector,
            tint = tint,
            modifier = Modifier
                .size(20.dp)
                .background(background, shape = shape)
                .padding(2.dp)
                .conditional(!checked) {
                    border(
                        width = 1.dp,
                        color = borderColor,
                        shape = shape,
                    )
                },
            contentDescription = null,
        )
    }
}

/**
 * Preview [MegaCheckbox]
 */
@CombinedThemePreviews
@Composable
private fun PreviewMegaCheckboxRound(
    @PreviewParameter(BooleanProvider::class) isEnabled: Boolean,
) {
    Preview(checked = isEnabled, rounded = true)
}

/**
 * Preview [MegaCheckbox]
 */
@CombinedThemePreviews
@Composable
private fun PreviewMegaCheckboxSquare(
    @PreviewParameter(BooleanProvider::class) checked: Boolean,
) {
    Preview(checked = checked, rounded = false)
}

@Composable
private fun Preview(checked: Boolean, rounded: Boolean) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        var interactiveChecked by remember { mutableStateOf(checked) }
        MegaCheckbox(
            modifier = Modifier,
            checked = interactiveChecked,
            onCheckedChange = { interactiveChecked = !interactiveChecked },
            rounded = rounded
        )
    }
}

