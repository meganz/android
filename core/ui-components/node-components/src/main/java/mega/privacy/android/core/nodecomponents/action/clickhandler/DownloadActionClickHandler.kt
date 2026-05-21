package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.model.ZipFileTypedNode
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

class DownloadActionClickHandler @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is DownloadMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        val nodeSourceType = provider.viewModel.getNodeSourceType()
        if (nodeSourceType == NodeSourceType.VIDEO_PLAYER_ZIP_FILE && node is ZipFileTypedNode) {
            provider.viewModel.downloadZipFile(node = node, withStartMessage = false)
        } else {
            provider.viewModel.downloadNode(withStartMessage = false)
        }
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        provider.viewModel.downloadNode(withStartMessage = false)
    }
}
