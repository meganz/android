package mega.privacy.android.core.nodecomponents.action.clickhandler

import kotlinx.coroutines.launch
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.SendToChatMenuAction
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.chat.GetNodeToAttachUseCase
import timber.log.Timber
import javax.inject.Inject

class SendToChatActionClickHandler @Inject constructor(
    private val getNodeToAttachUseCase: GetNodeToAttachUseCase,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is SendToChatMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        if (node is TypedFileNode) {
            provider.coroutineScope.launch {
                runCatching {
                    getNodeToAttachUseCase(node)
                }.onSuccess { typedNode ->
                    typedNode?.let {
                        provider.sendToChatLauncher.launch(
                            longArrayOf(node.id.longValue)
                        )
                    }
                }.onFailure { Timber.e(it) }
            }
        }
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
        provider.sendToChatLauncher.launch(nodeHandleArray)
    }
}
