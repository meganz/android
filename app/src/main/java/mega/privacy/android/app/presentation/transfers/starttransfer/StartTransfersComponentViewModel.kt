package mega.privacy.android.app.presentation.transfers.starttransfer

import android.net.Uri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.middlelayer.iar.OnCompleteListener
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.transfers.TransfersConstants
import mega.privacy.android.app.presentation.transfers.starttransfer.model.ConfirmLargeDownloadInfo
import mega.privacy.android.app.presentation.transfers.starttransfer.model.SaveDestinationInfo
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferEvent.SlowDownloadPreviewInProgress
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferJobInProgress
import mega.privacy.android.app.presentation.transfers.starttransfer.model.StartTransferViewState
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isPreviewDownload
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.NotEnoughStorageException
import mega.privacy.android.domain.usecase.SetAskForDownloadLocationUseCase
import mega.privacy.android.domain.usecase.SetDownloadLocationUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.canceltoken.InvalidateCancelTokenUseCase
import mega.privacy.android.domain.usecase.chat.message.SendChatAttachmentsUseCase
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import mega.privacy.android.domain.usecase.file.TotalFileSizeOfNodesUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.GetFilePreviewDownloadPathUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflinePathForNodeUseCase
import mega.privacy.android.domain.usecase.setting.IsAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.setting.SetAskBeforeLargeDownloadsSettingUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.DeleteCacheFilesUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileNameFromStringUriUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.active.ClearActiveTransfersIfFinishedUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.SetAskedResumeTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.ShouldAskForResumeTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransfersByIdUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetCurrentDownloadSpeedUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetOrCreateDownloadLocationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.SaveDoNotPromptToSaveDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ShouldAskDownloadDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.ShouldPromptToSaveDestinationUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.StartDownloadsWorkerAndWaitUntilIsStartedUseCase
import mega.privacy.android.domain.usecase.transfers.filespermission.MonitorRequestFilesPermissionDeniedUseCase
import mega.privacy.android.domain.usecase.transfers.filespermission.SetRequestFilesPermissionDeniedUseCase
import mega.privacy.android.domain.usecase.transfers.offline.SaveOfflineNodesToDevice
import mega.privacy.android.domain.usecase.transfers.offline.SaveUriToDeviceUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransfersQueueUseCase
import mega.privacy.android.domain.usecase.transfers.pending.DeleteAllPendingTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.pending.InsertPendingDownloadsForNodesUseCase
import mega.privacy.android.domain.usecase.transfers.pending.InsertPendingUploadsForFilesUseCase
import mega.privacy.android.domain.usecase.transfers.pending.MonitorPendingTransfersUntilResolvedUseCase
import mega.privacy.android.domain.usecase.transfers.previews.BroadcastTransferTagToCancelUseCase
import mega.privacy.android.domain.usecase.transfers.previews.MonitorTransferTagToCancelUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.GetCurrentUploadSpeedUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.StartUploadsWorkerAndWaitUntilIsStartedUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * View model to handle start transfers component
 */
