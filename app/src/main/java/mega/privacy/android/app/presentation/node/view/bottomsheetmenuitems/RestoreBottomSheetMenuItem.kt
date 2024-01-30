package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.mapper.RestoreNodeResultMapper
import mega.privacy.android.app.presentation.node.model.menuaction.RestoreMenuAction
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.RestoreNodesUseCase
import javax.inject.Inject

/**
 * Restore bottom sheet menu item
 *
 * @param menuAction [RestoreMenuAction]
 */
class RestoreBottomSheetMenuItem @Inject constructor(
    override val menuAction: RestoreMenuAction,
    @ApplicationScope private val scope: CoroutineScope,
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase,
    private val restoreNodesUseCase: RestoreNodesUseCase,
    private val restoreNodeResultMapper: RestoreNodeResultMapper,
    private val snackBarHandler: SnackBarHandler,
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
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
    ): () -> Unit = {
        node.restoreId?.let {
            onDismiss()
            val restoreMap = mapOf(Pair(node.id.longValue, it.longValue))
            scope.launch {
                val result =
                    checkNodesNameCollisionUseCase(restoreMap, NodeNameCollisionType.RESTORE)
                if (result.conflictNodes.isNotEmpty()) {
                    actionHandler(menuAction, node)
                }
                if (result.noConflictNodes.isNotEmpty()) {
                    val restoreResult = restoreNodesUseCase(result.noConflictNodes)
                    val message = restoreNodeResultMapper(restoreResult)
                    snackBarHandler.postSnackbarMessage(message)
                }
            }
        }
    }
}