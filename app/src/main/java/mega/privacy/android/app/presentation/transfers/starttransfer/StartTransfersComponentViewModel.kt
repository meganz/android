package mega.privacy.android.app.presentation.transfers.starttransfer

import android.net.Uri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import mega.privacy.android.app.middlelayer.iar.OnCompleteListener
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.transfers.TransfersConstants
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferJobInProgress
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferViewState
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.usecase.SetStorageDownloadAskAlwaysUseCase
import mega.privacy.android.domain.usecase.SetStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.chat.message.SendChatAttachmentsUseCase
import mega.privacy.android.domain.usecase.file.TotalFileSizeOfNodesUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.GetFilePreviewDownloadPathUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflinePathForNodeUseCase
import mega.privacy.android.domain.usecase.setting.IsAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.setting.SetAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorActiveTransferFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.SetAskedResumeTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.ShouldAskForResumeTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetCurrentDownloadSpeedUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetOrCreateStorageDownloadLocationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.SaveDoNotPromptToSaveDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ShouldAskDownloadDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ShouldPromptToSaveDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.StartDownloadsWithWorkerUseCase
import mega.privacy.android.domain.usecase.transfers.offline.SaveOfflineNodesToDevice
import mega.privacy.android.domain.usecase.transfers.offline.SaveUriToDeviceUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseAllTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.GetCurrentUploadSpeedUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.StartUploadsWithWorkerUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * View model to handle start transfers component
 */
