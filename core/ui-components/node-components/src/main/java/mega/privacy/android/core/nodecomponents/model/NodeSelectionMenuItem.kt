package mega.privacy.android.core.nodecomponents.model

import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.navigation.contract.NavigationHandler

data class NodeSelectionClickHandler(
    val onDismiss: () -> Unit,
    val actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit,
    val navigationHandler: NavigationHandler,
    val coroutineScope: CoroutineScope,
)

/**
 * Node selection handler
 */
typealias NodeSelectionHandler = @Composable (NodeSelectionClickHandler) -> () -> Unit

interface NodeSelectionMenuItem<T : MenuActionWithIcon> {
    /**
     * should display
     *
     * checks if menu item should be displayed or not
     *
     * @param hasNodeAccessPermission
     * @param selectedNodes
     * @param canBeMovedToTarget
     * @param noNodeInBackups
     * @param noNodeTakenDown
     * @param allFileNodes
     * @param resultCount
     * @return [Boolean]
     */
    suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
    ): Boolean

    /**
     * Build handler
     */
    fun buildHandler(selectedNodes: List<TypedNode>): NodeSelectionHandler =
        { handler -> getOnClick(selectedNodes, handler) }


    /**
     * Get on click function
     */
    fun getOnClick(
        selectedNodes: List<TypedNode>,
        handler: NodeSelectionClickHandler,
    ): () -> Unit = {
        handler.actionHandler(menuAction, selectedNodes)
        handler.onDismiss()
    }

    /**
     * Menu action item
     */
    val menuAction: T
}