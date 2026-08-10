package mega.privacy.android.core.nodecomponents.action

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Selection mode action handler for multiple node operations.
 *
 * Provides a non-Composable interface for handling menu actions on multiple nodes
 * from selection mode toolbar. Acts as a bridge between Composable components
 * (with activity result launchers) and non-Composable components (like toolbar actions).
 *
 * @see rememberMultiNodeActionHandler
 */
class MultiNodeActionHandler(
    private val handler: (MenuAction, List<TypedNode>) -> Unit,
) {
    /**
     * Handles actions for multiple nodes.
     *
     * @param action The menu action to handle
     * @param nodes The list of nodes to act upon
     */
    operator fun invoke(action: MenuAction, nodes: List<TypedNode>) {
        handler(action, nodes)
    }
}

