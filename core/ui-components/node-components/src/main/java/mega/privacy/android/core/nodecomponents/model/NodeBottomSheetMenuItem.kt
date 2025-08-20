package mega.privacy.android.core.nodecomponents.model

import android.content.Context
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.SnackBarAttributes
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Bottom sheet click handler
 */
data class BottomSheetClickHandler(
    val onDismiss: () -> Unit,
    val actionHandler: NodeActionHandler,
    val navigationHandler: NavigationHandler,
    val coroutineScope: CoroutineScope,
    val context: Context,
    val snackbarHandler: (SnackBarAttributes) -> Unit,
)

/**
 * Node bottom sheet menu item
 */
interface NodeBottomSheetMenuItem<T : MenuActionWithIcon> {

    /**
     * Build compose control
     */
    fun buildComposeControl(
        selectedNode: TypedNode,
    ): @Composable (BottomSheetClickHandler) -> Unit =
        { handler ->
            NodeActionListTile(
                menuAction = menuAction,
                isDestructive = isDestructiveAction,
                onActionClicked = getOnClickFunction(
                    node = selectedNode,
                    handler = handler,
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
        handler: BottomSheetClickHandler
    ): () -> Unit = {
        handler.actionHandler(menuAction, node)
        handler.onDismiss()
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