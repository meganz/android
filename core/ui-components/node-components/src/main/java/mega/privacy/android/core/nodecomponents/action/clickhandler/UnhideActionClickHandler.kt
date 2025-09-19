package mega.privacy.android.core.nodecomponents.action.clickhandler

import kotlinx.coroutines.launch
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.UnhideMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import timber.log.Timber
import javax.inject.Inject

class UnhideActionClickHandler @Inject constructor(
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
) : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is UnhideMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.coroutineScope.launch {
            runCatching {
                updateNodeSensitiveUseCase(node.id, false)
            }.onFailure { Timber.e(it) }
        }
    }
}
