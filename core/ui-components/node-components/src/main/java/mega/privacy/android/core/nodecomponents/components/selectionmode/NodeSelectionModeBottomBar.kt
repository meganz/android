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
import androidx.hilt.navigation.compose.hiltViewModel
import mega.android.core.ui.components.toolbar.MegaFloatingToolbar
import mega.android.core.ui.model.TopAppBarAction
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.core.nodecomponents.sheet.nodeactions.NodeMoreOptionsBottomSheet
import mega.privacy.android.domain.entity.node.TypedNode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeSelectionModeBottomBar(
    availableActions: List<NodeSelectionAction>,
    visibleActions: List<NodeSelectionAction>,
    visible: Boolean,
    nodeActionHandler: NodeActionHandler,
    selectedNodes: List<TypedNode>,
    isSelecting: Boolean,
    modifier: Modifier = Modifier,
    viewModel: NodeSelectionModeBottomBarViewModel = hiltViewModel(),
    onActionPressed: (TopAppBarAction) -> Unit = {},
) {
    var showMoreBottomSheet by rememberSaveable { mutableStateOf(false) }

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
                actions = visibleActions,
                actionsEnabled = !isSelecting,
                onActionPressed = { action ->
                    onActionPressed(action)

                    if (action !is NodeSelectionAction) return@MegaFloatingToolbar

                    viewModel.handleAction(
                        action = action,
                        selectedNodes = selectedNodes,
                        nodeActionHandler = nodeActionHandler,
                        onMoreActionPressed = {
                            showMoreBottomSheet = true
                        },
                    )
                }
            )
        }
    }

    if (showMoreBottomSheet) {
        NodeMoreOptionsBottomSheet(
            actions = availableActions.filterNot { it is NodeSelectionAction.More },
            sheetState = rememberModalBottomSheetState(),
            onDismissRequest = {
                showMoreBottomSheet = false
            },
            onActionPressed = { action ->
                onActionPressed(action)
                viewModel.handleAction(
                    action = action,
                    selectedNodes = selectedNodes,
                    nodeActionHandler = nodeActionHandler,
                )
                showMoreBottomSheet = false
            }
        )
    }
}
