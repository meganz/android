package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.extension.isOutShare
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareFolderMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeUseCase
import mega.privacy.android.domain.usecase.shares.CreateShareKeyUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Share folder bottom sheet menu item
 *
 * @param menuAction [ShareFolderMenuAction]
 */
class ShareFolderBottomSheetMenuItem @Inject constructor(
    override val menuAction: ShareFolderMenuAction,
    private val createShareKeyUseCase: CreateShareKeyUseCase,
    private val checkBackupNodeTypeUseCase: CheckBackupNodeTypeUseCase,
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && node is TypedFolderNode
            && isNodeInRubbish.not()
            && node.isOutShare().not()
            && accessPermission == AccessPermission.OWNER


    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: NodeActionHandler,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        parentCoroutineScope.launch {
            withContext(NonCancellable) {
                if (node is TypedFolderNode) {
                    runCatching { createShareKeyUseCase(node) }.onFailure { Timber.e(it) }
                    val backupType =
                        runCatching { checkBackupNodeTypeUseCase(node) }
                            .onFailure { Timber.e(it) }.getOrNull()
                    if (backupType != BackupNodeType.NonBackupNode) {
                        val handles = listOf(node.id.longValue)
                        runCatching {
                            nodeHandlesToJsonMapper(handles)
                        }.onSuccess { handle ->
                            parentCoroutineScope.ensureActive()
                            // Todo: navigationHandler
                            navController.navigate(
                                searchFolderShareDialog.plus("/${handle}")
                            )
                        }.onFailure {
                            Timber.e(it)
                        }
                    } else {
                        parentCoroutineScope.ensureActive()
                        actionHandler(menuAction, node)
                    }
                }
            }
        }
    }

    override val groupId = 7

    companion object {
        // Todo duplicate to the one in mega.privacy.android.app.presentation.search.model.navigation.SearchFolderShareNavigation.kt
        private const val searchFolderShareDialog = "search/folder_share"
    }
}