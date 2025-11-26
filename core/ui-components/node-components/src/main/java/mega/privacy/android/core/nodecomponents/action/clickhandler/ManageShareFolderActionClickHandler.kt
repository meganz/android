package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageShareFolderMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject

class ManageShareFolderActionClickHandler @Inject constructor(
    private val megaNavigator: MegaNavigator,
) : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is ManageShareFolderMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        megaNavigator.openFileContactListActivity(
            context = provider.context,
            handle = node.id.longValue,
            nodeName = node.name,
        )
    }
}

