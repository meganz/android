package mega.privacy.android.app.presentation.transfers.startdownload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferEvent
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferJobInProgress
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferViewState
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.transfer.DownloadNodesEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.BroadcastOfflineFileAvailabilityUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflinePathForNodeUseCase
import mega.privacy.android.domain.usecase.offline.SaveOfflineNodeInformationUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetDownloadLocationForNodeUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.StartDownloadUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View model to handle start downloading
 */
@HiltViewModel
class StartDownloadTransfersViewModel @Inject constructor(
    private val getOfflinePathForNodeUseCase: GetOfflinePathForNodeUseCase,
    private val getDownloadLocationForNodeUseCase: GetDownloadLocationForNodeUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
    private val saveOfflineNodeInformationUseCase: SaveOfflineNodeInformationUseCase,
    private val broadcastOfflineFileAvailabilityUseCase: BroadcastOfflineFileAvailabilityUseCase,
    private val clearActiveTransfersIfFinishedUseCase: ClearActiveTransfersIfFinishedUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
) : ViewModel() {

    private var currentInProgressJob: Job? = null

    private val _uiState = MutableStateFlow(StartDownloadTransferViewState())

    /**
     * the state of the view
     */
    internal val uiState = _uiState.asStateFlow()

    /**
     * It starts downloading the node with the appropriate use case
     * @param siblingNodes the [Node]s to be download, they must belong to same parent folder
     */
    fun startDownloadNodes(siblingNodes: List<Node>) {
        if (siblingNodes.isEmpty()) return
        val firstSibling = siblingNodes.first()
        val parentId = firstSibling.parentId
        if (!siblingNodes.all { it.parentId == parentId }) {
            Timber.e("All nodes must have the same parent")
            _uiState.updateEventAndClearProgress(StartDownloadTransferEvent.Message.TransferCancelled)
        } else {
            currentInProgressJob = viewModelScope.launch {
                startDownloadNodes(
                    siblingNodes,
                    getPath = {
                        getDownloadLocationForNodeUseCase(firstSibling)
                    },
                )
            }
        }
    }

    /**
     * It starts downloading the node for offline with the appropriate use case
     * @param nodes the [Node] to be saved offline
     */
    fun startDownloadForOffline(nodes: Node) {
        currentInProgressJob = viewModelScope.launch {
            startDownloadNodes(
                nodes = listOf(nodes),
                getPath = {
                    getOfflinePathForNodeUseCase(nodes)
                },
                toDoAfterProcessing = {
                    saveOfflineNodeInformationUseCase(nodes.id)
                    broadcastOfflineFileAvailabilityUseCase(nodes.id.longValue)
                }
            )
        }
    }

    /**
     * common logic to start downloading nodes, either for offline or ordinary download
     */
    private suspend fun startDownloadNodes(
        nodes: List<Node>,
        toDoAfterProcessing: (suspend () -> Unit)? = null,
        getPath: suspend () -> String?,
    ) {
        if (!checkAndHandleIsDeviceConnected()) {
            return
        }
        clearActiveTransfersIfFinishedUseCase(TransferType.DOWNLOAD)
        _uiState.update {
            it.copy(jobInProgressState = StartDownloadTransferJobInProgress.ProcessingFiles)
        }
        var lastError: Throwable? = null
        val terminalEvent = runCatching {
            getPath().also {
                if (it.isNullOrBlank()) {
                    throw NullPointerException("path not found!")
                }
            }
        }.onFailure { lastError = it }
            .getOrNull()?.let { path ->
                startDownloadUseCase(
                    destinationPath = path,
                    nodes = nodes,
                    appData = null,
                    isHighPriority = false
                ).catch {
                    lastError = it
                    Timber.e(it)
                }.onCompletion {
                    if (it is CancellationException) {
                        _uiState.updateEventAndClearProgress(StartDownloadTransferEvent.Message.TransferCancelled)
                    }
                }.firstOrNull {
                    it == DownloadNodesEvent.FinishProcessingTransfers || it == DownloadNodesEvent.NotSufficientSpace
                }
            }
        if (terminalEvent == DownloadNodesEvent.FinishProcessingTransfers) toDoAfterProcessing?.invoke()
        _uiState.updateEventAndClearProgress(
            when (terminalEvent) {
                DownloadNodesEvent.NotSufficientSpace -> StartDownloadTransferEvent.Message.NotSufficientSpace
                else -> {
                    StartDownloadTransferEvent.FinishProcessing(
                        exception = lastError?.takeIf { terminalEvent == null },
                        totalNodes = nodes.size,
                    )
                }
            }
        )
    }

    /**
     * Some events need to be consumed to don't be missed or fired more than once
     */
    fun consumeOneOffEvent() {
        _uiState.updateEventAndClearProgress(null)
    }

    /**
     * Cancel current in progress job
     */
    fun cancelCurrentJob() {
        currentInProgressJob?.cancel()
    }

    private fun checkAndHandleIsDeviceConnected() =
        if (!isConnectedToInternetUseCase()) {
            _uiState.updateEventAndClearProgress(StartDownloadTransferEvent.NotConnected)
            false
        } else {
            true
        }

    private fun MutableStateFlow<StartDownloadTransferViewState>.updateEventAndClearProgress(
        event: StartDownloadTransferEvent?,
    ) =
        this.update {
            it.copy(
                oneOffViewEvent = event?.let { triggered(event) } ?: consumed(),
                jobInProgressState = null,
            )
        }
}