@HiltViewModel
internal class StartTransfersComponentViewModel @Inject constructor(
    private val getOfflinePathForNodeUseCase: GetOfflinePathForNodeUseCase,
    private val getOrCreateStorageDownloadLocationUseCase: GetOrCreateStorageDownloadLocationUseCase,
    private val getFilePreviewDownloadPathUseCase: GetFilePreviewDownloadPathUseCase,
    private val startDownloadsWithWorkerUseCase: StartDownloadsWithWorkerUseCase,
    private val clearActiveTransfersIfFinishedUseCase: ClearActiveTransfersIfFinishedUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val totalFileSizeOfNodesUseCase: TotalFileSizeOfNodesUseCase,
    private val fileSizeStringMapper: FileSizeStringMapper,
    private val isAskBeforeLargeDownloadsSettingUseCase: IsAskBeforeLargeDownloadsSettingUseCase,
    private val setAskBeforeLargeDownloadsSettingUseCase: SetAskBeforeLargeDownloadsSettingUseCase,
    private val monitorOngoingActiveTransfersUseCase: MonitorOngoingActiveTransfersUseCase,
    private val getCurrentDownloadSpeedUseCase: GetCurrentDownloadSpeedUseCase,
    private val shouldAskDownloadDestinationUseCase: ShouldAskDownloadDestinationUseCase,
    private val shouldPromptToSaveDestinationUseCase: ShouldPromptToSaveDestinationUseCase,
    private val saveDoNotPromptToSaveDestinationUseCase: SaveDoNotPromptToSaveDestinationUseCase,
    private val setStorageDownloadAskAlwaysUseCase: SetStorageDownloadAskAlwaysUseCase,
    private val setStorageDownloadLocationUseCase: SetStorageDownloadLocationUseCase,
    private val monitorActiveTransferFinishedUseCase: MonitorActiveTransferFinishedUseCase,
    private val sendChatAttachmentsUseCase: SendChatAttachmentsUseCase,
    private val shouldAskForResumeTransfersUseCase: ShouldAskForResumeTransfersUseCase,
    private val setAskedResumeTransfersUseCase: SetAskedResumeTransfersUseCase,
    private val pauseAllTransfersUseCase: PauseAllTransfersUseCase,
    private val startUploadWithWorkerUseCase: StartUploadsWithWorkerUseCase,
    private val saveOfflineNodesToDevice: SaveOfflineNodesToDevice,
    private val saveUriToDeviceUseCase: SaveUriToDeviceUseCase,
    private val getCurrentUploadSpeedUseCase: GetCurrentUploadSpeedUseCase,
) : ViewModel(), DefaultLifecycleObserver {

    private var currentInProgressJob: Job? = null

    private val _uiState = MutableStateFlow(StartTransferViewState())

    /**
     * the state of the view
     */
    internal val uiState = _uiState.asStateFlow()

    init {
        checkDownloadRating()
        monitorDownloadFinish()
        checkUploadRating()
        monitorUploadFinish()
    }

    /**
     * It starts the triggered transfer, asking for confirmation in case of large transfers if corresponds
     * @param transferTriggerEvent the event that triggered this transfer
     */
    fun startTransfer(
        transferTriggerEvent: TransferTriggerEvent,
    ) {
        viewModelScope.launch {
            when (transferTriggerEvent) {
                is TransferTriggerEvent.DownloadTriggerEvent -> {
                    val isCopyEvent = transferTriggerEvent is TransferTriggerEvent.CopyTriggerEvent
                    if (!isCopyEvent && checkAndHandleDeviceIsNotConnected()) {
                        return@launch
                    }
                    if (transferTriggerEvent.nodes.isEmpty() && !isCopyEvent) {
                        Timber.e("Node in $transferTriggerEvent must exist")
                        _uiState.updateEventAndClearProgress(StartTransferEvent.Message.TransferCancelled)
                    } else if (!checkAndHandleNeedConfirmationForLargeDownload(transferTriggerEvent)) {
                        startDownloadWithoutConfirmation(transferTriggerEvent)
                    }
                }

                is TransferTriggerEvent.StartChatUpload -> {
                    startChatUploads(
                        chatId = transferTriggerEvent.chatId,
                        uris = transferTriggerEvent.uris,
                        isVoiceClip = transferTriggerEvent.isVoiceClip
                    )
                    checkAndHandleTransfersPaused()
                }

                is TransferTriggerEvent.StartUpload -> {
                    if (checkAndHandleDeviceIsNotConnected()) {
                        return@launch
                    }
                    startUploads(
                        pathsAndNames = transferTriggerEvent.pathsAndNames,
                        destinationId = transferTriggerEvent.destinationId,
                        transferTriggerEvent = transferTriggerEvent,
                    )
                }
            }
        }
    }

    /**
     * It starts downloading the related nodes, without asking confirmation for large transfers (because it's already asked)
     * @param transferTriggerEvent the event that triggered this download
     * @param saveDoNotAskAgainForLargeTransfers if true, it will save in settings to don't ask again for confirmation on large files
     */
    fun startDownloadWithoutConfirmation(
        transferTriggerEvent: TransferTriggerEvent.DownloadTriggerEvent,
        saveDoNotAskAgainForLargeTransfers: Boolean = false,
    ) {
        if (saveDoNotAskAgainForLargeTransfers) {
            viewModelScope.launch {
                runCatching { setAskBeforeLargeDownloadsSettingUseCase(askForConfirmation = false) }
                    .onFailure { Timber.e(it) }
            }
        }
        val node = transferTriggerEvent.nodes.firstOrNull()
        val isCopyEvent = transferTriggerEvent is TransferTriggerEvent.CopyTriggerEvent
        if (node == null && !isCopyEvent) {
            Timber.e("Node in $transferTriggerEvent must exist")
            _uiState.updateEventAndClearProgress(StartTransferEvent.Message.TransferCancelled)
        } else {
            lastDownloadTriggerEvent = transferTriggerEvent
            lastTransferStartedHere = true
            when (transferTriggerEvent) {
                is TransferTriggerEvent.StartDownloadForOffline -> {
                    startDownloadForOffline(transferTriggerEvent)
                }

                is TransferTriggerEvent.StartDownloadNode, is TransferTriggerEvent.CopyOfflineNode, is TransferTriggerEvent.CopyUri -> {
                    viewModelScope.launch {
                        if (runCatching { shouldAskDownloadDestinationUseCase() }.getOrDefault(false)) {
                            _uiState.updateEventAndClearProgress(
                                StartTransferEvent.AskDestination(
                                    transferTriggerEvent
                                )
                            )
                        } else {
                            runCatching { getOrCreateStorageDownloadLocationUseCase() }
                                .onFailure { Timber.e(it) }
                                .getOrNull()?.let { location ->
                                    startDownloadNodes(transferTriggerEvent, location)
                                }
                        }
                    }
                }

                is TransferTriggerEvent.StartDownloadForPreview -> {
                    startDownloadNodeForPreview(transferTriggerEvent)
                }
            }
        }
    }

    /**
     * Start download with the destination manually set by the user
     * @param startDownloadNode initial event that triggered this download
     * @param destinationUri the chosen destination
     */
    fun startDownloadWithDestination(
        startDownloadNode: TransferTriggerEvent,
        destinationUri: Uri,
    ) {
        Timber.d("Selected destination $destinationUri")
        viewModelScope.launch {
            startDownloadNodes(startDownloadNode, destinationUri.toString())
            if (runCatching { shouldPromptToSaveDestinationUseCase() }.getOrDefault(false)) {
                _uiState.update {
                    it.copy(promptSaveDestination = triggered(destinationUri.toString()))
                }
            }
        }
    }

    /**
     * It starts downloading the nodes with the appropriate use case
     * @param startDownloadNode the [TransferTriggerEvent] that starts this download
     * @param destination the destination where to download the nodes
     */
    private suspend fun startDownloadNodes(
        startDownloadNode: TransferTriggerEvent,
        destination: String,
    ) {
        when (startDownloadNode) {
            is TransferTriggerEvent.StartDownloadNode -> {
                val nodes = startDownloadNode.nodes
                if (nodes.isEmpty()) return
                currentInProgressJob = viewModelScope.launch {
                    startDownloadNodes(
                        nodes = nodes,
                        isHighPriority = startDownloadNode.isHighPriority,
                        getUri = {
                            destination.ensureSuffix(File.separator)
                        },
                        transferTriggerEvent = startDownloadNode
                    )
                }
            }

            is TransferTriggerEvent.CopyOfflineNode -> {
                saveOfflineNodeToDevice(startDownloadNode, destination)
            }

            is TransferTriggerEvent.CopyUri -> {
                saveUriToDevice(startDownloadNode, destination)
            }

            else -> Unit
        }
    }

    private suspend fun saveUriToDevice(
        startDownloadNode: TransferTriggerEvent.CopyUri,
        destination: String,
    ) {
        runCatching {
            saveUriToDeviceUseCase(
                name = startDownloadNode.name,
                source = UriPath(startDownloadNode.uri.toString()),
                destination = UriPath(destination)
            )
        }.onSuccess {
            _uiState.updateEventAndClearProgress(StartTransferEvent.Message.FinishCopyUri)
        }.onFailure {
            Timber.e(it)
        }
    }

    private suspend fun saveOfflineNodeToDevice(
        startDownloadNode: TransferTriggerEvent.CopyOfflineNode,
        destination: String,
    ) {
        runCatching {
            saveOfflineNodesToDevice(startDownloadNode.nodeIds, UriPath(destination))
        }.onSuccess { totalFiles ->
            _uiState.updateEventAndClearProgress(
                StartTransferEvent.MessagePlural.FinishDownloading(
                    totalFiles
                )
            )
        }.onFailure {
            Timber.e(it)
        }
    }

    /**
     * It starts downloading the node for preview with the appropriate use case
     * @param event the [TransferTriggerEvent.StartDownloadForPreview] event that starts this download
     */
    private fun startDownloadNodeForPreview(event: TransferTriggerEvent.StartDownloadForPreview) {
        if (event.node == null) {
            return
        }
        currentInProgressJob = viewModelScope.launch {
            startDownloadNodes(
                nodes = listOf(event.node),
                isHighPriority = true,
                getUri = {
                    runCatching { getFilePreviewDownloadPathUseCase() }
                        .onFailure { Timber.e(it) }
                        .getOrNull()
                },
                transferTriggerEvent = event
            )
        }
    }

    /**
     * It starts downloading the node for offline with the appropriate use case
     * @param event the [TransferTriggerEvent.StartDownloadForOffline] event that starts this download
     */
    private fun startDownloadForOffline(event: TransferTriggerEvent.StartDownloadForOffline) {
        if (event.node == null) {
            return
        }
        currentInProgressJob = viewModelScope.launch {
            startDownloadNodes(
                nodes = listOf(event.node),
                event.isHighPriority,
                getUri = {
                    runCatching { getOfflinePathForNodeUseCase(event.node) }
                        .onFailure { Timber.e(it) }
                        .getOrNull()
                },
                transferTriggerEvent = event,
            )
        }
    }

    /**
     * common logic to start downloading nodes, either for offline or ordinary download
     */
    private suspend fun startDownloadNodes(
        nodes: List<TypedNode>,
        isHighPriority: Boolean,
        getUri: suspend () -> String?,
        transferTriggerEvent: TransferTriggerEvent,
    ) {
        monitorDownloadFinishJob?.cancel()
        runCatching { clearActiveTransfersIfFinishedUseCase(TransferType.DOWNLOAD) }
            .onFailure { Timber.e(it) }
        monitorDownloadFinish()
        _uiState.update {
            it.copy(jobInProgressState = StartTransferJobInProgress.ScanningTransfers)
        }
        var lastError: Throwable? = null
        var startMessageShown = false
        val terminalEvent = runCatching {
            getUri().also {
                if (it.isNullOrBlank()) {
                    throw NullPointerException("path not found!")
                }
            }
        }.onFailure { lastError = it }
            .getOrNull()?.let { uri ->
                startDownloadsWithWorkerUseCase(
                    destinationPathOrUri = uri,
                    nodes = nodes,
                    isHighPriority = isHighPriority,
                ).onEach { event ->
                    val singleTransferEvent = (event as? MultiTransferEvent.SingleTransferEvent)
                    // clear scanning transfers state
                    if (_uiState.value.jobInProgressState == StartTransferJobInProgress.ScanningTransfers &&
                        singleTransferEvent?.scanningFinished == true
                    ) {
                        _uiState.update {
                            it.copy(jobInProgressState = null)
                        }
                    }
                    //show start message as soon as an event with all transfers updated is received
                    if (!startMessageShown && singleTransferEvent?.allTransfersUpdated == true) {
                        startMessageShown = true
                        updateWithDownloadFinishProcessing(
                            singleTransferEvent,
                            transferTriggerEvent,
                            nodes.size,
                        )
                    }
                }.catch {
                    lastError = it
                    Timber.e(it)
                }.onCompletion {
                    if (it is CancellationException) {
                        _uiState.updateEventAndClearProgress(StartTransferEvent.Message.TransferCancelled)
                    }
                }.last()
            }
        checkDownloadRating()
        when {
            terminalEvent == MultiTransferEvent.InsufficientSpace ->
                _uiState.updateEventAndClearProgress(StartTransferEvent.Message.NotSufficientSpace)

            !startMessageShown -> updateWithDownloadFinishProcessing(
                terminalEvent as? MultiTransferEvent.SingleTransferEvent,
                transferTriggerEvent,
                nodes.size,
                lastError?.takeIf { terminalEvent == null },
            )
        }
    }

    private fun updateWithDownloadFinishProcessing(
        event: MultiTransferEvent.SingleTransferEvent?,
        transferTriggerEvent: TransferTriggerEvent,
        totalNodes: Int,
        error: Throwable? = null,
    ) {
        _uiState.updateEventAndClearProgress(
            StartTransferEvent.FinishDownloadProcessing(
                exception = error,
                totalNodes = totalNodes,
                totalFiles = event?.startedFiles ?: 0,
                totalAlreadyDownloaded = event?.alreadyTransferred ?: 0,
                triggerEvent = transferTriggerEvent,
            )
        )
    }

    private suspend fun startChatUploads(
        chatId: Long,
        uris: List<Uri>,
        isVoiceClip: Boolean = false,
    ) {
        runCatching { clearActiveTransfersIfFinishedUseCase(TransferType.CHAT_UPLOAD) }
            .onFailure { Timber.e(it) }
        runCatching {
            sendChatAttachmentsUseCase(
                uris.map { it.toString() }.associateWith { null }, isVoiceClip, chatId
            )
        }.onSuccess {
            _uiState.updateEventAndClearProgress(null)
        }.onFailure {
            Timber.e(it)
            _uiState.updateEventAndClearProgress(
                if (it is NotEnoughStorageException) {
                    StartTransferEvent.Message.NotSufficientSpace
                } else {
                    StartTransferEvent.Message.TransferCancelled
                }
            )
        }
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
        _uiState.update {
            it.copy(
                jobInProgressState = null,
            )
        }
    }

    /**
     * consume prompt save destination event
     */
    fun consumePromptSaveDestination() {
        _uiState.update {
            it.copy(promptSaveDestination = consumed())
        }
    }

    /**
     * Save selected destination as location for future downloads
     */
    fun saveDestination(destination: String) {
        viewModelScope.launch {
            runCatching {
                setStorageDownloadLocationUseCase(destination)
                setStorageDownloadAskAlwaysUseCase(false)
            }.onFailure {
                Timber.e("Error saving the destination:\n$it")
            }
        }
    }

    /**
     * Save setting to don't prompt the user again to save selected destination
     */
    fun doNotPromptToSaveDestinationAgain() {
        viewModelScope.launch {
            runCatching {
                saveDoNotPromptToSaveDestinationUseCase()
            }.onFailure {
                Timber.e("Error saving the don't save destination again prompt:\n$it")
            }

        }
    }

    private fun checkAndHandleDeviceIsNotConnected() =
        if (runCatching { isConnectedToInternetUseCase() }.getOrDefault(true)) {
            false
        } else {
            _uiState.updateEventAndClearProgress(StartTransferEvent.NotConnected)
            true
        }

    /**
     * Checks if confirmation dialog for large download should be shown and updates uiState if so
     *
     * @return true if the state has been handled to ask for confirmation, so no extra action should be done
     */
    private suspend fun checkAndHandleNeedConfirmationForLargeDownload(transferTriggerEvent: TransferTriggerEvent.DownloadTriggerEvent): Boolean {
        if (runCatching { isAskBeforeLargeDownloadsSettingUseCase() }.getOrDefault(false)) {
            val size = runCatching { totalFileSizeOfNodesUseCase(transferTriggerEvent.nodes) }
                .getOrDefault(0L)
            if (size > TransfersConstants.CONFIRM_SIZE_MIN_BYTES) {
                _uiState.updateEventAndClearProgress(
                    StartTransferEvent.ConfirmLargeDownload(
                        fileSizeStringMapper(size), transferTriggerEvent
                    )
                )
                return true
            }
        }
        return false
    }

    private var checkShowDownloadRating = true
    private fun checkDownloadRating() {
        //check download speed and size to show rating
        if (checkShowDownloadRating) {
            viewModelScope.launch {
                monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD).conflate().takeWhile {
                    checkShowDownloadRating
                }.catch {
                    Timber.e(it)
                }.collect { (transferTotals, paused) ->
                    if (checkShowDownloadRating && !paused && transferTotals.totalFileTransfers > 0) {
                        val currentDownloadSpeed = getCurrentDownloadSpeedUseCase()
                        RatingHandlerImpl().showRatingBaseOnSpeedAndSize(
                            size = transferTotals.totalFileTransfers.toLong(),
                            speed = currentDownloadSpeed.toLong(),
                            listener = object : OnCompleteListener {
                                override fun onComplete() {
                                    checkShowDownloadRating = false
                                }


                                override fun onConditionsUnmet() {
                                    checkShowDownloadRating = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private var monitorDownloadFinishJob: Job? = null

    /**
     * Monitor finish to send the corresponding event (will display a "Download Finished" snackbar or similar)
     */
    private fun monitorDownloadFinish() {
        monitorDownloadFinishJob?.cancel()
        monitorDownloadFinishJob = viewModelScope.launch {
            monitorActiveTransferFinishedUseCase(TransferType.DOWNLOAD)
                .catch { Timber.e(it) }
                .collect { totalNodes ->
                    if (!lastTransferStartedHere) {
                        yield() //wait for others to listen the event to try to show the snackbar in the same component that started the transfer
                    }
                    if (active) {
                        when (lastDownloadTriggerEvent) {
                            is TransferTriggerEvent.StartDownloadForOffline -> {
                                StartTransferEvent.Message.FinishOffline
                            }

                            is TransferTriggerEvent.StartDownloadNode -> {
                                StartTransferEvent.MessagePlural.FinishDownloading(
                                    totalNodes
                                )
                            }

                            else -> null
                        }?.let { finishEvent ->
                            _uiState.updateEventAndClearProgress(finishEvent)
                        }
                        lastDownloadTriggerEvent = null
                    }
                    lastTransferStartedHere = false
                }
        }
    }

    private fun MutableStateFlow<StartTransferViewState>.updateEventAndClearProgress(
        event: StartTransferEvent?,
    ) = this.update {
        it.copy(
            oneOffViewEvent = event?.let { triggered(event) } ?: consumed(),
            jobInProgressState = null,
        )
    }

    private fun String.ensureSuffix(suffix: String) =
        if (this.endsWith(suffix)) this else this.plus(suffix)

    private var active = false
    private var lastTransferStartedHere = false

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        active = true
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        active = false
    }

    /**
     * Set asked resume transfers.
     */
    fun setAskedResumeTransfers() {
        viewModelScope.launch {
            setAskedResumeTransfersUseCase()
        }
    }

    /**
     * Resume transfers.
     */
    fun resumeTransfers() {
        viewModelScope.launch {
            runCatching { pauseAllTransfersUseCase(false) }
                .onFailure { Timber.e(it) }
        }
    }

    private fun checkAndHandleTransfersPaused() {
        if (runCatching { shouldAskForResumeTransfersUseCase() }.getOrDefault(false)) {
            _uiState.updateEventAndClearProgress(StartTransferEvent.PausedTransfers)
        }
    }

    private suspend fun startUploads(
        pathsAndNames: Map<String, String?>,
        destinationId: NodeId,
        transferTriggerEvent: TransferTriggerEvent.StartUpload,
    ) {
        runCatching { clearActiveTransfersIfFinishedUseCase(TransferType.GENERAL_UPLOAD) }
            .onFailure { Timber.e(it) }

        if (pathsAndNames.isEmpty()) {
            Timber.e("Paths in $pathsAndNames must exist")
            _uiState.updateEventAndClearProgress(StartTransferEvent.Message.TransferCancelled)
            return
        }

        lastUpload = transferTriggerEvent
        lastTransferStartedHere = true
        monitorUploadFinish()
        _uiState.update {
            it.copy(jobInProgressState = StartTransferJobInProgress.ScanningTransfers)
        }
        var startMessageShown = false
        startUploadWithWorkerUseCase(
            pathsAndNames,
            destinationId
        ).catch {
            if (transferTriggerEvent is TransferTriggerEvent.StartUpload.TextFile) {
                lastUploadError = it
            }
            Timber.e(it)
        }.onCompletion {
            if (it is CancellationException) {
                _uiState.updateEventAndClearProgress(StartTransferEvent.Message.TransferCancelled)
            }
        }.collect { event ->
            when (event) {
                is MultiTransferEvent.TransferNotStarted<*> -> {
                    Timber.e(event.exception, "Error starting upload")
                    StartTransferEvent.Message.TransferCancelled
                }

                MultiTransferEvent.InsufficientSpace -> StartTransferEvent.Message.NotSufficientSpace
                is MultiTransferEvent.SingleTransferEvent -> {
                    //show start message as soon as an event with all transfers updated is received
                    if (!startMessageShown && event.scanningFinished) {
                        startMessageShown = true
                        StartTransferEvent.FinishUploadProcessing(
                            totalFiles = pathsAndNames.size,
                            triggerEvent = transferTriggerEvent,
                        )
                    } else {
                        null
                    }
                }
            }?.let {
                _uiState.updateEventAndClearProgress(it)
            }
        }
        checkUploadRating()
    }

    private var monitorUploadFinishJob: Job? = null

    /**
     * Monitor finish to send the corresponding event (will display a "Upload Finished" snackbar or similar)
     */
    private fun monitorUploadFinish() {
        monitorUploadFinishJob?.cancel()
        monitorUploadFinishJob = viewModelScope.launch {
            monitorActiveTransferFinishedUseCase(TransferType.GENERAL_UPLOAD)
                .catch { Timber.e(it) }
                .collect { totalFiles ->
                    if (!lastTransferStartedHere) {
                        yield() //wait for others to listen the event to try to show the snackbar in the same component that started the transfer
                    }
                    if (active) {
                        when (lastUpload) {
                            is TransferTriggerEvent.StartUpload.TextFile -> {
                                val isSuccess = lastUploadError == null
                                val lastUpload =
                                    lastUpload as TransferTriggerEvent.StartUpload.TextFile
                                lastUploadError = null

                                StartTransferEvent.Message.FinishTextFileUpload(
                                    isSuccess = isSuccess,
                                    isEditMode = lastUpload.isEditMode,
                                    isCloudFile = lastUpload.fromHomePage
                                )
                            }

                            is TransferTriggerEvent.StartUpload.Files,
                            is TransferTriggerEvent.StartUpload.CollidedFiles,
                            -> {
                                StartTransferEvent.MessagePlural.FinishUploading(totalFiles)
                            }

                            else -> null
                        }?.let { finishEvent ->
                            _uiState.updateEventAndClearProgress(finishEvent)
                        }
                        lastUpload = null
                    }
                    lastTransferStartedHere = false
                }
        }
    }

    private var checkShowUploadRating = true
    private fun checkUploadRating() {
        //check upload speed and size to show rating
        if (checkShowUploadRating) {
            viewModelScope.launch {
                monitorOngoingActiveTransfersUseCase(TransferType.GENERAL_UPLOAD).conflate()
                    .takeWhile {
                        checkShowUploadRating
                    }.catch {
                        Timber.e(it)
                    }.collect { (transferTotals, paused) ->
                        if (checkShowUploadRating && !paused && transferTotals.totalFileTransfers > 0) {
                            val currentUploadSpeed = getCurrentUploadSpeedUseCase()
                            RatingHandlerImpl().showRatingBaseOnSpeedAndSize(
                                size = transferTotals.totalFileTransfers.toLong(),
                                speed = currentUploadSpeed,
                                listener = object : OnCompleteListener {
                                    override fun onComplete() {
                                        checkShowUploadRating = false
                                    }

                                    override fun onConditionsUnmet() {
                                        checkShowUploadRating = false
                                    }
                                }
                            )
                        }
                    }
            }
        }
    }

    companion object {
        /**
         * The last trigger event that started the download, we need to keep it to know what to do when the download finishes even in other screens
         */
        private var lastDownloadTriggerEvent: TransferTriggerEvent.DownloadTriggerEvent? = null

        /**
         * The last trigger event that started the upload, we need to keep it to know what to do when the upload finishes even in other screens
         */
        private var lastUpload: TransferTriggerEvent.StartUpload? = null

        /**
         * The last upload error, we need to keep it to know what to show when the upload finishes in other screens
         */
        private var lastUploadError: Throwable? = null
    }
}
