package mega.privacy.android.core.nodecomponents.action.clickhandler

import kotlinx.coroutines.launch
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageShareFolderMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.destination.FileContactInfo
import javax.inject.Inject

class ManageShareFolderActionClickHandler @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val megaNavigator: MegaNavigator,
) : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is ManageShareFolderMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.coroutineScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.SingleActivity)) {
                provider.navigationHandler?.navigate(
                    FileContactInfo(
                        folderHandle = node.id.longValue, folderName = node.name
                    )
                )
            } else {
                megaNavigator.openFileContactListActivity(provider.context, node.id.longValue)
            }
        }
    }
}
