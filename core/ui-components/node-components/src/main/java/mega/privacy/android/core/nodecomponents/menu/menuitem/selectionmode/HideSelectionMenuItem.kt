package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import kotlinx.coroutines.flow.first
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionMenuItem
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import javax.inject.Inject

class HideSelectionMenuItem @Inject constructor(
    override val menuAction: HideMenuAction,
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
        if (!hasNodeAccessPermission || !noNodeTakenDown || !noNodeInBackups) {
            return false
        }

        val hasNotAllowedNode = selectedNodes.any { node ->
            runCatching { !isHidingActionAllowedUseCase(node.id) }.getOrDefault(true)
        }

        if (hasNotAllowedNode) {
            return false
        }

        val isPaid = runCatching {
            monitorAccountDetailUseCase().first().levelDetail?.accountType?.isPaid ?: false
        }.getOrDefault(false)

        val isBusinessAccountExpired = runCatching {
            getBusinessStatusUseCase() == BusinessAccountStatus.Expired
        }.getOrDefault(false)

        if (!isPaid || isBusinessAccountExpired) {
            return true
        }

        return selectedNodes.any { !it.isMarkedSensitive } && selectedNodes.none { it.isSensitiveInherited }
    }
}