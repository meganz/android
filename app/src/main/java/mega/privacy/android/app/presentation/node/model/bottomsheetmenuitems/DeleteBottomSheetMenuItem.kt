package mega.privacy.android.app.presentation.node.model.bottomsheetmenuitems

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.node.model.menuaction.DeleteMenuAction
import mega.privacy.android.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Delete menu item
 *
 * @param menuAction [DeleteMenuAction]
 */
class DeleteBottomSheetMenuItem @Inject constructor(
    override val menuAction: DeleteMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override fun shouldDisplay() = true

    override fun menuAction(selectedNode: TypedNode): @Composable ((MenuAction) -> Unit) -> Unit =
        {
            MenuActionListTile(
                text = menuAction.getDescription(),
                icon = menuAction.getIconPainter(),
                addSeparator = false,
                isDestructive = true,
                onActionClicked = { it(menuAction) }
            )
        }
}