package mega.privacy.android.core.nodecomponents.components.selectionmode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.toolbar.MegaFloatingToolbar
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.core.nodecomponents.sheet.nodeactions.NodeMoreOptionsBottomSheet
import mega.privacy.android.domain.entity.node.TypedNode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeSelectionModeBottomBar(
    availableActions: List<MenuActionWithIcon>,
    visibleActions: List<MenuActionWithIcon>,
    visible: Boolean,
    nodeActionHandler: NodeActionHandler,
    selectedNodes: List<TypedNode>,
    isSelecting: Boolean,
    modifier: Modifier = Modifier,
    onActionPressed: (MenuActionWithIcon) -> Unit = {},
) {
    var showMoreBottomSheet by rememberSaveable { mutableStateOf(false) }

    SelectionModeBottomBar(
        visible = visible,
        actions = visibleActions,
        modifier = modifier,
        actionsEnabled = !isSelecting,
        onActionPressed = { action ->
            onActionPressed(action)

            if (action is NodeSelectionAction.More) {
                showMoreBottomSheet = true
                return@SelectionModeBottomBar
            }

            nodeActionHandler(action, selectedNodes)
        }
    )

    if (showMoreBottomSheet) {
        NodeMoreOptionsBottomSheet(
            actions = availableActions.filterNot { it is NodeSelectionAction.More },
            sheetState = rememberModalBottomSheetState(),
            onDismissRequest = {
                showMoreBottomSheet = false
            },
            onActionPressed = { action ->
                onActionPressed(action)
                nodeActionHandler(action, selectedNodes)
                showMoreBottomSheet = false
            }
        )
    }
}

@Composable
fun SelectionModeBottomBar(
    visible: Boolean,
    actions: List<MenuActionWithIcon>,
    modifier: Modifier = Modifier,
    actionsEnabled: Boolean = true,
    onActionPressed: (MenuActionWithIcon) -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 300),
            initialOffsetY = { it }
        ),
        exit = ExitTransition.None
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            MegaFloatingToolbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                actions = actions,
                actionsEnabled = actionsEnabled,
                onActionPressed = onActionPressed
            )
        }
    }
}
