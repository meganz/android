package mega.privacy.android.core.nodecomponents.action

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Action handlers for single node operations.
 *
 * Provides a non-Composable interface for handling menu actions on a single node
 * from bottom sheet menu items. Acts as a bridge between Composable components
 * (with activity result launchers) and non-Composable components (like menu items).
 *
 * @see rememberSingleNodeActionHandler
 */
class SingleNodeActionHandler(
    private val handler: (MenuAction, TypedNode) -> Unit,
) {
    /**
     * Handles actions for a single node.
     *
     * @param action The menu action to handle
     * @param node The node to act upon
     */
    operator fun invoke(action: MenuAction, node: TypedNode) {
        handler(action, node)
    }
}

