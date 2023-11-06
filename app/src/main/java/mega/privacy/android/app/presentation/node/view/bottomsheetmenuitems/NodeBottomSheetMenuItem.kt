package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems


import androidx.compose.runtime.Composable
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission

/**
 * Bottom sheet click handler
 */
typealias BottomSheetClickHandler = @Composable (onDismiss: () -> Unit, actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit) -> Unit

/**
 * Node bottom sheet menu item
 */
interface NodeBottomSheetMenuItem<T : MenuActionWithIcon> {

    /**
     * Build compose control
     */
    fun buildComposeControl(
        selectedNode: TypedNode,
    ): BottomSheetClickHandler =
        { onDismiss, handler ->
            MenuActionListTile(
                text = menuAction.getDescription(),
                icon = menuAction.getIconPainter(),
                isDestructive = isDestructiveAction,
                onActionClicked = getOnClickFunction(
                    node = selectedNode,
                    onDismiss = onDismiss,
                    actionHandler = handler,
                ),
            )
        }

    /**
     * should display
     *
     * checks if menu item should be displayed or not
     * @return [Boolean]
     */
    fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
    ): Boolean


    /**
     * handle on click function
     */
    fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
    ): () -> Unit = {
        actionHandler(menuAction, node)
        onDismiss()
    }

    /**
     * Is destructive action
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