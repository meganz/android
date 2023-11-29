package mega.privacy.android.app.presentation.bottomsheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import javax.inject.Inject

/**
 * View model to be shared by legacy screens where there isn't a view model to handle download state following our architecture (like [NodeOptionsBottomSheetDialogFragment] and its callers)
 * It can receive download events through a state flow instead of of directly calling legacy download methods
 * Once these screens are migrated to compose this view model should be removed and the state added to the corresponding view model (the same state that will trigger the event to show the bottom sheet).
 */
@HiltViewModel
class NodeOptionsDownloadViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getChatFileUseCase: GetChatFileUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<StateEventWithContent<TransferTriggerEvent>>(consumed())

    /**
     * State flow that emits [TransferTriggerEvent]s
     */
    val state = _state.asStateFlow()

    /**
     * DownloadsWorker is still under feature flag, so this method should be used to check the usage of this view model
     * This should be removed once the feature flag is removed
     */
    suspend fun shouldDownloadWithDownloadWorker() =
        getFeatureFlagValueUseCase(AppFeatures.DownloadWorker)

    /**
     * Triggers the event related to download a node with [nodeId]
     * @param nodeId
     */
    fun onDownloadClicked(nodeId: NodeId) {
        viewModelScope.launch {
            val nodes = listOf(getNodeByIdUseCase(nodeId)).filterNotNull()
            _state.update {
                triggered(TransferTriggerEvent.StartDownloadNode(nodes))
            }
        }
    }

    /**
     * Triggers the event related to download a chat node
     * @param chatId
     * @param messageId
     */
    fun onDownloadClicked(chatId: Long, messageId: Long) {
        viewModelScope.launch {
            getChatFileUseCase(chatId, messageId)?.let { chatFile ->
                _state.update {
                    triggered(TransferTriggerEvent.StartDownloadNode(listOf(chatFile)))
                }
            }
        }
    }

    /**
     * Triggers the event related to download a node
     * @param node
     */
    fun onDownloadClicked(node: TypedNode) {
        _state.update {
            triggered(TransferTriggerEvent.StartDownloadNode(listOf(node)))
        }
    }

    /**
     * Triggers the event related to save offline a node with [nodeId]
     * @param nodeId
     */
    fun onSaveOfflineClicked(nodeId: NodeId) {
        viewModelScope.launch {
            val node = getNodeByIdUseCase(nodeId)
            _state.update {
                triggered(TransferTriggerEvent.StartDownloadForOffline(node))
            }
        }
    }

    /**
     * Triggers the event related to save offline a chat node
     * @param chatId
     * @param messageId
     */
    fun onSaveOfflineClicked(chatId: Long, messageId: Long) {
        viewModelScope.launch {
            getChatFileUseCase(chatId, messageId)?.let { chatFile ->
                _state.update {
                    triggered(TransferTriggerEvent.StartDownloadForOffline(chatFile))
                }
            }
        }
    }

    /**
     * Triggers the event related to save offline a node
     * @param node
     */
    fun onSaveOfflineClicked(node: TypedNode) {
        _state.update {
            triggered(TransferTriggerEvent.StartDownloadForOffline(node))
        }
    }

    /**
     * consume the event once it's processed by the view
     */
    fun consumeDownloadEvent() {
        _state.update { consumed() }
    }
}