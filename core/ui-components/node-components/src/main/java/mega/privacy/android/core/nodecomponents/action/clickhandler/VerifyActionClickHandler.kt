package mega.privacy.android.core.nodecomponents.action.clickhandler

import kotlinx.coroutines.launch
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.dialog.contact.CannotVerifyContactDialogNavKey
import mega.privacy.android.core.nodecomponents.menu.menuaction.VerifyMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.shares.GetNodeShareDataUseCase
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject

class VerifyActionClickHandler @Inject constructor(
    private val getNodeShareDataUseCase: GetNodeShareDataUseCase,
    private val megaNavigator: MegaNavigator,
) : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is VerifyMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.coroutineScope.launch {
            getNodeShareDataUseCase(node)?.let { data ->
                if (data.isVerified.not() && data.isPending) {
                    provider.viewModel.navigateWithNavKey(
                        CannotVerifyContactDialogNavKey(data.user.orEmpty())
                    )
                } else {
                    megaNavigator.openAuthenticityCredentialsActivity(
                        context = provider.context,
                        email = data.user.orEmpty(),
                        isIncomingShares = node.isIncomingShare
                    )
                    provider.viewModel.dismiss()
                }
            }
        }
    }
}
