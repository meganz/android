package mega.privacy.android.app.main.managerSections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.isBackgroundTransfer
import mega.privacy.android.domain.entity.transfer.isPreviewDownload
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.chat.message.pendingmessages.RetryChatUploadUseCase
import mega.privacy.android.domain.usecase.file.CanReadUriUseCase
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.GetFailedOrCanceledTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.GetInProgressTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferByUniqueIdUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferBeforeByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferToFirstByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferToLastByTagUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteFailedOrCancelledTransferCacheFilesUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetDownloadParentDocumentFileUseCase
import mega.privacy.android.domain.usecase.transfers.completed.MonitorCompletedTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.completed.MonitorCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransferByTagUseCase
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject

/**
 * The logic regarding transfers UI
 */
@HiltViewModel
class TransfersViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val moveTransferBeforeByTagUseCase: MoveTransferBeforeByTagUseCase,
    private val moveTransferToFirstByTagUseCase: MoveTransferToFirstByTagUseCase,
    private val moveTransferToLastByTagUseCase: MoveTransferToLastByTagUseCase,
    private val getTransferByUniqueIdUseCase: GetTransferByUniqueIdUseCase,
    private val getInProgressTransfersUseCase: GetInProgressTransfersUseCase,
    private val monitorCompletedTransfersUseCase: MonitorCompletedTransfersUseCase,
    monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    monitorCompletedTransferEventUseCase: MonitorCompletedTransferEventUseCase,
    private val getFailedOrCanceledTransfersUseCase: GetFailedOrCanceledTransfersUseCase,
    private val deleteCompletedTransferUseCase: DeleteCompletedTransferUseCase,
    private val pauseTransferByTagUseCase: PauseTransferByTagUseCase,
    private val cancelTransferByTagUseCase: CancelTransferByTagUseCase,
    monitorPausedTransfersUseCase: MonitorPausedTransfersUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val retryChatUploadUseCase: RetryChatUploadUseCase,
    private val monitorTransferOverQuotaUseCase: MonitorTransferOverQuotaUseCase,
    private val deleteFailedOrCancelledTransferCacheFilesUseCase: DeleteFailedOrCancelledTransferCacheFilesUseCase,
    private val canReadUriUseCase: CanReadUriUseCase,
    private val getDownloadParentDocumentFileUseCase: GetDownloadParentDocumentFileUseCase,
    private val getPathByDocumentContentUriUseCase: GetPathByDocumentContentUriUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TransfersUiState())

    /**
     * Ui state
     */
    val uiState = _uiState.asStateFlow()

    private val _activeState = MutableStateFlow<ActiveTransfersState>(ActiveTransfersState.Default)

    /**
     * The state regarding active transfers UI
     */
    val activeState = _activeState.asStateFlow()

    /**
     * Failed transfer
     */
    val failedTransfer = monitorCompletedTransferEventUseCase().distinctUntilChanged()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    private val _activeTransfers = MutableStateFlow(emptyList<Transfer>())

    /**
     * Active transfer
     */
    val activeTransfer = _activeTransfers.asStateFlow()

    /**
     * Flow that emits true if transfers are paused globally or false otherwise
     */
    val areTransfersPaused by lazy { monitorPausedTransfersUseCase() }

    private val _completedTransfers = MutableStateFlow(emptyList<CompletedTransfer>())

    /**
     * Completed transfers
     */
    val completedTransfers = _completedTransfers.asStateFlow()

    private val _completedTransfersDestinations =
        MutableStateFlow(emptyMap<CompletedTransfer, String>())

    /**
     * Completed transfers
     */
    val completedTransfersDestinations = _completedTransfersDestinations.asStateFlow()

    private var transferCallback = 0L
    private var currentTab = TransfersTab.NONE
    private var previousTab = TransfersTab.NONE

    init {
        getAllActiveTransfers()
        viewModelScope.launch {
            monitorTransferEventsUseCase()
                .catch { Timber.e(it) }
                .collect {
                    if (it.transfer.isStreamingTransfer
                        || it.transfer.isBackgroundTransfer()
                        || it.transfer.isFolderTransfer
                        || it.transfer.isPreviewDownload()
                        || transferCallback > it.transfer.notificationNumber
                    ) return@collect
                    transferCallback = it.transfer.notificationNumber
                    when (it) {
                        is TransferEvent.TransferFinishEvent -> transferFinished(it.transfer)
                        is TransferEvent.TransferStartEvent -> startTransfer(it.transfer)
                        is TransferEvent.TransferUpdateEvent -> updateTransfer(it.transfer)
                        is TransferEvent.TransferTemporaryErrorEvent,
                        is TransferEvent.TransferDataEvent,
                        is TransferEvent.TransferPaused,
                        is TransferEvent.FolderTransferUpdateEvent,
                            -> Unit
                    }
                }
        }

        viewModelScope.launch {
            monitorCompletedTransfersUseCase(MAX_TRANSFERS)
                .catch {
                    Timber.e(it)
                }.collect { completedTransfers ->
                    completedTransfers.associateWith {
                        getPath(it)
                    }.let { completedTransfersDestinations ->
                        _completedTransfersDestinations.update { completedTransfersDestinations }
                    }
                    _completedTransfers.update { completedTransfers }
                }
        }

        monitorTransferOverQuota()
    }

    private suspend fun getPath(completedTransfer: CompletedTransfer): String =
        with(completedTransfer) {
            if (isContentUriDownload) {
                runCatching { getDownloadParentDocumentFileUseCase(path) }
                    .onFailure { Timber.w(it) }
                    .getOrNull()?.uri?.let { uriPath ->
                        runCatching { getPathByDocumentContentUriUseCase(uriPath.value) }
                            .onFailure { Timber.w(it) }
                            .getOrNull()
                    } ?: path
            } else {
                path
            }
        }

    private fun monitorTransferOverQuota() {
        viewModelScope.launch {
            monitorTransferOverQuotaUseCase().collectLatest { isInTransferOverQuota ->
                _uiState.update { state -> state.copy(isInTransferOverQuota = isInTransferOverQuota) }
            }
        }
    }

    /**
     * Set the visibility for get more quota view
     */
    fun setGetMoreQuotaViewVisibility() {
        viewModelScope.launch {
            _activeState.update {
                ActiveTransfersState.GetMoreQuotaViewVisibility(
                    uiState.value.isInTransferOverQuota
                )
            }
        }
    }

    /**
     * Checks if it is on transfer over quota.
     *
     * @return True if it is on transfer over quota, false otherwise.
     */
    fun isOnTransferOverQuota() = uiState.value.isInTransferOverQuota

    /**
     * Set active transfers
     */
    fun getAllActiveTransfers() =
        viewModelScope.launch {
            runCatching {
                val transfers = getInProgressTransfersUseCase()
                    .filterNot { it.isPreviewDownload() }
                _activeTransfers.update { transfers }
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess {
                Timber.d("Active transfers correctly set.")
            }
        }

    /**
     * Get active transfer list
     *
     * @return active transfer list
     */
    fun getActiveTransfers() = _activeTransfers.value.toList()

    /**
     * Get active transfer
     *
     * @param pos the item position
     * @return active transfer item
     */
    fun getActiveTransfer(pos: Int) = activeTransfer.value.getOrNull(pos)

    /**
     * Update the active transfer
     *
     * @param transfer updated item
     */
    private fun updateTransfer(transfer: Transfer) {
        viewModelScope.launch(ioDispatcher) {
            val current = activeTransfer.value.toMutableList()
            current.indexOfFirst { it.uniqueId == transfer.uniqueId }
                .takeIf { pos -> pos in current.indices }
                ?.let { pos ->
                    current[pos] = transfer
                    _activeTransfers.update { current }
                }
        }
    }

    /**
     * Swap active transfer
     *
     * @param currentPos the current position
     * @param targetPos the target position
     * @return new [MegaTransfer] list
     */
    fun activeTransfersSwap(currentPos: Int, targetPos: Int) {
        val current = activeTransfer.value.toMutableList()
        if (currentPos in current.indices && targetPos in current.indices) {
            Collections.swap(current, currentPos, targetPos)
        } else {
            // This can happen if one or several transfers finish while the user is dragging
            Timber.d("Error: index out of range $currentPos $targetPos")
        }
        _activeTransfers.update { current }
    }

    /**
     * Set the current active transfers tab
     */
    fun setCurrentSelectedTab(tab: TransfersTab) = viewModelScope.launch {
        if (tab == TransfersTab.NONE && currentTab != TransfersTab.NONE) {
            previousTab = currentTab
        }
        currentTab = tab
    }

    /**
     * clears the current selected tab, because the full section is not visible for instance
     */
    fun clearSelectedTab() = setCurrentSelectedTab(TransfersTab.NONE)

    /**
     * resets the selected tab when the section is visible again
     */
    fun resetSelectedTab() = setCurrentSelectedTab(previousTab)

    /**
     * Updates the UI in consequence after a transfer movement.
     * The update depends on if the movement finished with or without success.
     * If it finished with success, simply update the transfer in the transfers list and in adapter.
     * If not, reverts the movement, leaving the transfer in the same position it has before made the change.
     *
     * @param success  True if the movement finished with success, false otherwise.
     * @param uniqueId Identifier of the transfer.
     */
    private fun activeTransferFinishMovement(success: Boolean, uniqueId: Long) =
        viewModelScope.launch(ioDispatcher) {
            getTransferByUniqueIdUseCase(uniqueId).let { transfer ->
                if (transfer != null && transfer.state >= TransferState.STATE_COMPLETING) {
                    val current = activeTransfer.value.toMutableList()
                    val transferPosition = current.indexOfFirst { it.uniqueId == uniqueId }
                    if (transferPosition != INVALID_POSITION) {
                        current[transferPosition] = transfer
                        if (!success) {
                            current.sortBy { it.priority }
                        }
                        _activeTransfers.update { current }
                        _activeState.update {
                            ActiveTransfersState.TransferMovementFinishedUpdated(
                                success = success,
                                pos = transferPosition,
                                newTransfers = current
                            )
                        }
                    } else {
                        Timber.w("The transfer doesn't exist.")
                    }
                } else {
                    Timber.w("The transfer doesn't exist, finished or is finishing.")
                }
            }
        }

    /**
     * Adds a active transfer when starts.
     *
     * @param transfer transfer to add
     */
    private fun startTransfer(transfer: Transfer) {
        Timber.d("The transfer with uniqueId: ${transfer.uniqueId} has started, left: ${_activeTransfers.value.size}")
        viewModelScope.launch(ioDispatcher) {
            val current = activeTransfer.value.toMutableList()
            if (current.none { it.uniqueId == transfer.uniqueId }) {
                current.add(transfer)
                current.sortBy { it.priority }
                _activeTransfers.update { current }
            }
        }
    }

    /**
     * Removes a active transfer when finishes.
     *
     * @param transfer
     */
    private fun transferFinished(transfer: Transfer) {
        viewModelScope.launch(ioDispatcher) {
            val current = activeTransfer.value.toMutableList()
            current.indexOfFirst {
                it.uniqueId == transfer.uniqueId
            }.takeIf { pos -> pos in current.indices }
                ?.let { pos ->
                    current.removeAt(pos)
                    _activeTransfers.update { current }
                    _activeState.update {
                        ActiveTransfersState.TransferFinishedUpdated(pos, current)
                    }
                }
        }
    }

    /**
     * Active transfer status is changed
     */
    fun activeTransferChangeStatus(uniqueId: Long) = viewModelScope.launch {
        Timber.d("uniqueId: $uniqueId")
        getTransferByUniqueIdUseCase(uniqueId)?.let { transfer ->
            Timber.d("The transfer with uniqueId : $uniqueId has been paused/resumed, left: ${_activeTransfers.value.size}")
            updateTransfer(transfer)
        }
    }

    /**
     * Removes a completed transfer.
     *
     * @param transfer transfer to remove
     * @param isRemovedCache If ture, remove cache file, otherwise doesn't remove cache file
     */
    fun completedTransferRemoved(transfer: CompletedTransfer, isRemovedCache: Boolean) =
        viewModelScope.launch {
            deleteCompletedTransferUseCase(transfer, isRemovedCache)
        }

    /**
     * Removes all the cache files which were cancelled or failed while uploading.
     */
    fun deleteFailedOrCancelledTransferCacheFiles() = viewModelScope.launch(ioDispatcher) {
        runCatching { deleteFailedOrCancelledTransferCacheFilesUseCase() }
    }

    /**
     * Get the completed transfers
     *
     * @return [CompletedTransfer] list
     */
    fun getCompletedTransfers() = completedTransfers.value

    /**
     * Launches the request to change the priority of a transfer.
     *
     * @param transfer    MegaTransfer to change its priority.
     * @param newPosition The new position on the list.
     */
    fun moveTransfer(
        transfer: Transfer,
        newPosition: Int,
    ) = viewModelScope.launch {
        val current = activeTransfer.value.toList()
        val result = runCatching {
            when (newPosition) {
                0 -> moveTransferToFirstByTagUseCase(tag = transfer.tag)
                current.lastIndex -> moveTransferToLastByTagUseCase(tag = transfer.tag)
                else -> moveTransferBeforeByTagUseCase(
                    tag = transfer.tag,
                    prevTag = current[newPosition + 1].tag,
                )
            }
        }
        activeTransferFinishMovement(result.isSuccess, transfer.uniqueId)
    }

    /**
     * Has failed or cancelled transfer
     */
    fun hasFailedOrCancelledTransfer() =
        completedTransfers.value.any { it.state == TransferState.STATE_FAILED || it.state == TransferState.STATE_CANCELLED }

    /**
     * Pause or resume transfer
     *
     * @param transfer
     */
    fun pauseOrResumeTransfer(transfer: Transfer) {
        viewModelScope.launch {
            val result = runCatching {
                pauseTransferByTagUseCase(
                    transfer.tag,
                    transfer.state != TransferState.STATE_PAUSED
                )
            }.onFailure {
                Timber.e(it)
            }.map { transfer }
            _uiState.update { it.copy(pauseOrResumeTransferResult = result) }
        }
    }

    /**
     * Mark handled pause or resume transfer result
     */
    fun markHandledPauseOrResumeTransferResult() {
        _uiState.update { it.copy(pauseOrResumeTransferResult = null) }
    }

    /**
     * Cancel transfers by tag
     *
     * @param tags
     */
    fun cancelTransfersByTag(tags: List<Int>) {
        viewModelScope.launch {
            val result = runCatching {
                coroutineScope {
                    tags.map {
                        async {
                            cancelTransferByTagUseCase(it)
                        }
                    }.awaitAll()
                }
                Unit
            }
            _uiState.update { it.copy(cancelTransfersResult = result) }
        }
    }

    /**
     * Mark handled cancel transfer result
     */
    fun markHandledCancelTransfersResult() {
        _uiState.update { it.copy(cancelTransfersResult = null) }
    }

    /**
     * Retry all canceled or failed transfers
     */
    fun retryAllTransfers() = viewModelScope.launch(ioDispatcher) {
        runCatching { getFailedOrCanceledTransfersUseCase() }
            .onSuccess { transfers ->
                var cannotReadCount = 0

                transfers.forEach { transfer ->
                    if (canReadTransferUri(transfer)) {
                        retryCompletedTransfer(transfer)
                    } else {
                        cannotReadCount++
                    }
                }

                if (cannotReadCount > 0) {
                    _uiState.update { state -> state.copy(readRetryError = cannotReadCount) }
                }
            }
    }

    private suspend fun canReadTransferUri(transfer: CompletedTransfer) =
        if (transfer.type.isUploadType()) {
            val path = transfer.appData?.getOriginalContentUri() ?: transfer.originalPath

            canReadUriUseCase(path)
        } else {
            true
        }

    /**
     * trigger retry transfer event
     */
    suspend fun retryTransfer(transfer: CompletedTransfer) {
        if (canReadTransferUri(transfer)) {
            retryCompletedTransfer(transfer)
        } else {
            _uiState.update { state -> state.copy(readRetryError = 1) }
        }
    }

    /**
     * Consume retry read error
     */
    fun onConsumeRetryReadError() {
        _uiState.update { state -> state.copy(readRetryError = null) }
    }

    /**
     * trigger retry transfer event
     */
    private suspend fun retryCompletedTransfer(transfer: CompletedTransfer) {
        with(transfer) {
            when {
                type.isDownloadType() -> {
                    getNodeByIdUseCase(NodeId(handle))?.let { typedNode ->
                        if (isOffline == true) {
                            TransferTriggerEvent.StartDownloadForOffline(
                                node = typedNode,
                                withStartMessage = false
                            )
                        } else {
                            TransferTriggerEvent.StartDownloadNode(
                                nodes = listOf(typedNode),
                                withStartMessage = false
                            )
                        }
                    } ?: run {
                        Timber.e("Node not found for this transfer")
                    }
                }

                type.isUploadType() -> {
                    val isChatUpload =
                        appData?.any { it is TransferAppData.ChatUpload } == true
                    val path = appData?.getOriginalContentUri() ?: originalPath

                    if (isChatUpload) {
                        runCatching {
                            retryChatUploadUseCase(appData?.mapNotNull { it as? TransferAppData.ChatUpload }
                                ?: emptyList())
                        }.onFailure {
                            //No uploads were retried, try general upload only.
                            _uiState.update { state ->
                                state.copy(
                                    startEvent = triggered(
                                        getUploadTriggerEvent(path, parentHandle)
                                    )
                                )
                            }
                        }
                    } else {
                        getUploadTriggerEvent(path, parentHandle)
                    }
                }

                else -> throw IllegalArgumentException("This transfer type cannot be retried here for now")
            }.let { event ->
                if (event is TransferTriggerEvent) {
                    deleteCompletedTransferUseCase(transfer, false)
                    _uiState.update { state -> state.copy(startEvent = triggered(event)) }
                }
            }
        }
    }

    private fun List<TransferAppData>.getOriginalContentUri(): String? = this
        .filterIsInstance<TransferAppData.OriginalUriPath>()
        .firstOrNull()?.originalUriPath?.value

    private fun getUploadTriggerEvent(path: String, parentHandle: Long): TransferTriggerEvent {
        return TransferTriggerEvent.StartUpload.Files(
            mapOf(path to null),
            NodeId(parentHandle)
        )
    }

    /**
     * Consume retry event
     */
    fun consumeRetry() {
        _uiState.update { state -> state.copy(startEvent = consumed()) }
    }

    companion object {
        /**
         * Maximum number of transfers to be shown in the completed transfers list
         */
        const val MAX_TRANSFERS = 100
    }
}
