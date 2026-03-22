package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.EditMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.OpenTextEditorParams
import javax.inject.Inject

class EditActionClickHandler @Inject constructor(
    private val megaNavigator: MegaNavigator,
) : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is EditMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        megaNavigator.openTextEditor(
            context = provider.context,
            params = OpenTextEditorParams.CloudNode(
                nodeId = node.id,
                nodeSourceType = NodeSourceTypeInt.FILE_BROWSER_ADAPTER,
                mode = TextEditorMode.Edit,
                fileName = null,
            ),
        )
        provider.viewModel.dismiss()
    }
}
