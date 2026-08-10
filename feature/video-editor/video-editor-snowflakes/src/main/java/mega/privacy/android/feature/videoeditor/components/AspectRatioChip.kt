package mega.privacy.android.feature.videoeditor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
 * Aspect-ratio preset chip for the Crop tool: a small ratio glyph + label that
 * highlights with the brand colour when selected. Decoupled from any preset
 * type — the caller supplies primitive props.
 */
@Composable
fun AspectRatioChip(
    label: String,
    aspectRatio: Float?,
    isFree: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = if (selected) DSTokens.colors.brand.default else DSTokens.colors.text.secondary
    val background =
        if (selected) DSTokens.colors.brand.default.copy(alpha = 0.2f) else DSTokens.colors.background.surface3
    val borderColor = if (selected) DSTokens.colors.brand.default else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .width(64.dp),
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            val ratio = aspectRatio ?: (16f / 9f)
            val shapeW: Float
            val shapeH: Float
            if (ratio >= 1f) {
                shapeW = 24f
                shapeH = 24f / ratio
            } else {
                shapeH = 24f
                shapeW = 24f * ratio
            }
            Box(
                modifier = Modifier
                    .width(shapeW.dp)
                    .height(shapeH.dp)
                    .background(accent.copy(alpha = 0.85f), RoundedCornerShape(2.dp))
                    .border(
                        if (isFree) 1.dp else 0.dp,
                        if (isFree) accent else Color.Transparent,
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
        MegaText(
            text = label,
            style = AppTheme.typography.labelSmall,
            textColor = if (selected) TextColor.Brand else TextColor.Secondary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AspectRatioChipPreview() {
    AndroidThemeForPreviews {
        AspectRatioChip(
            label = "16:9",
            aspectRatio = 16f / 9f,
            isFree = false,
            selected = true,
            onClick = {},
        )
    }
}
