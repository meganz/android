package mega.privacy.android.core.nodecomponents.action.clickhandler

import kotlinx.coroutines.launch
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.NodeActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.UnhideMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import javax.inject.Inject

class UnhideActionClickHandler @Inject constructor(
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is UnhideMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        handleUnhide(provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        handleUnhide(provider)
    }

    private fun handleUnhide(provider: NodeActionProvider) {
        provider.coroutineScope.launch {
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            val isOnboarding = provider.viewModel.isOnboarding()
            if (isOnboarding && isHiddenNodesOnboarded) {
                provider.viewModel.handleHiddenNodesOnboardingResult(
                    isOnboarded = true,
                    isHidden = false
                )
            } else {
                provider.hiddenNodesOnboardingLauncher.launch(isOnboarding)
            }
        }
    }
}
