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
import mega.privacy.android.core.nodecomponents.action.MultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.core.nodecomponents.sheet.nodeactions.NodeMoreOptionsBottomSheet
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * A bottom bar component for node selection mode that displays action buttons
 * and handles multi-node operations.
 *
 * This composable wraps [SelectionModeBottomBar] and adds support for showing
 * a "More" options bottom sheet when additional actions are available.
 *
 * @param availableActions All available actions for the selected nodes (shown in "More" bottom sheet)
 * @param visibleActions Actions to display directly in the bottom bar
 * @param visible Whether the bottom bar should be visible
 * @param multiNodeActionHandler Handler for executing actions on multiple nodes
 * @param selectedNodes The list of currently selected nodes
 * @param isSelecting Whether a selection operation is in progress (disables actions when true)
 * @param modifier Modifier to be applied to the bottom bar
 * @param onActionPressed Optional callback invoked when any action is pressed (before handling)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeSelectionModeBottomBar(
    availableActions: List<MenuActionWithIcon>,
    visible: Boolean,
    multiNodeActionHandler: MultiNodeActionHandler,
    selectedNodes: List<TypedNode>,
    isSelecting: Boolean,
    modifier: Modifier = Modifier,
    visibleActions: List<MenuActionWithIcon> = availableActions,
    onActionPressed: (MenuActionWithIcon) -> Unit = {},
    onMoreClicked: () -> Unit = {},
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
                onMoreClicked()
                showMoreBottomSheet = true
                return@SelectionModeBottomBar
            }

            multiNodeActionHandler(action, selectedNodes)
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
                multiNodeActionHandler(action, selectedNodes)
                showMoreBottomSheet = false
            }
        )
    }
}

/**
 * A floating toolbar bottom bar for selection mode that animates in/out.
 *
 * This is a lower-level composable that displays a [MegaFloatingToolbar] with
 * slide-in animation from the bottom. It does not handle action execution -
 * use [NodeSelectionModeBottomBar] for complete node selection handling.
 *
 * @param visible Whether the bottom bar should be visible (controls animation)
 * @param actions List of actions to display in the toolbar
 * @param modifier Modifier to be applied to the container
 * @param actionsEnabled Whether the action buttons are enabled
 * @param onActionPressed Callback invoked when an action is pressed
 */
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
