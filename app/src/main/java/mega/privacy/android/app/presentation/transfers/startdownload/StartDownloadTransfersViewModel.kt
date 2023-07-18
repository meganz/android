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
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.DownloadNodesEvent
import mega.privacy.android.domain.usecase.BroadcastOfflineFileAvailabilityUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.downloads.GetDefaultDownloadPathForNodeUseCase
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationUseCase
import mega.privacy.android.domain.usecase.offline.SaveOfflineNodeInformationUseCase
import mega.privacy.android.domain.usecase.transfer.StartDownloadUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View model to handle start downloading
 */
@HiltViewModel
class StartDownloadTransfersViewModel @Inject constructor(
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getDefaultDownloadPathForNodeUseCase: GetDefaultDownloadPathForNodeUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
    private val getOfflineNodeInformationUseCase: GetOfflineNodeInformationUseCase,
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
    private val saveOfflineNodeInformationUseCase: SaveOfflineNodeInformationUseCase,
    private val broadcastOfflineFileAvailabilityUseCase: BroadcastOfflineFileAvailabilityUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
) : ViewModel() {

    private var currentInProgressJob: Job? = null

    private val _uiState = MutableStateFlow(StartDownloadTransferViewState())

    /**
     * the state of the view
     */
    internal val uiState = _uiState.asStateFlow()

    /**
     * It starts downloading the node with the appropriate use case
     * @param typedNodes the [TypedNode]s to be download, they must belong to same parent folder
     */
    fun startDownloadNode(typedNodes: List<TypedNode>) {
        if (typedNodes.isEmpty()) return
        currentInProgressJob = viewModelScope.launch {
            startDownloadNode(typedNodes) {
                (getNodeByIdUseCase(typedNodes.first().parentId) as? FolderNode)?.let { parent ->
                    getDefaultDownloadPathForNodeUseCase(parent)
                }
            }
        }
    }

    /**
     * It starts downloading the node for offline with the appropriate use case
     * @param typedNode the [TypedNode] to be saved offline
     */
    fun startDownloadForOffline(typedNode: TypedNode) {
        currentInProgressJob = viewModelScope.launch {
            startDownloadNode(
                typedNodes = listOf(typedNode),
                getPath = { getOfflineFileUseCase(getOfflineNodeInformationUseCase(typedNode)).path },
                toDoAfterProcessing = {
                    saveOfflineNodeInformationUseCase(typedNode.id)
                    broadcastOfflineFileAvailabilityUseCase(typedNode.id.longValue)
                }
            )
        }
    }

    /**
     * common logic to start downloading nodes, either for offline or ordinary download
     */
    private suspend fun startDownloadNode(
        typedNodes: List<TypedNode>,
        toDoAfterProcessing: (suspend () -> Unit)? = null,
        getPath: suspend () -> String?,
    ) {
        if (!checkAndHandleIsDeviceConnected()) {
            return
        }
        _uiState.update {
            it.copy(jobInProgressState = StartDownloadTransferJobInProgress.ProcessingFiles)
        }
        var lastError: Throwable? = null
        val terminalEvent = runCatching { getPath() }
            .onFailure { lastError = it }
            .getOrNull()?.let { path ->
                startDownloadUseCase(
                    destinationPath = path,
                    nodes = typedNodes,
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
                        totalNodes = typedNodes.size,
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
        if (!monitorConnectivityUseCase().value) {
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