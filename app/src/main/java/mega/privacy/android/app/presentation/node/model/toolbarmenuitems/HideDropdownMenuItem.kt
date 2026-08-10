package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.node.model.menuaction.HideDropdownMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.HideMenuAction
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import mega.privacy.mobile.analytics.event.HideNodeMultiSelectMenuItemEvent
import javax.inject.Inject

/**
 * Hide selection menu item
 *
 * @property menuAction [HideMenuAction]
 */
class HideDropdownMenuItem @Inject constructor(
    override val menuAction: HideDropdownMenuAction,
    private val isHidingActionAllowedUseCase: IsHidingActionAllowedUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
) : NodeToolbarMenuItem<MenuAction> {
    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
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

    override fun getOnClick(
        selectedNodes: List<TypedNode>,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit,
        navController: NavHostController,
        parentScope: CoroutineScope,
    ): () -> Unit = {
        parentScope.launch {
            Analytics.tracker.trackEvent(HideNodeMultiSelectMenuItemEvent)
            actionHandler(menuAction, selectedNodes)
        }
        onDismiss()
    }
}