package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.LabelMenuAction
import mega.privacy.android.core.nodecomponents.sheet.changelabel.ChangeLabelBottomSheet
import mega.privacy.android.core.nodecomponents.sheet.changelabel.ChangeLabelBottomSheetMultiple
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

class LabelActionClickHandler @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is LabelMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.viewModel.navigateWithNavKey(ChangeLabelBottomSheet(node.id.longValue))
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        if (nodes.isEmpty()) return
        val navKey = if (nodes.size == 1) {
            ChangeLabelBottomSheet(nodes.single().id.longValue)
        } else {
            ChangeLabelBottomSheetMultiple(nodes.map { it.id.longValue })
        }
        provider.viewModel.navigateWithNavKey(navKey)
    }
}
