package mega.privacy.android.feature.fileinfo.presentation.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.chip.DefaultChipStyle
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack

/**
 * The tags section: a "Tags" header with an edit chevron (when [canEdit]) and a wrapped list of tag
 * chips. Tapping the section (when editable) opens the tags editor.
 *
 * @param tags the node tags
 * @param canEdit whether the current user can edit the tags
 * @param onClick invoked when the section is tapped (only wired when [canEdit])
 * @param modifier modifier for the section
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagsSection(
    tags: List<String>,
    canEdit: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (canEdit) Modifier.clickable(onClick = onClick) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MegaText(
                modifier = Modifier.weight(1f),
                // TODO extract to a localized string resource
                text = "Tags",
                textColor = TextColor.Primary,
                style = AppTheme.typography.bodyLarge,
            )
            if (canEdit) {
                MegaIcon(
                    modifier = Modifier.size(24.dp),
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ChevronRight),
                    tint = IconColor.Secondary,
                    contentDescription = null,
                )
            }
        }
        if (tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { tag ->
                    MegaChip(
                        selected = false,
                        content = "#$tag",
                        style = DefaultChipStyle,
                        onClick = onClick,
                    )
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun TagsSectionPreview() {
    AndroidThemeForPreviews {
        TagsSection(
            tags = listOf("marketing", "2026", "confidential"),
            canEdit = true,
            onClick = {},
        )
    }
}
