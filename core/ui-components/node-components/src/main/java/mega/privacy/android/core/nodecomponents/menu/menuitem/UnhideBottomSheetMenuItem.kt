package mega.privacy.android.core.nodecomponents.menu.menuitem

import kotlinx.coroutines.flow.first
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.UnhideMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
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
        val isPaid =
            monitorAccountDetailUseCase().first().levelDetail?.accountType?.isPaid ?: false
        val isBusinessAccountExpired = getBusinessStatusUseCase() == BusinessAccountStatus.Expired
        if (!isPaid || isBusinessAccountExpired)
            return false

        return isHidingActionAllowedUseCase(node.id) && node.isMarkedSensitive && !node.isSensitiveInherited
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }

    override val groupId = 8
}