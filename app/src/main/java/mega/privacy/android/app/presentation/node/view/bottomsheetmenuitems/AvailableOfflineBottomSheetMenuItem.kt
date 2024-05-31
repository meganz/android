package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.presentation.node.model.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.foldernode.IsFolderEmptyUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
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
    private val isFolderEmptyUseCase: IsFolderEmptyUseCase,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {

    override fun buildComposeControl(
        selectedNode: TypedNode,
    ): BottomSheetClickHandler =
        { onDismiss, handler, navController, coroutineScope ->
            val onClick = getOnClickFunction(
                node = selectedNode,
                onDismiss = onDismiss,
                actionHandler = handler,
                navController = navController,
                parentCoroutineScope = coroutineScope
            )
            MenuActionListTile(
                text = menuAction.getDescription(),
                icon = menuAction.getIconPainter(),
                isDestructive = isDestructiveAction,
                onActionClicked = onClick,
                dividerType = null,
                trailingItem = {
                    MegaSwitch(
                        checked = selectedNode.isAvailableOffline,
                    ) { onClick() }
                }
            )
        }

    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = isNodeInRubbish.not() && node.isTakenDown.not() && isFolderEmptyUseCase(node).not()

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        if (node.isAvailableOffline) {
            parentCoroutineScope.launch {
                withContext(NonCancellable) {
                    runCatching {
                        removeOfflineNodeUseCase(nodeId = node.id)
                    }.onFailure { Timber.e(it) }
                }
            }
        } else {
            actionHandler(menuAction, node)
        }
    }

    override val groupId = 6
}