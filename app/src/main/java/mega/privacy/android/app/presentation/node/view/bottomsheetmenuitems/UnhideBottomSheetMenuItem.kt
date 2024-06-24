package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.node.model.menuaction.UnhideMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import timber.log.Timber
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
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ): Boolean {
        val isHiddenNodesEnabled = getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)
        if (!isHiddenNodesEnabled) return false
        if (isNodeInRubbish || accessPermission != AccessPermission.OWNER || node.isTakenDown)
            return false
        val isPaid =
            monitorAccountDetailUseCase().first().levelDetail?.accountType?.isPaid ?: false
        if (!isPaid)
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
            runCatching {
                updateNodeSensitiveUseCase(node.id, false)
            }.onFailure { Timber.e(it) }
        }
        onDismiss()
    }

    override val groupId = 8
}