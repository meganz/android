package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import kotlinx.coroutines.flow.first
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.UnhideMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import javax.inject.Inject

class UnhideSelectionMenuItem @Inject constructor(
    override val menuAction: UnhideMenuAction,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val isHidingActionAllowedUseCase: IsHidingActionAllowedUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
) : NodeSelectionMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
    ): Boolean {
        if (!hasNodeAccessPermission || !noNodeTakenDown) return false

        val isHiddenNodesEnabled = isHiddenNodesActive()
        if (!isHiddenNodesEnabled) return false

        val isPaid = runCatching {
            monitorAccountDetailUseCase().first().levelDetail?.accountType?.isPaid ?: false
        }.getOrDefault(false)

        val isBusinessAccountExpired = runCatching {
            getBusinessStatusUseCase() == BusinessAccountStatus.Expired
        }.getOrDefault(false)

        if (!isPaid || isBusinessAccountExpired) return false

        val hasNotAllowedNode = selectedNodes.any { node ->
            runCatching { !isHidingActionAllowedUseCase(node.id) }.getOrDefault(true)
        }

        if (hasNotAllowedNode) {
            return false
        }

        return selectedNodes.all { it.isMarkedSensitive } && selectedNodes.none { it.isSensitiveInherited }
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }
}
