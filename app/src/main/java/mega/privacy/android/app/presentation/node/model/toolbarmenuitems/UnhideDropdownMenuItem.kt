package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.node.model.menuaction.UnhideDropdownMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.UnhideMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import javax.inject.Inject

/**
 * Unhide selection menu item
 *
 * @property menuAction [UnhideMenuAction]
 */
class UnhideDropdownMenuItem @Inject constructor(
    override val menuAction: UnhideDropdownMenuAction,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val isHidingActionAllowedUseCase: IsHidingActionAllowedUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
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
        if (!hasNodeAccessPermission || !noNodeTakenDown) return false

        val isHiddenNodesEnabled = getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)
        if (!isHiddenNodesEnabled) return false

        val isPaid = monitorAccountDetailUseCase().first().levelDetail?.accountType?.isPaid ?: false
        if (!isPaid) return false

        if (selectedNodes.any { !isHidingActionAllowedUseCase(it.id) }) return false

        return selectedNodes.all { it.isMarkedSensitive } && selectedNodes.none { it.isSensitiveInherited }
    }

    override fun getOnClick(
        selectedNodes: List<TypedNode>,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit,
        navController: NavHostController,
        parentScope: CoroutineScope,
    ): () -> Unit = {
        parentScope.launch {
            selectedNodes.forEach {
                updateNodeSensitiveUseCase(it.id, false)
            }
        }
        onDismiss()
    }
}