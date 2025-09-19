package mega.privacy.android.core.nodecomponents.action.clickhandler

import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.DisputeTakeDownMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject

class DisputeTakeDownActionClickHandler @Inject constructor(
    private val megaNavigator: MegaNavigator,
) : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is DisputeTakeDownMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        megaNavigator.launchUrl(provider.context, DISPUTE_URL)
    }

    companion object {
        private const val DISPUTE_URL: String = "https://mega.io/dispute"
    }
}
