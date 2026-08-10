package mega.privacy.android.feature.videoeditor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/** A single tab in the [ToolTabBar]. */
@Immutable
data class ToolTabUiItem(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val selected: Boolean,
    val applied: Boolean,
)

/**
 * Bottom tab row of editor tools. Decoupled from the editor state: the caller
 * supplies [items] and is notified of selection by tool id via [onSelect]. The
 * selected tab's brand-tinted pill is the one part needing a brand colour, which
 * is why this lives in the snowflakes (design-token) module.
 */
@Composable
fun ToolTabBar(
    items: List<ToolTabUiItem>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxSurface(
        surfaceColor = SurfaceColor.Surface1,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                ToolTabItem(
                    item = item,
                    onClick = { onSelect(item.id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ToolTabItem(
    item: ToolTabUiItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlighted = item.selected || item.applied
    val iconTint = if (highlighted) IconColor.Brand else IconColor.Secondary
    val labelColor = if (highlighted) TextColor.Primary else TextColor.Secondary
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (item.selected) DSTokens.colors.brand.default.copy(alpha = 0.2f) else Color.Transparent,
                ),
            contentAlignment = Alignment.Center,
        ) {
            MegaIcon(
                imageVector = item.icon,
                tint = iconTint,
                contentDescription = item.label,
                modifier = Modifier.size(22.dp),
            )
        }
        MegaText(
            text = item.label,
            style = AppTheme.typography.labelMedium,
            textColor = labelColor,
        )
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun ToolTabBarPreview() {
    AndroidThemeForPreviews {
        ToolTabBar(
            items = listOf(
                ToolTabUiItem("trim", Icons.Filled.ContentCut, "Trim", selected = true, applied = false),
                ToolTabUiItem("crop", Icons.Filled.ContentCut, "Crop", selected = false, applied = true),
            ),
            onSelect = {},
        )
    }
}
