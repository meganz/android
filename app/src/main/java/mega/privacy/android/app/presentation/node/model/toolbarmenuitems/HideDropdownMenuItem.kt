package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.node.model.menuaction.HideDropdownMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.HideMenuAction
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.mobile.analytics.event.HideNodeMultiSelectMenuItemEvent
import javax.inject.Inject

/**
 * Hide selection menu item
 *
 * @property menuAction [HideMenuAction]
 */
class HideDropdownMenuItem @Inject constructor(
    override val menuAction: HideDropdownMenuAction,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val isHidingActionAllowedUseCase: IsHidingActionAllowedUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
) : NodeToolbarMenuItem<MenuAction> {
    private var isPaid: Boolean = false
    private var isBusinessAccountExpired: Boolean = false

    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ): Boolean {
        val isHiddenNodesEnabled = isHiddenNodesActive()

        if (!isHiddenNodesEnabled || !hasNodeAccessPermission || !noNodeTakenDown || !noNodeInBackups) {
            return false
        }

        if (selectedNodes.any { !isHidingActionAllowedUseCase(it.id) }) {
            return false
        }

        isPaid = monitorAccountDetailUseCase().first().levelDetail?.accountType?.isPaid ?: false
        isBusinessAccountExpired = getBusinessStatusUseCase() == BusinessAccountStatus.Expired
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
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            if (!this@HideDropdownMenuItem.isPaid || isBusinessAccountExpired) {
                actionHandler(menuAction, selectedNodes)
            } else if (isHiddenNodesOnboarded) {
                selectedNodes.forEach {
                    updateNodeSensitiveUseCase(it.id, true)
                }
            } else {
                actionHandler(menuAction, selectedNodes)
            }
        }
        onDismiss()
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }
}