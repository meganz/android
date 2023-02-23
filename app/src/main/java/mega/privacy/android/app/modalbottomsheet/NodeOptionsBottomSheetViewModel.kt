package mega.privacy.android.app.modalbottomsheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.OpenShareDialog
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * View model associated with [NodeOptionsBottomSheetDialogFragment]
 */
@HiltViewModel
class NodeOptionsBottomSheetViewModel @Inject constructor(
    private val openShareDialog: OpenShareDialog,
    private val getNodeByHandle: GetNodeByHandle,
) : ViewModel() {

    /**
     * Private UI state
     */
    private val _state = MutableStateFlow(NodeOptionsBottomSheetState())

    /**
     * Public Ui state
     */
    val state: StateFlow<NodeOptionsBottomSheetState> = _state


    /**
     * Calls OpenShareDialog use case to create crypto key for sharing
     *
     * @param nodeHandle: [MegaNode] handle
     */
    fun callOpenShareDialog(nodeHandle: Long) {
        kotlin.runCatching {
            viewModelScope.launch {
                if (nodeHandle != MegaApiJava.INVALID_HANDLE) {
                    getNodeByHandle(nodeHandle)?.let { megaNode ->
                        openShareDialog(megaNode)
                    }
                }
            }
        }.onSuccess {
            _state.update {
                it.copy(isOpenShareDialogSuccess = true)
            }
        }.onFailure {
            _state.update {
                it.copy(isOpenShareDialogSuccess = false)
            }
        }
    }

    /**
     * Change the value of isOpenShareDialogSuccess to false after it is consumed.
     */
    fun resetIsOpenShareDialogSuccess() {
        viewModelScope.launch {
            _state.update {
                it.copy(isOpenShareDialogSuccess = null)
            }
        }
    }
}