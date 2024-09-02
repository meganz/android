package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.model.NodeAttachmentBottomSheetUiState
import mega.privacy.android.domain.entity.node.NodeId
import javax.inject.Inject

/**
 * View Model class for [NodeAttachmentBottomSheetDialogFragment]
 */
@HiltViewModel
internal class NodeAttachmentBottomSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * Node Id from [SavedStateHandle]
     */
    private val nodeId = NodeId(savedStateHandle.get<Long>(NODE_HANDLE) ?: -1L)

    /**
     * private mutable UI state
     */
    private val _uiState = MutableStateFlow(NodeAttachmentBottomSheetUiState(nodeId = nodeId))

    /**
     * public immutable UI State for view
     */
    val uiState = _uiState.asStateFlow()

    init {

    }

    private fun handleError() {
        _uiState.update {
            it.copy(
                errorEvent = triggered(true)
            )
        }
    }

    fun onErrorEventConsumed() =
        _uiState.update { state -> state.copy(errorEvent = consumed()) }

    companion object {
        const val NODE_HANDLE = "handle"
    }
}

