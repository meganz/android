package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuaction.RestoreMenuAction
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.RestoreNodesUseCase
import timber.log.Timber
import javax.inject.Inject
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Restore bottom sheet menu item
 *
 * @param menuAction [RestoreMenuAction]
 */
class RestoreBottomSheetMenuItem @Inject constructor(
    override val menuAction: RestoreMenuAction,
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase,
    private val restoreNodesUseCase: RestoreNodesUseCase,
    // Todo provide snackbar
    // private val restoreNodeResultMapper: RestoreNodeResultMapper,
    // private val snackBarHandler: SnackBarHandler,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = isNodeInRubbish

    override val groupId = 8

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: NodeActionHandler,
        navigationHandler: NavigationHandler,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        val restoreHandle = node.restoreId?.longValue ?: -1L
        onDismiss()
        val restoreMap = mapOf(Pair(node.id.longValue, restoreHandle))
        parentCoroutineScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    checkNodesNameCollisionUseCase(restoreMap, NodeNameCollisionType.RESTORE)
                }.onSuccess { result ->
                    if (result.conflictNodes.isNotEmpty()) {
                        parentCoroutineScope.ensureActive()
                        actionHandler(menuAction, node)
                    }
                    if (result.noConflictNodes.isNotEmpty()) {
                        val restoreResult = restoreNodesUseCase(result.noConflictNodes)
                        // Todo provide snackbar
                        // val message = restoreNodeResultMapper(restoreResult)
                        // snackBarHandler.postSnackbarMessage(message)
                    }
                }.onFailure { throwable ->
                    Timber.e(throwable)
                }
            }
        }
    }
}