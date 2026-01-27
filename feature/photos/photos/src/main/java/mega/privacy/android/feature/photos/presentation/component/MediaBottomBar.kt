package mega.privacy.android.feature.photos.presentation.component

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.action.MultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.components.selectionmode.SelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.photos.presentation.handler.MediaSelectionModeType

@SuppressLint("ComposeUnstableCollections")
@Composable
internal fun MediaBottomBar(
    selectionModeType: MediaSelectionModeType,
    nodeActionUiState: NodeActionState,
    timelineActions: List<MenuActionWithIcon>,
    albumsActions: List<MenuActionWithIcon>,
    selectedVideoNodes: List<TypedNode>,
    multiNodeActionHandler: MultiNodeActionHandler,
    onActionPressed: (mode: MediaSelectionModeType, action: MenuActionWithIcon) -> Unit,
) {
    // We don’t use a when condition here because it would cause us to lose the animation for both bottom bars.
    SelectionModeBottomBar(
        visible = selectionModeType == MediaSelectionModeType.Timeline || selectionModeType == MediaSelectionModeType.Albums,
        actions = when (selectionModeType) {
            MediaSelectionModeType.Timeline -> timelineActions
            MediaSelectionModeType.Albums -> albumsActions
            else -> emptyList()
        },
        onActionPressed = { onActionPressed(selectionModeType, it) }
    )

    NodeSelectionModeBottomBar(
        availableActions = nodeActionUiState.availableActions,
        visibleActions = nodeActionUiState.visibleActions,
        visible = nodeActionUiState.visibleActions.isNotEmpty() && selectionModeType == MediaSelectionModeType.Videos,
        multiNodeActionHandler = multiNodeActionHandler,
        selectedNodes = selectedVideoNodes,
        isSelecting = false,
        onActionPressed = { onActionPressed(selectionModeType, it) }
    )
}
