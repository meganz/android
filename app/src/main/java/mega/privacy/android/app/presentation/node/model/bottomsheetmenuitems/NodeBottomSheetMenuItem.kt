package mega.privacy.android.app.presentation.node.model.bottomsheetmenuitems


import androidx.compose.runtime.Composable
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode


/**
 * Node bottom sheet menu item
 */
interface NodeBottomSheetMenuItem<T : MenuActionWithIcon> {

    /**
     * should display
     *
     * checks if menu item should be displayed or not
     * @return [Boolean]
     */
    fun shouldDisplay(): Boolean


    /**
     * Menu action
     *
     * Creates composable menu item for bottom sheet
     * @param selectedNode
     * @param showDivider
     */
    fun menuAction(
        selectedNode: TypedNode,
        showDivider: Boolean,
    ): @Composable ((MenuAction) -> Unit) -> Unit


    /**
     * Menu action item
     */
    val menuAction: T

    /**
     * Group in which menu item is part of
     */
    val groupId: Int
}