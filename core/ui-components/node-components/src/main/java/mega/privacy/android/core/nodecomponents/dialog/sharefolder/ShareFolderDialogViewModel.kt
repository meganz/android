package mega.privacy.android.core.nodecomponents.dialog.sharefolder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View model for share folder dialog
 * @property getNodeByIdUseCase [GetNodeByIdUseCase]
 * @property checkBackupNodeTypeUseCase [CheckBackupNodeTypeUseCase]
 */
@HiltViewModel
class ShareFolderDialogViewModel @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val checkBackupNodeTypeUseCase: CheckBackupNodeTypeUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ShareFolderDialogState())

    /**
     * state for ShareFolderDialog
     */
    val state: StateFlow<ShareFolderDialogState> = _state.asStateFlow()

    /**
     * Get contents of share dialog
     * @param nodeIdList List of node Ids
     */
    fun getDialogContents(nodeIdList: List<NodeId>) {
        viewModelScope.launch {
            val typeNodeList = nodeIdList
                .mapNotNull {
                    runCatching { getNodeByIdUseCase(it) }
                        .getOrElse { t ->
                            Timber.e(t)
                            null
                        }
                }

            typeNodeList.firstOrNull()?.let { node ->
                val nodeType = getBackupNodeType(node)
                if (nodeIdList.size > 1 && nodeType == BackupNodeType.RootNode) {
                    _state.update { state ->
                        state.copy(
                            dialogType = ShareFolderDialogType.Multiple(
                                nodeList = typeNodeList
                            )
                        )
                    }
                } else {
                    _state.update { state ->
                        state.copy(
                            dialogType = ShareFolderDialogType.Single(
                                nodeList = typeNodeList
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun getBackupNodeType(node: TypedNode) = runCatching {
        checkBackupNodeTypeUseCase(node)
    }.getOrElse {
        Timber.e(it)
        BackupNodeType.NonBackupNode
    }
}
