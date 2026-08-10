package mega.privacy.android.feature.videoeditor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * A speed-multiplier chip for the Speed tool: a centred label that highlights
 * with the brand colour when [selected]. Decoupled — the caller supplies the
 * formatted label and the click handler.
 */
@Composable
fun SpeedChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background =
        if (selected) DSTokens.colors.brand.default.copy(alpha = 0.2f) else DSTokens.colors.background.surface3
    val borderColor = if (selected) DSTokens.colors.brand.default else Color.Transparent
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MegaText(
            text = label,
            style = AppTheme.typography.titleSmall,
            textColor = if (selected) TextColor.Brand else TextColor.Primary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SpeedChipPreview() {
    AndroidThemeForPreviews {
        SpeedChip(label = "2×", selected = true, onClick = {})
    }
}