@HiltViewModel
internal class StartTransfersComponentViewModel @Inject constructor(
    private val getOfflinePathForNodeUseCase: GetOfflinePathForNodeUseCase,
    private val getOrCreateDownloadLocationUseCase: GetOrCreateDownloadLocationUseCase,
    private val getFilePreviewDownloadPathUseCase: GetFilePreviewDownloadPathUseCase,
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
    private val setAskForDownloadLocationUseCase: SetAskForDownloadLocationUseCase,
    private val setDownloadLocationUseCase: SetDownloadLocationUseCase,
    private val sendChatAttachmentsUseCase: SendChatAttachmentsUseCase,
    private val shouldAskForResumeTransfersUseCase: ShouldAskForResumeTransfersUseCase,
    private val setAskedResumeTransfersUseCase: SetAskedResumeTransfersUseCase,
    private val pauseTransfersQueueUseCase: PauseTransfersQueueUseCase,
    private val saveOfflineNodesToDevice: SaveOfflineNodesToDevice,
    private val saveUriToDeviceUseCase: SaveUriToDeviceUseCase,
    private val getCurrentUploadSpeedUseCase: GetCurrentUploadSpeedUseCase,
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    private val monitorRequestFilesPermissionDeniedUseCase: MonitorRequestFilesPermissionDeniedUseCase,
    private val setRequestFilesPermissionDeniedUseCase: SetRequestFilesPermissionDeniedUseCase,
    private val startDownloadsWorkerAndWaitUntilIsStartedUseCase: StartDownloadsWorkerAndWaitUntilIsStartedUseCase,
    private val startUploadsWorkerAndWaitUntilIsStartedUseCase: StartUploadsWorkerAndWaitUntilIsStartedUseCase,
    private val deleteAllPendingTransfersUseCase: DeleteAllPendingTransfersUseCase,
    private val monitorPendingTransfersUntilResolvedUseCase: MonitorPendingTransfersUntilResolvedUseCase,
    private val insertPendingDownloadsForNodesUseCase: InsertPendingDownloadsForNodesUseCase,
    private val insertPendingUploadsForFilesUseCase: InsertPendingUploadsForFilesUseCase,
    private val monitorStorageOverQuotaUseCase: MonitorStorageOverQuotaUseCase,
    private val invalidateCancelTokenUseCase: InvalidateCancelTokenUseCase,
    private val getCurrentTimeInMillisUseCase: GetCurrentTimeInMillisUseCase,
    private val areTransfersPausedUseCase: AreTransfersPausedUseCase,
    private val getFileNameFromStringUriUseCase: GetFileNameFromStringUriUseCase,
    private val cancelTransferByTagUseCase: CancelTransferByTagUseCase,
    private val deleteCacheFilesUseCase: DeleteCacheFilesUseCase,
    private val getTransferByTagUseCase: GetTransferByTagUseCase,
    private val monitorTransferTagToCancelUseCase: MonitorTransferTagToCancelUseCase,
    private val broadcastTransferTagToCancelUseCase: BroadcastTransferTagToCancelUseCase,
    private val deleteCompletedTransfersByIdUseCase: DeleteCompletedTransfersByIdUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val ratingHandler: RatingHandlerImpl,
) : ViewModel(), DefaultLifecycleObserver {

    private val _uiState = MutableStateFlow(StartTransferViewState())

    /**
     * the state of the view
     */
    internal val uiState = _uiState.asStateFlow()

    init {
        checkDownloadRating()
        checkUploadRating()
        monitorRequestFilesPermissionDenied()
        monitorStorageOverQuota()
        monitorPreviews()
        monitorTransferToCancel()
    }

    /**
     * It starts the triggered transfer, asking for confirmation in case of large transfers if corresponds
     * @param transferTriggerEvent the event that triggered this transfer
     */
    fun startTransfer(
        transferTriggerEvent: TransferTriggerEvent,
    ) {
        viewModelScope.launch {
            if (monitorStorageStateEventUseCase().value.storageState == StorageState.PayWall) {
                _uiState.updateEventAndClearProgress(StartTransferEvent.PayWall)
                return@launch
            }
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
                }

                is TransferTriggerEvent.StartUpload -> {
                    if (checkAndHandleDeviceIsNotConnected()) {
                        return@launch
                    }
                    startUploads(transferTriggerEvent = transferTriggerEvent)
                }

                is TransferTriggerEvent.RetryTransfers -> {
                    if (checkAndHandleDeviceIsNotConnected()) {
                        return@launch
                    }
                    retryTransfers(transferTriggerEvent = transferTriggerEvent)
                }
            }
            checkAndHandleTransfersPaused(transferTriggerEvent)
        }
    }

    private suspend fun retryTransfers(transferTriggerEvent: TransferTriggerEvent.RetryTransfers) {
        var retryUploads = false
        var retryDownloads = false
        var notEnoughStorage = false
        var shouldPromptToSaveDestination = false
        var defaultLocation: String? = null

        runCatching { clearActiveTransfersIfFinishedUseCase() }
            .onFailure { Timber.e(it) }

        if (transferTriggerEvent.idsAndEvents.values.any { it is TransferTriggerEvent.RetryDownloadNode }) {
            shouldPromptToSaveDestination = runCatching { shouldPromptToSaveDestinationUseCase() }
                .getOrDefault(false)

            if (shouldPromptToSaveDestination.not()) {
                defaultLocation = runCatching { getOrCreateDownloadLocationUseCase() }
                    .onFailure { Timber.e(it) }
                    .getOrNull()
            }
        }

        buildList {
            for ((id, event) in transferTriggerEvent.idsAndEvents) {
                when (event) {
                    is TransferTriggerEvent.DownloadTriggerEvent -> {
                        (if (event is TransferTriggerEvent.StartDownloadForOffline) {
                            if (event.node == null) {
                                Timber.e("Node in $event must exist")
                                null
                            } else {
                                runCatching {
                                    val node = event.node
                                    requireNotNull(node)
                                    getOfflinePathForNodeUseCase(node)
                                }.onFailure { Timber.e(it) }
                                    .getOrNull()
                            }
                        } else if (event is TransferTriggerEvent.RetryDownloadNode) {
                            when {
                                event.node == null -> {
                                    Timber.e("Node in $event must exist")
                                    null
                                }

                                shouldPromptToSaveDestination -> event.downloadLocation
                                else -> defaultLocation
                            }
                        } else {
                            null
                        })?.let { location ->
                            runCatching {
                                insertPendingDownloadsForNodesUseCase(
                                    event.nodes,
                                    UriPath(location),
                                    event.isHighPriority,
                                    event.appData
                                )
                            }.onSuccess {
                                retryDownloads = true
                                add(id)
                            }.onFailure {
                                if (it is NotEnoughStorageException) {
                                    notEnoughStorage = true
                                } else {
                                    Timber.e(it)
                                }
                            }
                        }
                    }

                    is TransferTriggerEvent.StartUpload -> {
                        if (event.pathsAndNames.isEmpty()) {
                            Timber.e("Paths in $event must exist")
                        } else {
                            retryUploads = true
                            add(id)
                            viewModelScope.launch {
                                insertPendingUploadsForFilesUseCase(
                                    pathsAndNames = event.pathsAndNames,
                                    parentFolderId = event.destinationId,
                                    isHighPriority = event.isHighPriority
                                )
                            }
                        }
                    }
                }
            }
        }.let { retriedTransferIds ->
            if (notEnoughStorage) {
                _uiState.update { state ->
                    state.copy(
                        oneOffViewEvent = triggered(StartTransferEvent.Message.NotSufficientSpace)
                    )
                }
            }

            if (retriedTransferIds.isNotEmpty()) {
                viewModelScope.launch {
                    when {
                        retryUploads && retryDownloads -> combine(
                            monitorPendingTransfersUntilResolvedUseCase(TransferType.GENERAL_UPLOAD),
                            monitorPendingTransfersUntilResolvedUseCase(TransferType.DOWNLOAD)
                        ) { uploads, downloads -> uploads + downloads }

                        retryUploads -> monitorPendingTransfersUntilResolvedUseCase(TransferType.GENERAL_UPLOAD)

                        retryDownloads -> monitorPendingTransfersUntilResolvedUseCase(TransferType.DOWNLOAD)

                        else -> emptyFlow()
                    }.onEach { Timber.d("Pending transfers to process: ${it.size}") }
                        .catch { Timber.e(it) }
                        .collect() //just wait until finishes

                    Timber.d("Scanning finished")

                    invalidateCancelTokenUseCase()
                    deleteAllPendingTransfersUseCase()
                    deleteCompletedTransfersByIdUseCase(retriedTransferIds)
                }

                if (retryUploads) {
                    startUploadsWorkerAndWaitUntilIsStartedUseCase()
                    checkUploadRating()
                }

                if (retryDownloads) {
                    startDownloadsWorkerAndWaitUntilIsStartedUseCase()
                    checkDownloadRating()
                }
            }
        }
    }

    /**
     * It starts downloading the related nodes, without asking confirmation for large transfers (because it's already asked)
     * @param transferTriggerEvent the event that triggered this download
     */
    fun startDownloadWithoutConfirmation(
        transferTriggerEvent: TransferTriggerEvent.DownloadTriggerEvent,
    ) {
        val node = transferTriggerEvent.nodes.firstOrNull()
        val isCopyEvent = transferTriggerEvent is TransferTriggerEvent.CopyTriggerEvent
        if (node == null && !isCopyEvent) {
            Timber.e("Node in $transferTriggerEvent must exist")
            _uiState.updateEventAndClearProgress(StartTransferEvent.Message.TransferCancelled)
        } else {
            when (transferTriggerEvent) {
                is TransferTriggerEvent.StartDownloadForOffline -> {
                    startDownloadForOffline(transferTriggerEvent)
                }

                is TransferTriggerEvent.StartDownloadNode, is TransferTriggerEvent.CopyOfflineNode, is TransferTriggerEvent.CopyUri -> {
                    viewModelScope.launch {
                        if (runCatching { shouldAskDownloadDestinationUseCase() }.getOrDefault(false)) {
                            _uiState.update {
                                it.copy(
                                    askDestinationForDownload = transferTriggerEvent,
                                    jobInProgressState = null,
                                )
                            }
                        } else {
                            runCatching { getOrCreateDownloadLocationUseCase() }
                                .onFailure { Timber.e(it) }
                                .getOrNull()?.let { location ->
                                    startDownloadNodes(transferTriggerEvent, location)
                                }
                        }
                    }
                }

                is TransferTriggerEvent.StartDownloadForPreview -> {
                    _uiState.update { it.copy(isOpenWithAction = transferTriggerEvent.isOpenWith) }
                    startDownloadNodeForPreview(transferTriggerEvent)
                }

                is TransferTriggerEvent.RetryDownloadNode -> {
                    /* No actions required. Execution should not reach here as this event
                    is only used for [TransferTriggerEvent.RetryTransfers] */
                }
            }
        }
    }

    /**
     * Start download with the destination manually set by the user
     * @param destinationUri the chosen destination or null if cancelled
     */
    fun startDownloadWithDestination(
        destinationUri: Uri?,
    ) {
        Timber.d("Selected destination $destinationUri")
        val originalEvent = uiState.value.askDestinationForDownload
        consumeAskDestination()
        if (destinationUri != null && originalEvent != null) {
            viewModelScope.launch {
                val destination = destinationUri.toString()
                startDownloadNodes(originalEvent, destination)
                if (runCatching { shouldPromptToSaveDestinationUseCase() }.getOrDefault(false)) {
                    val destinationName =
                        runCatching { getFileNameFromStringUriUseCase(destination) }
                            .onFailure { Timber.e(it) }
                            .getOrNull()

                    _uiState.update {
                        it.copy(
                            promptSaveDestination = triggered(
                                SaveDestinationInfo(
                                    destination = destination,
                                    destinationName = destinationName ?: destination
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun consumeAskDestination() {
        _uiState.update {
            it.copy(
                askDestinationForDownload = null,
                jobInProgressState = null,
            )
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
                viewModelScope.launch {
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
                source = startDownloadNode.uriPath,
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
        }.onSuccess {
            _uiState.updateEventAndClearProgress(StartTransferEvent.FinishCopyOffline(it))
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
        viewModelScope.launch {
            startDownloadNodes(
                nodes = listOfNotNull(event.node),
                isHighPriority = true,
                getUri = {
                    runCatching {
                        getFilePreviewDownloadPathUseCase().also {
                            // delete the existing file if already exists, because if the preview exists and we need to download it means it's outdated
                            runCatching {
                                val node = event.node
                                requireNotNull(node)
                                deleteCacheFilesUseCase(listOf(UriPath(it + node.name)))
                            }.onFailure {
                                Timber.e(it, "Error deleting existing preview file")
                            }
                        }
                    }
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
        viewModelScope.launch {
            startDownloadNodes(
                nodes = listOfNotNull(event.node),
                isHighPriority = event.isHighPriority,
                getUri = {
                    runCatching {
                        val node = event.node
                        requireNotNull(node)
                        getOfflinePathForNodeUseCase(node)
                    }.onFailure { Timber.e(it) }
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
        transferTriggerEvent: TransferTriggerEvent.DownloadTriggerEvent,
    ) {
        runCatching { clearActiveTransfersIfFinishedUseCase() }
            .onFailure { Timber.e(it) }
        _uiState.updateJobInProgress(StartTransferJobInProgress.ScanningTransfers(TransferStage.STAGE_NONE))
        runCatching {
            val uri = getUri()
            if (uri.isNullOrBlank()) {
                throw NullPointerException("path not found!")
            }
            insertPendingDownloadsForNodesUseCase(
                nodes,
                UriPath(uri),
                isHighPriority,
                transferTriggerEvent.appData
            )
            monitorPendingTransfersUntilProcessed(transferTriggerEvent)
            startDownloadsWorkerAndWaitUntilIsStartedUseCase()
        }.onFailure {
            Timber.e(it, "Error on startDownloadNodes")
            _uiState.updateEventAndClearProgressWithException(it)
        }
        checkDownloadRating()
    }

    private fun monitorPendingTransfersUntilProcessed(
        transferTriggerEvent: TransferTriggerEvent.CloudTransfer,
    ) {
        viewModelScope.launch {
            var error: Throwable? = null
            val transferType: TransferType = when (transferTriggerEvent) {
                is TransferTriggerEvent.DownloadTriggerEvent -> TransferType.DOWNLOAD
                is TransferTriggerEvent.StartUpload -> TransferType.GENERAL_UPLOAD
            }
            monitorPendingTransfersUntilResolvedUseCase(transferType).onEach { pendingTransfers ->
                Timber.d("Pending transfers to process: ${pendingTransfers.size}")
                if (pendingTransfers.isNotEmpty()) {
                    _uiState.updateJobInProgress(
                        StartTransferJobInProgress.ScanningTransfers(
                            stage = pendingTransfers.minOf { it.scanningFoldersData.stage },
                            fileCount = pendingTransfers.sumOf { it.scanningFoldersData.fileCount },
                            folderCount = pendingTransfers.sumOf { it.scanningFoldersData.folderCount },
                            createdFolderCount = pendingTransfers.sumOf { it.scanningFoldersData.createdFolderCount },
                        )
                    )
                }
            }
                .catch { error = it }
                .collect() //just wait until finishes

            Timber.d("Scanning finished")
            invalidateCancelTokenUseCase()
            deleteAllPendingTransfersUseCase()
            _uiState.updateEventAndClearProgress(
                when (transferTriggerEvent) {
                    is TransferTriggerEvent.DownloadTriggerEvent -> {
                        StartTransferEvent.FinishDownloadProcessing(
                            exception = error,
                            triggerEvent = transferTriggerEvent,
                        )
                    }

                    is TransferTriggerEvent.StartUpload -> {
                        StartTransferEvent.FinishUploadProcessing(
                            totalFiles = transferTriggerEvent.pathsAndNames.size,
                            triggerEvent = transferTriggerEvent,
                        )
                    }
                }
            )
        }
    }

    private suspend fun startChatUploads(
        chatId: Long,
        uris: List<UriPath>,
        isVoiceClip: Boolean = false,
    ) {
        runCatching { clearActiveTransfersIfFinishedUseCase() }
            .onFailure { Timber.e(it) }
        runCatching {
            sendChatAttachmentsUseCase(
                uris.map { it }.associateWith { null }, isVoiceClip, chatId
            )
        }.onSuccess {
            _uiState.updateEventAndClearProgress(null)
        }.onFailure {
            Timber.e(it)
            _uiState.updateEventAndClearProgressWithException(it)
        }
    }

    /**
     * Some events need to be consumed to don't be missed or fired more than once
     */
    fun consumeOneOffEvent() {
        _uiState.updateEventAndClearProgress(null)
    }

    /**
     * Cancel the current in-progress transfers job.
     *
     * This won't stop the transfer flow immediately but will send a cancel token to the SDK, which will cancel the transfer flow with the appropriate events.
     */
    fun cancelCurrentTransfersJob() =
        viewModelScope.launch {
            runCatching {
                cancelCancelTokenUseCase()
            }.onSuccess {
                _uiState.update {
                    if (it.jobInProgressState is StartTransferJobInProgress.ScanningTransfers) {
                        it.copy(
                            jobInProgressState = StartTransferJobInProgress.CancellingTransfers,
                        )
                    } else it
                }
            }.onFailure {
                Timber.e(it)
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
                saveDoNotPromptToSaveDestinationUseCase()
                setDownloadLocationUseCase(destination)
                setAskForDownloadLocationUseCase(false)
            }.onFailure {
                Timber.e("Error saving the destination:\n$it")
            }
        }
    }

    /**
     * Save setting to don't prompt the user again to save selected destination
     */
    fun alwaysAskForDestination() {
        viewModelScope.launch {
            runCatching {
                saveDoNotPromptToSaveDestinationUseCase()
                setAskForDownloadLocationUseCase(true)
            }.onFailure {
                Timber.e("Error saving the don't save destination again prompt:\n$it")
            }

        }
    }

    /**
     * Large download confirmation has been answered
     * @param downloadTriggerEvent the event that has been confirmed, or null if is not confirmed
     * @param saveDoNotAskAgain if true, future large downloads won't need confirmation
     */
    fun largeDownloadAnswered(
        downloadTriggerEvent: TransferTriggerEvent.DownloadTriggerEvent?,
        saveDoNotAskAgain: Boolean,
    ) {
        if (downloadTriggerEvent != null) {
            if (saveDoNotAskAgain) {
                viewModelScope.launch {
                    runCatching { setAskBeforeLargeDownloadsSettingUseCase(askForConfirmation = false) }
                        .onFailure { Timber.e(it) }
                }
            }
            startDownloadWithoutConfirmation(
                downloadTriggerEvent,
            )
        }
        _uiState.update {
            it.copy(confirmLargeDownload = null)
        }
    }

    fun previewFile(file: File) {
        _uiState.update { state ->
            state.copy(
                previewFileToOpen = file,
                jobInProgressState = null,
            )
        }
    }

    /**
     * Consume preview file opened
     */
    fun consumePreviewFileOpened() {
        _uiState.update {
            it.copy(previewFileToOpen = null, isOpenWithAction = false)
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
                _uiState.update {
                    it.copy(
                        confirmLargeDownload = ConfirmLargeDownloadInfo(
                            fileSizeStringMapper(size), transferTriggerEvent
                        ),
                        jobInProgressState = null,
                    )
                }
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
                monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD).conflate()
                    .takeWhile {
                        checkShowDownloadRating
                    }.catch {
                        Timber.e(it)
                    }.collect { (transferTotals, paused) ->
                        if (checkShowDownloadRating && !paused && transferTotals.totalFileTransfers > 0) {
                            val currentDownloadSpeed = getCurrentDownloadSpeedUseCase()
                            ratingHandler.showRatingBaseOnSpeedAndSize(
                                size = transferTotals.totalBytes,
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

    private fun monitorPreviews() {
        viewModelScope.launch {
            monitorOngoingActiveTransfersUseCase(TransferType.DOWNLOAD).conflate()
                .catch {
                    Timber.e(it)
                }.collect { (transferTotals, _) ->
                    transferTotals.actionGroups
                        .filter { it.isPreviewDownload() }
                        .takeIf { it.isNotEmpty() }?.let { previewGroups ->
                            if (active) {
                                checkFinishedPreviews(previewGroups)
                                checkSlowPreviews(previewGroups)
                            }
                        }
                }
        }
    }

    private fun checkFinishedPreviews(actionGroups: List<ActiveTransferTotals.ActionGroup>) {
        actionGroups.filter { it.finished() && !alreadyFinishedPreviewGroups.contains(it.groupId) }
            .let { finishedGroups ->
                alreadyFinishedPreviewGroups.addAll(finishedGroups.map { it.groupId })
                finishedGroups.forEach { group ->
                    if (group.completedFiles == 1) {
                        val file = File(group.destination + group.singleFileName)
                        val duration = group.durationFromStart(getCurrentTimeInMillisUseCase())

                        if (group.singleTransferTag == uiState.value.transferTagToCancel) {
                            broadcastTransferTagToCancelAsNull()
                        }

                        if (duration < 1.5.seconds) {
                            previewFile(file)
                        }
                    }
                }
            }
    }

    private fun checkSlowPreviews(actionGroups: List<ActiveTransferTotals.ActionGroup>) {
        actionGroups.filter {
            !it.finished()
                    && !alreadySlowNotifiedGroups.contains(it.groupId)
                    && it.durationFromStart(getCurrentTimeInMillisUseCase()) > 1.5.seconds
        }
            .takeIf { it.isNotEmpty() }
            ?.let { notFinishedSlowGroups ->
                //as this is responding an user action, usually only one group at a time, but
                alreadySlowNotifiedGroups.addAll(notFinishedSlowGroups.map { it.groupId })
                notFinishedSlowGroups.first().let { group ->
                    group.singleTransferTag?.let { tag ->
                        viewModelScope.launch {
                            getTransferByTagUseCase(tag)?.let { transfer ->
                                _uiState.updateEventAndClearProgress(
                                    SlowDownloadPreviewInProgress(
                                        transferUniqueId = transfer.uniqueId,
                                        transferPath = transfer.localPath
                                    )
                                )
                            } ?: run {
                                File(group.destination + group.singleFileName).let {
                                    if (it.exists()) {
                                        previewFile(it)
                                    }
                                }
                            }
                        }
                    }
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

    private fun MutableStateFlow<StartTransferViewState>.updateEventAndClearProgressWithException(
        exception: Throwable,
    ) {
        updateEventAndClearProgress(
            if (exception is NotEnoughStorageException) {
                StartTransferEvent.Message.NotSufficientSpace
            } else {
                StartTransferEvent.Message.TransferCancelled
            }
        )
    }

    private fun MutableStateFlow<StartTransferViewState>.updateJobInProgress(
        jobInProgress: StartTransferJobInProgress?,
    ) {
        this.update { it.copy(jobInProgressState = jobInProgress) }
    }

    private fun String.ensureSuffix(suffix: String) =
        if (this.endsWith(suffix)) this else this.plus(suffix)


    private var active = false

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
            runCatching { pauseTransfersQueueUseCase(false) }
                .onFailure { Timber.e(it) }
        }
    }

    private fun checkAndHandleTransfersPaused(triggerEvent: TransferTriggerEvent) {
        if (runCatching {
                when (triggerEvent.checkPausedTransfers) {
                    TransferTriggerEvent.CheckPausedTransfersType.Never -> false
                    TransferTriggerEvent.CheckPausedTransfersType.OncePerPausedState -> shouldAskForResumeTransfersUseCase()
                    TransferTriggerEvent.CheckPausedTransfersType.Always -> areTransfersPausedUseCase()
                }
            }.getOrDefault(false)
        ) {
            _uiState.updateEventAndClearProgress(StartTransferEvent.PausedTransfers(triggerEvent))
        }
    }

    private suspend fun startUploads(
        transferTriggerEvent: TransferTriggerEvent.StartUpload,
    ) = with(transferTriggerEvent) {
        runCatching { clearActiveTransfersIfFinishedUseCase() }
            .onFailure { Timber.e(it) }

        if (pathsAndNames.isEmpty()) {
            Timber.e("Paths in $pathsAndNames must exist")
            _uiState.updateEventAndClearProgress(StartTransferEvent.Message.TransferCancelled)
            return
        }

        runCatching {
            insertPendingUploadsForFilesUseCase(
                pathsAndNames = pathsAndNames,
                parentFolderId = destinationId,
                isHighPriority = transferTriggerEvent.isHighPriority
            )
            monitorPendingTransfersUntilProcessed(transferTriggerEvent)
            startUploadsWorkerAndWaitUntilIsStartedUseCase()
        }.onFailure {
            Timber.e(it, "Error on startUploadFilesInWorker")
            if (transferTriggerEvent is TransferTriggerEvent.StartUpload.TextFile) {
                _uiState.updateEventAndClearProgress(
                    StartTransferEvent.Message.FailedTextFileUpload(
                        isEditMode = transferTriggerEvent.isEditMode,
                        isCloudFile = transferTriggerEvent.fromHomePage
                    )
                )
            } else {
                _uiState.updateEventAndClearProgressWithException(it)
            }
        }
        checkUploadRating()
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
                            ratingHandler.showRatingBaseOnSpeedAndSize(
                                size = transferTotals.totalBytes,
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

    private fun monitorRequestFilesPermissionDenied() {
        viewModelScope.launch {
            monitorRequestFilesPermissionDeniedUseCase().collect { denied ->
                _uiState.update { state -> state.copy(requestFilesPermissionDenied = denied) }
            }
        }
    }

    fun setRequestFilesPermissionDenied() {
        viewModelScope.launch {
            runCatching { setRequestFilesPermissionDeniedUseCase() }
                .onFailure { Timber.e(it) }
        }
    }

    fun transferEventWaitingForPermissionRequest(transferTriggerEvent: TransferTriggerEvent) {
        viewModelScope.launch {
            _uiState.update { state -> state.copy(triggerEventWithoutPermission = transferTriggerEvent) }
        }
    }

    fun startTransferAfterPermissionRequest() {
        _uiState.value.triggerEventWithoutPermission?.let {
            startTransfer(it)
        }
        consumeRequestPermission()
    }

    fun consumeRequestPermission() {
        viewModelScope.launch {
            _uiState.update { state -> state.copy(triggerEventWithoutPermission = null) }
        }
    }

    private fun monitorStorageOverQuota() {
        viewModelScope.launch {
            monitorStorageOverQuotaUseCase().collect { isStorageOverQuota ->
                _uiState.update { state -> state.copy(isStorageOverQuota = isStorageOverQuota) }
            }
        }
    }

    /**
     * Set the transfer tag to cancel.
     *
     * @param transferTagToCancel The transfer tag to cancel, null if no transfer to cancel.
     */
    private fun setTransferTagToCancel(transferTagToCancel: Int?) {
        _uiState.update { state -> state.copy(transferTagToCancel = transferTagToCancel) }
    }

    /**
     * Confirms the cancellation of a transfer and proceeds with the action.
     */
    fun cancelTransferConfirmed() {
        uiState.value.transferTagToCancel?.let {
            viewModelScope.launch {
                runCatching { cancelTransferByTagUseCase(it) }
                    .onFailure {
                        Timber.e(it)
                        _uiState.update { state -> state.copy(cancelTransferFailure = triggered) }
                    }
            }
            broadcastTransferTagToCancelAsNull()
        }
    }

    fun onConsumeCancelTransferFailure() {
        _uiState.update { state -> state.copy(cancelTransferFailure = consumed) }
    }

    /**
     * Cancels the cancellation of a transfer.
     */
    fun cancelTransferCancelled() {
        broadcastTransferTagToCancelAsNull()
    }

    private fun monitorTransferToCancel() {
        viewModelScope.launch {
            monitorTransferTagToCancelUseCase()
                .retry {
                    Timber.e(it)
                    true
                }
                .collectLatest { transferTag ->
                    setTransferTagToCancel(transferTag)
                }
        }
    }

    private fun broadcastTransferTagToCancelAsNull() {
        viewModelScope.launch {
            broadcastTransferTagToCancelUseCase(null)
        }
    }

    companion object {
        // preview snackbar notifications can be shown in a different screen than the one that started it, that's why we need to store it in the companion object
        private val alreadyFinishedPreviewGroups = mutableListOf<Int>()
        private val alreadySlowNotifiedGroups = mutableListOf<Int>()
    }
}
