package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.EditMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.navigation.destination.LegacyTextEditorNavKey
import javax.inject.Inject

/**
 * Handles the Edit action from the node options bottom sheet.
 * Navigates to the text editor in Edit mode.
 */
class EditActionClickHandler @Inject constructor() : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is EditMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.navigationHandler?.navigate(
            LegacyTextEditorNavKey(
                nodeHandle = node.id.longValue,
                mode = TextEditorMode.Edit.value,
            )
        )
        provider.viewModel.dismiss()
    }
}
