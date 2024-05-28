package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.node.model.menuaction.HideMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsHidingActionAllowedUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import javax.inject.Inject

/**
 * Hide bottom sheet menu item
 *
 * @param menuAction [HideMenuAction]
 */
class HideBottomSheetMenuItem @Inject constructor(
    override val menuAction: HideMenuAction,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val isHidingActionAllowedUseCase: IsHidingActionAllowedUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    private var isPaid: Boolean = false

    override fun buildComposeControl(selectedNode: TypedNode): BottomSheetClickHandler =
        { onDismiss, handler, navController, coroutineScope ->
            MenuActionListTile(
                text = menuAction.getDescription(),
                icon = menuAction.getIconPainter(),
                isDestructive = isDestructiveAction,
                onActionClicked = getOnClickFunction(
                    node = selectedNode,
                    onDismiss = onDismiss,
                    actionHandler = handler,
                    navController = navController,
                    parentCoroutineScope = coroutineScope
                ),
                dividerType = null,
                trailingItem = {
                    if (!isPaid) {
                        MegaText(
                            text = stringResource(id = R.string.general_pro_only),
                            textColor = TextColor.Accent,
                        )
                    }
                }
            )
        }

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
        this.isPaid =
            monitorAccountDetailUseCase().first().levelDetail?.accountType?.isPaid ?: false
        if (!isPaid)
            return true

        return isHidingActionAllowedUseCase(node.id) && !node.isMarkedSensitive
    }

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        parentCoroutineScope.launch {
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            if (!this@HideBottomSheetMenuItem.isPaid) {
                actionHandler(menuAction, node)
            } else if (node.isMarkedSensitive || isHiddenNodesOnboarded) {
                updateNodeSensitiveUseCase(node.id, true)
            } else {
                actionHandler(menuAction, node)
            }
        }
        onDismiss()
    }

    override
    val groupId = 8
}