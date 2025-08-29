package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.app.presentation.node.model.menuaction.UnhideMenuAction
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import javax.inject.Inject

/**
 * Unhide bottom sheet menu item
 *
 * @param menuAction [UnhideMenuAction]
 */
class UnhideBottomSheetMenuItem @Inject constructor(
    override val menuAction: UnhideMenuAction,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val isHidingActionAllowedUseCase: IsHidingActionAllowedUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ): Boolean {
        val isHiddenNodesEnabled = isHiddenNodesActive()
        if (!isHiddenNodesEnabled) return false
        if (isNodeInRubbish || accessPermission != AccessPermission.OWNER || node.isTakenDown)
            return false

        val isPaid = runCatching {
            monitorAccountDetailUseCase().first().levelDetail?.accountType?.isPaid ?: false
        }.getOrDefault(false)

        val isBusinessAccountExpired = runCatching {
            getBusinessStatusUseCase() == BusinessAccountStatus.Expired
        }.getOrDefault(false)

        if (!isPaid || isBusinessAccountExpired)
            return false

        return isHidingActionAllowedUseCase(node.id) && node.isMarkedSensitive && !node.isSensitiveInherited
    }

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        parentCoroutineScope.launch {
            actionHandler(menuAction, node)
        }
        onDismiss()
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }

    override val groupId = 8
}