package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.presentation.node.model.menuaction.HideMenuAction
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import mega.privacy.android.shared.original.core.ui.theme.grey_alpha_070
import mega.privacy.android.shared.original.core.ui.theme.white_alpha_070
import mega.privacy.mobile.analytics.event.HideNodeMenuItemEvent
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
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    private var isPaid: Boolean = false
    private var isBusinessAccountExpired: Boolean = false

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
                    if (!isPaid || isBusinessAccountExpired) {
                        MegaText(
                            text = stringResource(id = R.string.general_pro_only),
                            textColor = TextColor.Accent,
                        )
                    } else {
                        Icon(
                            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.HelpCircle),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    handler(menuAction, selectedNode)
                                    onDismiss()
                                },
                            tint = grey_alpha_070.takeIf { MaterialTheme.colors.isLight }
                                ?: white_alpha_070,
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
        val isHiddenNodesEnabled = isHiddenNodesActive()
        if (!isHiddenNodesEnabled) return false

        if (isNodeInRubbish || accessPermission != AccessPermission.OWNER || node.isTakenDown || isInBackups)
            return false

        if (!isHidingActionAllowedUseCase(node.id))
            return false
        this.isPaid =
            monitorAccountDetailUseCase().first().levelDetail?.accountType?.isPaid ?: false
        this.isBusinessAccountExpired = getBusinessStatusUseCase() == BusinessAccountStatus.Expired
        if (!isPaid || isBusinessAccountExpired)
            return true

        return !node.isMarkedSensitive && !node.isSensitiveInherited
    }

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        parentCoroutineScope.launch {
            Analytics.tracker.trackEvent(HideNodeMenuItemEvent)
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            if (!this@HideBottomSheetMenuItem.isPaid || isBusinessAccountExpired) {
                actionHandler(menuAction, node)
            } else if (node.isMarkedSensitive || isHiddenNodesOnboarded) {
                updateNodeSensitiveUseCase(node.id, true)
            } else {
                actionHandler(menuAction, node)
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

    override
    val groupId = 8
}