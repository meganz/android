package mega.privacy.android.feature.videoeditor.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Icon + label tile used by the Rotate tool's action row (rotate left/right,
 * flip). Highlights with the brand colour when [selected]. Decoupled from any
 * action type — the caller supplies the icon and click handler.
 */
@Composable
fun RotateTile(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background =
        if (selected) DSTokens.colors.brand.default.copy(alpha = 0.2f) else DSTokens.colors.background.surface3
    val borderColor = if (selected) DSTokens.colors.brand.default else Color.Transparent
    Column(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MegaIcon(
            imageVector = icon,
            tint = if (selected) IconColor.Brand else IconColor.Primary,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(6.dp))
        MegaText(
            text = label,
            style = AppTheme.typography.labelMedium,
            textColor = if (selected) TextColor.Brand else TextColor.Primary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RotateTilePreview() {
    AndroidThemeForPreviews {
        RotateTile(icon = Icons.Filled.Flip, label = "Flip", selected = true, onClick = {})
    }
}
