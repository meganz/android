package mega.privacy.android.app.presentation.transfers.attach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.StorageStatePayWallException
import mega.privacy.android.domain.usecase.chat.AttachMultipleNodesUseCase
import mega.privacy.android.domain.usecase.chat.GetNodesToAttachUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Node attachment view model
 *
 */
@HiltViewModel
class NodeAttachmentViewModel @Inject constructor(
    private val getNodesToAttachUseCase: GetNodesToAttachUseCase,
    private val attachMultipleNodesUseCase: AttachMultipleNodesUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NodeAttachmentUiState())

    /**
     * Ui state
     */
    val uiState = _uiState.asStateFlow()

    /**
     * Start attach nodes
     *
     * @param nodeIds
     */
    fun startAttachNodes(nodeIds: List<NodeId>) {
        _uiState.update { state ->
            state.copy(event = NodeAttachmentEvent.AttachNode(nodeIds))
        }
    }

    /**
     * Get nodes to attach
     *
     */
    fun getNodesToAttach(nodeIds: List<NodeId>) {
        viewModelScope.launch {
            runCatching {
                getNodesToAttachUseCase(nodeIds)
            }.onSuccess {
                if (it.isNotEmpty()) {
                    _uiState.update { state ->
                        state.copy(event = NodeAttachmentEvent.SelectChat(it))
                    }
                } else {
                    Timber.d("No nodes to attach")
                }
            }.onFailure {
                if (it is StorageStatePayWallException) {
                    _uiState.update { state ->
                        state.copy(event = NodeAttachmentEvent.ShowOverDiskQuotaPaywall)
                    }
                }
                Timber.e(it)
            }
        }
    }

    /**
     * Attach nodes to chat
     *
     * @param nodeIds
     * @param chatIds
     */
    fun attachNodesToChat(nodeIds: List<NodeId>, chatIds: LongArray) {
        viewModelScope.launch {
            runCatching {
                attachMultipleNodesUseCase(nodeIds, chatIds)
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(event = NodeAttachmentEvent.AttachNodeSuccess(chatIds.toList()))
                }
                Timber.d("Nodes attached to chat")
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Mark event handled
     *
     */
    fun markEventHandled() {
        _uiState.update { state ->
            state.copy(event = null)
        }
    }
}