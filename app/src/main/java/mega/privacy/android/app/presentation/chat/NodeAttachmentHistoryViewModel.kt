package mega.privacy.android.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.copynode.CopyRequestState
import mega.privacy.android.app.presentation.copynode.toCopyRequestResult
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CopyChatNodesUseCase
import javax.inject.Inject


/**
 * View Model for [mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity]
 */
@HiltViewModel
class NodeAttachmentHistoryViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val copyChatNodesUseCase: CopyChatNodesUseCase,
) : ViewModel() {

    private val _copyResultFlow = MutableStateFlow<CopyRequestState?>(null)

    /**
     * Flow of [CopyRequestState] to notify the result of the copy operation.
     */
    val copyResultFlow = _copyResultFlow.asStateFlow()

    /**
     * Get latest [StorageState] from [MonitorStorageStateEventUseCase] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()

    /**
     * Is online
     *
     * @return
     */
    fun isOnline(): Boolean = isConnectedToInternetUseCase()

    /**
     * Copies the nodes to the destination node
     *
     * @param chatId The chat id
     * @param messageIds The list of message ids
     * @param newNodeParent The destination node handle
     */
    fun copyChatNodes(
        chatId: Long,
        messageIds: MutableList<Long>,
        newNodeParent: Long,
    ) = viewModelScope.launch {
        runCatching {
            copyChatNodesUseCase(
                chatId = chatId,
                messageIds = messageIds,
                newNodeParent = NodeId(newNodeParent)
            )
        }.onSuccess { result ->
            _copyResultFlow.update {
                CopyRequestState(result = result.toCopyRequestResult())
            }
        }.onFailure { throwable ->
            _copyResultFlow.update {
                CopyRequestState(error = throwable)
            }
        }
    }

    /**
     * Clears the copy result after consuming the value
     */
    fun copyResultConsumed() {
        _copyResultFlow.value = null
    }

}
