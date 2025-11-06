package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.SyncMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.navigation.destination.SyncNewFolderNavKey
import javax.inject.Inject

class SyncActionClickHandler @Inject constructor() : SingleNodeAction {

    override fun canHandle(action: MenuAction): Boolean = action is SyncMenuAction

    override fun handle(
        action: MenuAction,
        node: TypedNode,
        provider: SingleNodeActionProvider,
    ) {
        provider.navigationHandler?.navigate(
            SyncNewFolderNavKey(
                remoteFolderHandle = node.id.longValue,
                remoteFolderName = node.name,
                isFromManagerActivity = true,
            )
        )
    }
}