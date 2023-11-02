import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.node.model.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.NodeBottomSheetMenuItem
import mega.privacy.android.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Available offline menu item
 *
 * @param menuAction [AvailableOfflineMenuAction]
 */
class AvailableOfflineBottomSheetMenuItem @Inject constructor(
    override val menuAction: AvailableOfflineMenuAction,
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase,
    @ApplicationScope private val scope: CoroutineScope,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {

    override fun buildComposeControl(
        selectedNode: TypedNode,
    ): @Composable (onDismiss: () -> Unit, actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit) -> Unit =
        { onDismiss, handler ->
            val onClick = getOnClickFunction(
                node = selectedNode,
                onDismiss = onDismiss,
                actionHandler = handler,
            )
            MenuActionListTile(
                text = menuAction.getDescription(),
                icon = menuAction.getIconPainter(),
                isDestructive = isDestructiveAction,
                onActionClicked = onClick,
                trailingItem = {
                    MegaSwitch(
                        checked = selectedNode.isAvailableOffline,
                        onCheckedChange = { onClick() },
                    )
                }
            )
        }

    override fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
    ) = isNodeInRubbish.not() && node.isTakenDown.not()

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
    ): () -> Unit = {
        onDismiss()
        if (node.isAvailableOffline) {
            scope.launch {
                runCatching {
                    removeOfflineNodeUseCase(nodeId = node.id)
                }.onFailure { Timber.e(it) }
            }
        } else {
            Timber.d("Save offline")
        }
    }

    override val groupId = 6
}