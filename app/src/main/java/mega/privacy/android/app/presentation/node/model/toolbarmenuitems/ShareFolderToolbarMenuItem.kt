package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.node.model.menuaction.ShareFolderMenuAction
import mega.privacy.android.app.presentation.search.navigation.searchFolderShareDialog
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeByHandleUseCase
import mega.privacy.android.domain.usecase.shares.CreateShareKeyUseCase
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import timber.log.Timber
import javax.inject.Inject

/**
 * Show share folder menu item
 *
 * @property menuAction [ShareFolderMenuAction]
 */
class ShareFolderToolbarMenuItem @Inject constructor(
    override val menuAction: ShareFolderMenuAction,
    @ApplicationScope private val scope: CoroutineScope,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val checkBackupNodeTypeByHandleUseCase: CheckBackupNodeTypeByHandleUseCase,
    private val listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
) : NodeToolbarMenuItem<MenuActionWithIcon> {


    override fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = noNodeTakenDown && selectedNodes.isNotEmpty() && selectedNodes.first() is FolderNode

    override fun getOnClick(
        selectedNodes: List<TypedNode>,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit,
        navController: NavHostController,
    ): () -> Unit = {
        selectedNodes.filterIsInstance<FolderNode>()
            .let {
                scope.launch(mainDispatcher) {
                    val hasBackUpNodes = it.find { handle ->
                        runCatching {
                            checkBackupNodeTypeByHandleUseCase(handle) != BackupNodeType.NonBackupNode
                        }.getOrElse {
                            Timber.e(it)
                            false
                        }
                    }
                    hasBackUpNodes?.let {
                        runCatching {
                            val handles = selectedNodes.map { it.id.longValue }
                            listToStringWithDelimitersMapper(handles)
                        }.onSuccess { handle ->
                            navController.navigate(
                                searchFolderShareDialog.plus("/${handle}")
                            )
                        }.onFailure {
                            Timber.e(it)
                        }
                    } ?: run {
                        actionHandler(menuAction, selectedNodes)
                    }
                }
            }
        onDismiss()
    }
}
