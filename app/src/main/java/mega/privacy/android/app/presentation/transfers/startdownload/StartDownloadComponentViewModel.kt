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
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.middlelayer.iar.OnCompleteListener
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.transfers.TransfersConstants
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferEvent
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferJobInProgress
import mega.privacy.android.app.presentation.transfers.startdownload.model.StartDownloadTransferViewState
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.BroadcastOfflineFileAvailabilityUseCase
import mega.privacy.android.domain.usecase.file.TotalFileSizeOfNodesUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.GetFilePreviewDownloadPathUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflinePathForNodeUseCase
import mega.privacy.android.domain.usecase.offline.SaveOfflineNodeInformationUseCase
import mega.privacy.android.domain.usecase.setting.IsAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.setting.SetAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetCurrentDownloadSpeedUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetOrCreateStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.StartDownloadsWithWorkerUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * View model to handle start downloading
 */
@HiltViewModel
internal class StartDownloadComponentViewModel @Inject constructor(
    private val getOfflinePathForNodeUseCase: GetOfflinePathForNodeUseCase,
    private val getOrCreateStorageDownloadLocationUseCase: GetOrCreateStorageDownloadLocationUseCase,
    private val getFilePreviewDownloadPathUseCase: GetFilePreviewDownloadPathUseCase,
    private val startDownloadsWithWorkerUseCase: StartDownloadsWithWorkerUseCase,
    private val saveOfflineNodeInformationUseCase: SaveOfflineNodeInformationUseCase,
    private val broadcastOfflineFileAvailabilityUseCase: BroadcastOfflineFileAvailabilityUseCase,
    private val clearActiveTransfersIfFinishedUseCase: ClearActiveTransfersIfFinishedUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val totalFileSizeOfNodesUseCase: TotalFileSizeOfNodesUseCase,
    private val fileSizeStringMapper: FileSizeStringMapper,
    private val isAskBeforeLargeDownloadsSettingUseCase: IsAskBeforeLargeDownloadsSettingUseCase,
    private val setAskBeforeLargeDownloadsSettingUseCase: SetAskBeforeLargeDownloadsSettingUseCase,
    private val monitorOngoingActiveTransfersUseCase: MonitorOngoingActiveTransfersUseCase,
    private val getCurrentDownloadSpeedUseCase: GetCurrentDownloadSpeedUseCase,
) : ViewModel() {

    private var currentInProgressJob: Job? = null

    private val _uiState = MutableStateFlow(StartDownloadTransferViewState())

    /**
     * the state of the view
     */
    internal val uiState = _uiState.asStateFlow()

    init {
        checkRating()
    }

    /**
     * It starts downloading the related nodes, asking for confirmation in case of large transfers if corresponds
     * @param transferTriggerEvent the event that triggered this download
     */
    fun startDownload(
        transferTriggerEvent: TransferTriggerEvent,
    ) {
        if (checkAndHandleDeviceIsNotConnected()) {
            return
        }
        viewModelScope.launch {
            if (transferTriggerEvent.nodes.isEmpty()) {
                Timber.e("Node in $transferTriggerEvent must exist")
                _uiState.updateEventAndClearProgress(StartDownloadTransferEvent.Message.TransferCancelled)
            } else if (!checkAndHandleNeedConfirmationForLargeDownload(transferTriggerEvent)) {
                startDownloadWithoutConfirmation(transferTriggerEvent)
            }
        }
    }

    /**
     * It starts downloading the related nodes, without asking confirmation for large transfers (because it's already asked)
     * @param transferTriggerEvent the event that triggered this download
     * @param saveDoNotAskAgainForLargeTransfers if true, it will save in settings to don't ask again for confirmation on large files
     */
    fun startDownloadWithoutConfirmation(
        transferTriggerEvent: TransferTriggerEvent,
        saveDoNotAskAgainForLargeTransfers: Boolean = false,
    ) {
        if (saveDoNotAskAgainForLargeTransfers) {
            viewModelScope.launch {
                setAskBeforeLargeDownloadsSettingUseCase(askForConfirmation = false)
            }
        }
        val node = transferTriggerEvent.nodes.firstOrNull()
        if (node == null) {
            Timber.e("Node in $transferTriggerEvent must exist")
            _uiState.updateEventAndClearProgress(StartDownloadTransferEvent.Message.TransferCancelled)
        } else {
            _uiState.update {
                it.copy(transferTriggerEvent = transferTriggerEvent)
            }
            when (transferTriggerEvent) {
                is TransferTriggerEvent.StartDownloadForOffline -> {
                    startDownloadForOffline(node)
                }

                is TransferTriggerEvent.StartDownloadNode -> {
                    startDownloadNodes(transferTriggerEvent.nodes)
                }

                is TransferTriggerEvent.StartDownloadForPreview -> {
                    startDownloadNodeForPreview(node)
                }
            }
        }
    }

    /**
     * It starts downloading the node for preview with the appropriate use case
     * @param node the [Node] to be downloaded for preview
     */
    private fun startDownloadNodeForPreview(node: TypedNode) {
        currentInProgressJob = viewModelScope.launch {
            startDownloadNodes(
                nodes = listOf(node),
                getPath = {
                    getFilePreviewDownloadPathUseCase()
                },
            )
        }
    }

    /**
     * It starts downloading the nodes with the appropriate use case
     * @param siblingNodes the [Node]s to be download, they must belong to same parent folder
     */
    private fun startDownloadNodes(siblingNodes: List<TypedNode>) {
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
                        getOrCreateStorageDownloadLocationUseCase()?.ensureSuffix(File.separator)
                    },
                )
            }
        }
    }

    /**
     * It starts downloading the node for offline with the appropriate use case
     * @param node the [Node] to be saved offline
     */
    private fun startDownloadForOffline(node: TypedNode) {
        currentInProgressJob = viewModelScope.launch {
            startDownloadNodes(
                nodes = listOf(node),
                getPath = {
                    getOfflinePathForNodeUseCase(node)
                },
                toDoAfterProcessing = {
                    saveOfflineNodeInformationUseCase(node.id)
                    broadcastOfflineFileAvailabilityUseCase(node.id.longValue)
                }
            )
        }
    }

    /**
     * common logic to start downloading nodes, either for offline or ordinary download
     */
    private suspend fun startDownloadNodes(
        nodes: List<TypedNode>,
        toDoAfterProcessing: (suspend () -> Unit)? = null,
        getPath: suspend () -> String?,
    ) {
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
                startDownloadsWithWorkerUseCase(
                    destinationPath = path,
                    nodes = nodes,
                    isHighPriority = false
                ).catch {
                    lastError = it
                    Timber.e(it)
                }.onCompletion {
                    if (it is CancellationException) {
                        _uiState.updateEventAndClearProgress(StartDownloadTransferEvent.Message.TransferCancelled)
                    }
                }.last()
            }
        checkRating()
        if (terminalEvent == MultiTransferEvent.ScanningFoldersFinished) toDoAfterProcessing?.invoke()
        _uiState.updateEventAndClearProgress(
            when (terminalEvent) {
                MultiTransferEvent.InsufficientSpace -> StartDownloadTransferEvent.Message.NotSufficientSpace
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

    private fun checkAndHandleDeviceIsNotConnected() =
        if (!isConnectedToInternetUseCase()) {
            _uiState.updateEventAndClearProgress(StartDownloadTransferEvent.NotConnected)
            true
        } else {
            false
        }

    /**
     * Checks if confirmation dialog for large download should be shown and updates uiState if so
     *
     * @return true if the state has been handled to ask for confirmation, so no extra action should be done
     */
    private suspend fun checkAndHandleNeedConfirmationForLargeDownload(transferTriggerEvent: TransferTriggerEvent): Boolean {
        if (isAskBeforeLargeDownloadsSettingUseCase()) {
            val size = totalFileSizeOfNodesUseCase(transferTriggerEvent.nodes)
            if (size > TransfersConstants.CONFIRM_SIZE_MIN_BYTES) {
                _uiState.updateEventAndClearProgress(
                    StartDownloadTransferEvent.ConfirmLargeDownload(
                        fileSizeStringMapper(size), transferTriggerEvent
                    )
                )
                return true
            }
        }
        return false
    }

    private var checkShowRating = true
    private fun checkRating() {
        //check download speed and size to show rating
        if (checkShowRating) {
            viewModelScope.launch {
                monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD).conflate().takeWhile {
                    checkShowRating
                }.collect { (transferTotals, paused) ->
                    if (checkShowRating && !paused && transferTotals.totalFileTransfers > 0) {
                        val currentDownloadSpeed = getCurrentDownloadSpeedUseCase()
                        RatingHandlerImpl().showRatingBaseOnSpeedAndSize(
                            size = transferTotals.totalFileTransfers.toLong(),
                            speed = currentDownloadSpeed.toLong(),
                            listener = object : OnCompleteListener {
                                override fun onComplete() {
                                    checkShowRating = false
                                }


                                override fun onConditionsUnmet() {
                                    checkShowRating = false
                                }
                            }
                        )
                    }
                }
            }
        }
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

    private fun String.ensureSuffix(suffix: String) =
        if (this.endsWith(suffix)) this else this.plus(suffix)
}
