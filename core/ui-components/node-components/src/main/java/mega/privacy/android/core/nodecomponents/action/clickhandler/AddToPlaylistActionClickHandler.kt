package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.AddToPlaylistMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

class AddToPlaylistActionClickHandler @Inject constructor() : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is AddToPlaylistMenuAction

    override fun handle(
        action: MenuAction,
        node: TypedNode,
        provider: SingleNodeActionProvider,
    ) {
        provider.videoToPlaylistLauncher.launch(node.id.longValue)
    }
}