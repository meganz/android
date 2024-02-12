package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Toolbar click handler
 */
typealias ToolbarClickHandler = @Composable (onDismiss: () -> Unit, actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit, navController: NavHostController) -> () -> Unit

/**
 * Node toolbar menu item
 */
interface NodeToolbarMenuItem<T : MenuAction> {


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
    fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ): Boolean

    /**
     * Build control
     */
    fun setControl(selectedNodes: List<TypedNode>): ToolbarClickHandler =
        { onDismiss, actionHandler, navController ->
            getOnClick(selectedNodes, onDismiss, actionHandler, navController)
        }


    /**
     * Get on click function
     */
    fun getOnClick(
        selectedNodes: List<TypedNode>,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit,
        navController: NavHostController,
    ): () -> Unit = {
        onDismiss()
        actionHandler(menuAction, selectedNodes)
    }

    /**
     * Menu action item
     */
    val menuAction: T
}