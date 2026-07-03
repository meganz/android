package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack

/**
 * A single icon + label row used for plan benefits on the redesigned subscription page,
 * both in the "Why go Pro?" card and inside each plan card.
 *
 * @param icon leading icon
 * @param text label text
 * @param textColor color of the label, defaults to [TextColor.Primary]
 * @param textStyle style of the label, defaults to titleSmall
 * @param iconTint tint of the leading icon, defaults to [IconColor.Brand]
 */
@Composable
fun PlanFeatureRow(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    textColor: TextColor = TextColor.Primary,
    textStyle: TextStyle = MaterialTheme.typography.titleSmall,
    iconTint: IconColor = IconColor.Brand,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MegaIcon(
            painter = rememberVectorPainter(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp),
        )
        MegaText(
            text = text,
            style = textStyle,
            textColor = textColor,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PlanFeatureRowPreview() {
    AndroidTheme(isSystemInDarkTheme()) {
        PlanFeatureRow(
            icon = IconPack.Medium.Thin.Outline.Cloud,
            text = "2 TB cloud storage",
            modifier = Modifier.padding(16.dp),
        )
    }
}
