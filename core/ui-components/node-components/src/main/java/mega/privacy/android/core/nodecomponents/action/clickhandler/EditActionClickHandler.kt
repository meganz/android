package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.EditMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Handles the Edit action from the node options bottom sheet.
 *
 * Returns a result via [RESULT_KEY] so that screens already showing the file (e.g. the text
 * editor in View mode) can switch to Edit mode in-place without opening a new destination.
 */
class EditActionClickHandler @Inject constructor() : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is EditMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.navigationHandler?.returnResult(
            RESULT_KEY,
            node.id.longValue,
        )
        provider.viewModel.dismiss()
    }

    companion object {
        const val RESULT_KEY = "EditActionClickHandler:edit_node_handle"
    }
}
