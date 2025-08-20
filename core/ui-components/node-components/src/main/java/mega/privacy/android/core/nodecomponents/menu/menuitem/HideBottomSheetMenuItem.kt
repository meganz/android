package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.IsHidingActionAllowedUseCase
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
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

    override fun buildComposeControl(
        selectedNode: TypedNode,
    ): @Composable (BottomSheetClickHandler) -> Unit = { handler ->
        NodeActionListTile(
            text = menuAction.getDescription(),
            icon = menuAction.getIconPainter(),
            isDestructive = isDestructiveAction,
            onActionClicked = getOnClickFunction(
                node = selectedNode,
                handler = handler
            ),
            trailingItem = {
                if (!isPaid || isBusinessAccountExpired) {
                    MegaText(
                        text = stringResource(id = sharedR.string.general_pro_only_label),
                        textColor = TextColor.Accent,
                    )
                } else {
                    MegaIcon(
                        painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.HelpCircle),
                        contentDescription = null,
                        modifier = Modifier.Companion
                            .size(24.dp)
                            .clickable {
                                handler.actionHandler(menuAction, selectedNode)
                                handler.onDismiss()
                            },
                        tint = IconColor.Secondary
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
        handler: BottomSheetClickHandler,
    ): () -> Unit = {
        handler.coroutineScope.launch {
            // Todo handle analytics tracking
            //Analytics.tracker.trackEvent(HideNodeMenuItemEvent)
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            if (!this@HideBottomSheetMenuItem.isPaid || isBusinessAccountExpired) {
                handler.actionHandler(menuAction, node)
            } else if (node.isMarkedSensitive || isHiddenNodesOnboarded) {
                updateNodeSensitiveUseCase(node.id, true)
            } else {
                handler.actionHandler(menuAction, node)
            }
        }
        handler.onDismiss()
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