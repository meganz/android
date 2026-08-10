package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.mapper.NodeSourceTypeToViewTypeMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.EditMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.navigation.destination.LegacyTextEditorNavKey
import javax.inject.Inject

/**
 * Handles the Edit action from the node options bottom sheet.
 * Navigates to the text editor in Edit mode.
 */
class EditActionClickHandler @Inject constructor(
    private val nodeSourceTypeToViewTypeMapper: NodeSourceTypeToViewTypeMapper,
) : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is EditMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        // The editor derives its menu visibility from the source type — Share is hidden for incoming
        // shares, for instance — so it has to be forwarded rather than left null.
        val nodeSourceType = provider.viewModel.getNodeSourceType()
        provider.navigationHandler?.navigate(
            LegacyTextEditorNavKey(
                nodeHandle = node.id.longValue,
                mode = TextEditorMode.Edit.value,
                nodeSourceType = nodeSourceType?.let { nodeSourceTypeToViewTypeMapper(it) },
            )
        )
        provider.viewModel.dismiss()
    }
}
