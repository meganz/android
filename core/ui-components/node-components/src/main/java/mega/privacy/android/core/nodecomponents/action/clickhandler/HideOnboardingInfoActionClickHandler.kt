package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideOnboardingInfoMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.navigation.destination.HiddenNodesOnboardingNavKey
import javax.inject.Inject

class HideOnboardingInfoActionClickHandler @Inject constructor() : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is HideOnboardingInfoMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.viewModel.navigateWithNavKey(HiddenNodesOnboardingNavKey)
    }
}
