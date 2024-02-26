package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.presentation.extensions.isOutShare
import mega.privacy.android.app.presentation.node.model.menuaction.ShareFolderMenuAction
import mega.privacy.android.app.presentation.search.navigation.searchFolderShareDialog
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeByHandleUseCase
import mega.privacy.android.domain.usecase.shares.CreateShareKeyUseCase
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
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
    private val checkBackupNodeTypeByHandleUseCase: CheckBackupNodeTypeByHandleUseCase,
    private val listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
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
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        parentCoroutineScope.launch {
            withContext(NonCancellable) {
                if (node is TypedFolderNode) {
                    runCatching { createShareKeyUseCase(node) }.onFailure { Timber.e(it) }
                    val backupType =
                        runCatching { checkBackupNodeTypeByHandleUseCase(node) }
                            .onFailure { Timber.e(it) }.getOrNull()
                    if (backupType != BackupNodeType.NonBackupNode) {
                        val handles = listOf(node.id.longValue)
                        runCatching {
                            listToStringWithDelimitersMapper(handles)
                        }.onSuccess { handle ->
                            parentCoroutineScope.ensureActive()
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
}