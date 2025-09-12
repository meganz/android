@file:OptIn(ExperimentalMaterial3Api::class)

package mega.privacy.android.core.nodecomponents.sheet.nodeactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.icon.pack.IconPack

@Composable
internal fun NodeMoreOptionsBottomSheet(
    actions: List<NodeSelectionAction>,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    onActionPressed: ((NodeSelectionAction) -> Unit)? = null,
    onHelpClicked: ((NodeSelectionAction) -> Unit)? = null,
) {
    MegaModalBottomSheet(
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        sheetState = sheetState,
        modifier = modifier,
        onDismissRequest = onDismissRequest
    ) {
        actions.distinct().forEach { action ->
            FlexibleLineListItem(
                modifier = Modifier
                    .testTag(action.testTag),
                title = action.getDescription(),
                titleTextColor = action.getTextColor(),
                leadingElement = {
                    NodeActionIcon(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp),
                        action = action,
                        contentDescription = action.getDescription()
                    )
                },
                trailingElement = {
                    if (action.showHelpButton()) {
                        HelpIcon(
                            action = action,
                            contentDescription = "Help for ${action.getDescription()}",
                            onHelpClicked = onHelpClicked
                        )
                    }
                },
                onClickListener = {
                    onActionPressed?.invoke(action)
                },
                enableClick = action.enabled
            )
        }
    }
}

@Composable
private fun NodeActionIcon(
    modifier: Modifier,
    action: NodeSelectionAction,
    contentDescription: String,
) {
    if (action is NodeSelectionAction.RubbishBin) {
        MegaIcon(
            modifier = modifier
                .testTag(ERROR_NODE_ICON_TAG)
                .size(24.dp),
            painter = action.getIconPainter(),
            contentDescription = contentDescription,
            textColorTint = TextColor.Error,
        )
    } else {
        MegaIcon(
            modifier = modifier
                .testTag(NODE_ICON_TAG)
                .size(24.dp),
            painter = action.getIconPainter(),
            contentDescription = contentDescription,
            tint = IconColor.Secondary,
        )
    }
}

@Composable
private fun HelpIcon(
    action: NodeSelectionAction,
    contentDescription: String,
    onHelpClicked: ((NodeSelectionAction) -> Unit)?,
) {
    MegaIcon(
        modifier = Modifier
            .size(24.dp)
            .testTag(HELP_ICON_TAG)
            .clickable { onHelpClicked?.invoke(action) },
        painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.HelpCircle),
        contentDescription = contentDescription,
        tint = IconColor.Secondary,
    )
}

@Composable
private fun NodeSelectionAction.getTextColor(): TextColor = when (this) {
    is NodeSelectionAction.RubbishBin -> TextColor.Error
    else -> TextColor.Primary
}

@Composable
private fun NodeSelectionAction.showHelpButton(): Boolean = when (this) {
    is NodeSelectionAction.Hide -> true
    else -> false
}

@Composable
@CombinedThemePreviews
private fun NodeMoreOptionsBottomSheetPreview() {
    AndroidThemeForPreviews {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        LaunchedEffect(Unit) {
            sheetState.show()
        }

        NodeMoreOptionsBottomSheet(
            actions = NodeSelectionAction.defaults,
            sheetState = sheetState,
        )
    }
}

internal const val HELP_ICON_TAG = "NodeMoreOptionsBottomSheet:help_icon"
internal const val ERROR_NODE_ICON_TAG = "NodeMoreOptionsBottomSheet:error_node_icon"
internal const val NODE_ICON_TAG = "NodeMoreOptionsBottomSheet:node_icon"