package mega.privacy.android.feature.transfers.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.icon.pack.IconPack

/**
 * Selected Transfer Icon
 * @param isSelected
 */
@Composable
fun SelectedTransferIcon(
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) =
    MegaIcon(
        rememberVectorPainter(if (isSelected) IconPack.Medium.Thin.Solid.CheckSquare else IconPack.Medium.Thin.Outline.Square),
        contentDescription = if (isSelected) "Selected" else "Unselected",
        tint = IconColor.Primary,
        modifier = modifier
            .size(24.dp)
            .testTag(TEST_TAG_TRANSFER_SELECTED)
    )

/**
 * Tag for selected transfer check icon.
 */
internal const val TEST_TAG_TRANSFER_SELECTED = "$TEST_TAG_ACTIVE_TAB:selected_icon"