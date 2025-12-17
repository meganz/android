package mega.privacy.android.core.nodecomponents.action.clickhandler

import kotlinx.coroutines.launch
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.NodeActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.mobile.analytics.event.HideNodeMenuItemEvent
import mega.privacy.mobile.analytics.event.HideNodeMultiSelectMenuItemEvent
import javax.inject.Inject

class HideActionClickHandler @Inject constructor(
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is HideMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        Analytics.tracker.trackEvent(HideNodeMenuItemEvent)
        handleHide(provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        Analytics.tracker.trackEvent(HideNodeMultiSelectMenuItemEvent)
        handleHide(provider)
    }

    private fun handleHide(provider: NodeActionProvider) {
        provider.coroutineScope.launch {
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            val isOnboarding = provider.viewModel.isOnboarding()
            if (isOnboarding && isHiddenNodesOnboarded) {
                provider.viewModel.handleHiddenNodesOnboardingResult(
                    isOnboarded = true,
                    isHidden = true
                )
            } else {
                provider.hiddenNodesOnboardingLauncher.launch(isOnboarding)
            }
        }
    }
}
