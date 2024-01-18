package mega.privacy.android.app.presentation.node.dialogs.sharefolder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeByHandleUseCase
import javax.inject.Inject

/**
 * View model for share folder dialog
 * @property getNodeByHandleUseCase [GetNodeByHandleUseCase]
 * @property checkBackupNodeTypeByHandleUseCase [CheckBackupNodeTypeByHandleUseCase]
 */
@HiltViewModel
class ShareFolderDialogViewModel @Inject constructor(
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val checkBackupNodeTypeByHandleUseCase: CheckBackupNodeTypeByHandleUseCase,
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
            nodeIdList.firstOrNull()?.let {
                getNodeByHandleUseCase(it.longValue)?.let { node ->
                    val nodeType = checkBackupNodeTypeByHandleUseCase(node)
                    if (nodeIdList.size > 1 && nodeType == BackupNodeType.RootNode) {
                        _state.update { state ->
                            state.copy(
                                info = R.string.backup_multi_share_permission_text,
                                positiveButton = R.string.general_positive_button,
                                negativeButton = R.string.general_cancel,
                            )
                        }
                    } else {
                        _state.update { state ->
                            state.copy(
                                info = R.string.backup_share_permission_text,
                                positiveButton = R.string.button_permission_info,
                                negativeButton = null,
                            )
                        }
                    }
                }
            }
        }
    }
}