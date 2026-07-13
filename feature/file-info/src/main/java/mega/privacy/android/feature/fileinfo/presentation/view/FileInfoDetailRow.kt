package mega.privacy.android.feature.fileinfo.presentation.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack

/**
 * A label/value detail row used throughout the File Info screen
 *
 * @param label the row label
 * @param value the row value
 * @param modifier modifier for the row
 * @param trailingIcon optional icon shown at the end of the row
 * @param onClick optional click handler; when set the whole row is clickable
 */
@Composable
internal fun FileInfoDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MegaText(
                text = label,
                textColor = TextColor.Primary,
                style = AppTheme.typography.bodyLarge,
            )
            MegaText(
                text = value,
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
            )
        }
        if (trailingIcon != null) {
            MegaIcon(
                modifier = Modifier.size(24.dp),
                painter = rememberVectorPainter(trailingIcon),
                tint = IconColor.Secondary,
                contentDescription = null,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun FileInfoDetailRowPreview() {
    AndroidThemeForPreviews {
        FileInfoDetailRow(
            label = "Added",
            value = "Jun 22, 2026, 17:27",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FileInfoDetailRowWithTrailingIconPreview() {
    AndroidThemeForPreviews {
        FileInfoDetailRow(
            label = "Location",
            value = "Cloud drive > Marketing",
            trailingIcon = IconPack.Medium.Thin.Outline.FolderSearch,
            onClick = {},
        )
    }
}
