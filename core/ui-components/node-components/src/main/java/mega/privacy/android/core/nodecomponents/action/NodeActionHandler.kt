package mega.privacy.android.core.nodecomponents.action

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Node action handler that wraps Composable functions.
 *
 * Provides a non-Composable interface for handling menu actions on nodes.
 * Acts as a bridge between Composable components (with activity result launchers)
 * and non-Composable components (like menu items).
 *
 * @see rememberNodeActionHandler
 */
class NodeActionHandler(
    private val singleNodeHandler: (MenuAction, TypedNode) -> Unit,
    private val multipleNodesHandler: (MenuAction, List<TypedNode>) -> Unit,
) {
    /**
     * Handles actions for a single node.
     *
     * @param action The menu action to handle
     * @param node The node to act upon
     *
     * @throws IllegalArgumentException if the action is not supported
     */
    operator fun invoke(action: MenuAction, node: TypedNode) {
        singleNodeHandler(action, node)
    }

    /**
     * Handles actions for multiple nodes.
     *
     * @param action The menu action to handle
     * @param nodes The list of nodes to act upon
     *
     * @throws IllegalArgumentException if the action is not supported or nodes list is empty
     */
    operator fun invoke(action: MenuAction, nodes: List<TypedNode>) {
        multipleNodesHandler(action, nodes)
    }
}