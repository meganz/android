package mega.privacy.android.core.sharedcomponents.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.IconPack

/**
 * A circular shortcut button with an icon and a label below it, used by the
 * "Do more with MEGA" home section to render each shortcut.
 *
 * @param icon the icon shown inside the circular button
 * @param label the text shown below the button; also used as the icon content description
 * @param onClick invoked when the button is tapped
 * @param modifier the [Modifier] to be applied to this button
 */
@Composable
fun DoMoreWithMegaItemButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(100.dp)
            .wrapContentHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(DSTokens.colors.icon.accent),
            contentAlignment = Alignment.Center,
        ) {
            MegaIcon(
                imageVector = icon,
                contentDescription = label,
                tint = IconColor.Inverse,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center),
            )
        }
        MegaText(
            text = label,
            textColor = TextColor.Primary,
            style = AppTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun DoMoreWithMegaItemButtonPreview() {
    AndroidThemeForPreviews {
        DoMoreWithMegaItemButton(
            icon = IconPack.Medium.Thin.Outline.Camera,
            label = "Camera uploads",
            onClick = {},
        )
    }
}
