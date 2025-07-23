package mega.privacy.android.core.nodecomponents.entity

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.TopAppBarAction
import mega.privacy.android.core.nodecomponents.list.view.NodeActionListTile
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission

/**
 * Bottom sheet click handler
 */
typealias BottomSheetClickHandler = @Composable (
    onDismiss: () -> Unit,
    actionHandler: (menuAction: TopAppBarAction, node: TypedNode) -> Unit,
    navController: NavHostController,
    coroutineScope: CoroutineScope,
) -> Unit

/**
 * Node bottom sheet menu item
 */
interface NodeBottomSheetMenuItem<T : TopAppBarAction> {

    /**
     * Build compose control
     */
    fun buildComposeControl(
        selectedNode: TypedNode,
    ): BottomSheetClickHandler =
        { onDismiss, handler, navController, coroutineScope ->
            NodeActionListTile(
                menuAction = menuAction,
                isDestructive = isDestructiveAction,
                onActionClicked = getOnClickFunction(
                    node = selectedNode,
                    onDismiss = onDismiss,
                    actionHandler = handler,
                    navController = navController,
                    parentCoroutineScope = coroutineScope
                ),
            )
        }

    /**
     * should display
     *
     * checks if menu item should be displayed or not
     * @return [Boolean]
     */
    suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ): Boolean


    /**
     * handle on click function
     */
    fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: TopAppBarAction, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        actionHandler(menuAction, node)
        onDismiss()
    }

    /**
     * Is destructive action
     *
     * true if action is destructive
     *      menu item will be displayed in red
     */
    val isDestructiveAction: Boolean
        get() = false

    /**
     * Menu action item
     */
    val menuAction: T

    /**
     * Group in which menu item is part of
     */
    val groupId: Int
}