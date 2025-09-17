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
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.icon.pack.IconPack

@Composable
internal fun NodeMoreOptionsBottomSheet(
    actions: List<MenuActionWithIcon>,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit = {},
    onActionPressed: ((MenuActionWithIcon) -> Unit)? = null,
    onHelpClicked: ((MenuActionWithIcon) -> Unit)? = null,
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
                    if (action is HideMenuAction) {
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
    action: MenuActionWithIcon,
    contentDescription: String,
) {
    if (action is TrashMenuAction) {
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
    action: MenuActionWithIcon,
    contentDescription: String,
    onHelpClicked: ((MenuActionWithIcon) -> Unit)?,
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
private fun MenuActionWithIcon.getTextColor(): TextColor = when (this) {
    is TrashMenuAction -> TextColor.Error
    else -> TextColor.Primary
}

@Composable
@CombinedThemePreviews
private fun NodeMoreOptionsBottomSheetPreview() {
    val previewActions = listOf(
        DownloadMenuAction(),
        ManageLinkMenuAction(),
        HideMenuAction(),
        MoveMenuAction(),
        CopyMenuAction(),
        TrashMenuAction()
    )

    AndroidThemeForPreviews {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        LaunchedEffect(Unit) {
            sheetState.show()
        }

        NodeMoreOptionsBottomSheet(
            actions = previewActions,
            sheetState = sheetState,
        )
    }
}

internal const val HELP_ICON_TAG = "NodeMoreOptionsBottomSheet:help_icon"
internal const val ERROR_NODE_ICON_TAG = "NodeMoreOptionsBottomSheet:error_node_icon"
internal const val NODE_ICON_TAG = "NodeMoreOptionsBottomSheet:node_icon"