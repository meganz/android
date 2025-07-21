package mega.privacy.android.app.presentation.transfers.model

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.extensions.matchOrderWithNewAtEnd
import mega.privacy.android.app.extensions.moveElement
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.app.presentation.transfers.view.navigation.TransfersInfo
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.message.pendingmessages.RetryChatUploadUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferBeforeByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferToFirstByTagUseCase
import mega.privacy.android.domain.usecase.transfers.MoveTransferToLastByTagUseCase
import mega.privacy.android.domain.usecase.transfers.active.MonitorInProgressTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransfersByIdUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteFailedOrCancelledTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.completed.MonitorCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.paused.PauseTransfersQueueUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Transfers screen.
 *
 * @property uiState [TransfersUiState] for UI state.
 */
@HiltViewModel
class TransfersViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorInProgressTransfersUseCase: MonitorInProgressTransfersUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val monitorTransferOverQuotaUseCase: MonitorTransferOverQuotaUseCase,
    private val monitorPausedTransfersUseCase: MonitorPausedTransfersUseCase,
    private val pauseTransferByTagUseCase: PauseTransferByTagUseCase,
    private val pauseTransfersQueueUseCase: PauseTransfersQueueUseCase,
    private val cancelTransfersUseCase: CancelTransfersUseCase,
    private val monitorCompletedTransfersUseCase: MonitorCompletedTransfersUseCase,
    private val moveTransferBeforeByTagUseCase: MoveTransferBeforeByTagUseCase,
    private val moveTransferToFirstByTagUseCase: MoveTransferToFirstByTagUseCase,
    private val moveTransferToLastByTagUseCase: MoveTransferToLastByTagUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val retryChatUploadUseCase: RetryChatUploadUseCase,
    private val deleteFailedOrCancelledTransfersUseCase: DeleteFailedOrCancelledTransfersUseCase,
    private val deleteCompletedTransfersUseCase: DeleteCompletedTransfersUseCase,
    private val deleteCompletedTransfersByIdUseCase: DeleteCompletedTransfersByIdUseCase,
    private val cancelTransferByTagUseCase: CancelTransferByTagUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransfersUiState())
    val uiState = _uiState.asStateFlow()

    private val transfersInfo = savedStateHandle.toRoute<TransfersInfo>()

    init {
        updateSelectedTab(transfersInfo.tabIndex)
        monitorActiveTransfers()
        monitorStorageOverQuota()
        monitorTransferOverQuota()
        monitorPausedTransfers()
        monitorCompletedTransfers()
    }

    private fun monitorActiveTransfers() {
        viewModelScope.launch {
            monitorInProgressTransfersUseCase().collectLatest { activeTransfersMap ->
                val sortedTransfers = activeTransfersMap.values.sortedBy { it.priority }
                setTransfers(
                    if (reordering) {
                        sortedTransfers.matchOrderWithNewAtEnd(uiState.value.activeTransfers) { it.tag }
                    } else {
                        sortedTransfers
                    }
                )
            }
        }
    }

    private fun setTransfers(activeTransfers: Collection<InProgressTransfer>) {
        _uiState.update { state ->
            state.copy(
                activeTransfers = activeTransfers.toImmutableList(),
                selectedActiveTransfersIds = state.selectedActiveTransfersIds?.filter { selectedId ->
                    activeTransfers.any { it.uniqueId == selectedId }
                }?.toImmutableList()
            )
        }
    }

    private fun monitorStorageOverQuota() {
        viewModelScope.launch {
            monitorStorageStateEventUseCase()
                .collectLatest { storageState ->
                    _uiState.update { state ->
                        val isStorageOverQuota = storageState.storageState == StorageState.Red
                                || storageState.storageState == StorageState.PayWall
                        val quotaWarning = getQuotaWarning(
                            isStorageOverQuota = isStorageOverQuota,
                            isTransferOverQuota = state.isTransferOverQuota
                        )

                        state.copy(
                            isStorageOverQuota = isStorageOverQuota,
                            quotaWarning = quotaWarning,
                        )
                    }
                }
        }
    }

    private fun monitorTransferOverQuota() {
        viewModelScope.launch {
            monitorTransferOverQuotaUseCase()
                .collectLatest { isTransferOverQuota ->
                    _uiState.update { state ->
                        val quotaWarning = getQuotaWarning(
                            isStorageOverQuota = state.isStorageOverQuota,
                            isTransferOverQuota = isTransferOverQuota,
                        )

                        state.copy(
                            isTransferOverQuota = isTransferOverQuota,
                            quotaWarning = quotaWarning,
                        )
                    }
                }
        }
    }

    private fun getQuotaWarning(
        isStorageOverQuota: Boolean,
        isTransferOverQuota: Boolean,
    ): QuotaWarning? {
        return when {
            isStorageOverQuota && isTransferOverQuota -> QuotaWarning.StorageAndTransfer
            isStorageOverQuota -> QuotaWarning.Storage
            isTransferOverQuota -> QuotaWarning.Transfer
            else -> null
        }
    }

    /**
     * Consume quota warning.
     */
    fun onConsumeQuotaWarning() {
        _uiState.update { state -> state.copy(quotaWarning = null) }
    }

    private fun monitorPausedTransfers() {
        viewModelScope.launch {
            monitorPausedTransfersUseCase().collectLatest { areTransfersPaused ->
                _uiState.update { state ->
                    state.copy(areTransfersPaused = areTransfersPaused)
                }
            }
        }
    }

    private fun monitorCompletedTransfers() {
        viewModelScope.launch {
            monitorCompletedTransfersUseCase().conflate().collect { completedTransfers ->
                val (completed, failed) = completedTransfers.partition { it.state == TransferState.STATE_COMPLETED }

                _uiState.update { state ->
                    state.copy(
                        completedTransfers = completed
                            .sortedByDescending { it.timestamp }.toImmutableList(),
                        failedTransfers = failed
                            .sortedByDescending { it.timestamp }.toImmutableList(),
                    )
                }
            }
        }
    }

    /**
     * Pause or resume a transfer by tag.
     */
    fun playOrPauseTransfer(tag: Int) {
        runCatching { uiState.value.activeTransfers.first { it.tag == tag }.isPaused }
            .getOrNull()?.let { isPaused ->
                viewModelScope.launch {
                    runCatching { pauseTransferByTagUseCase(tag, !isPaused) }
                        .onFailure { Timber.e(it) }
                }
            }
    }

    /**
     * Resume transfers queue.
     */
    fun resumeTransfersQueue() {
        pauseTransfersQueue(pause = false)
    }

    /**
     * Pause transfers queue.
     */
    fun pauseTransfersQueue() {
        pauseTransfersQueue(pause = true)
    }

    private fun pauseTransfersQueue(pause: Boolean) {
        viewModelScope.launch {
            runCatching { pauseTransfersQueueUseCase(pause) }
                .onSuccess { paused ->
                    _uiState.update { state ->
                        state.copy(areTransfersPaused = paused)
                    }
                }.onFailure {
                    Timber.e(it)
                }
        }
    }

    /**
     * Update selected tab.
     */
    fun updateSelectedTab(tabIndex: Int) {
        _uiState.update { state ->
            state.copy(selectedTab = tabIndex)
        }
    }

    /**
     * Cancel all transfers.
     */
    fun cancelAllTransfers() {
        viewModelScope.launch {
            runCatching { cancelTransfersUseCase() }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Retry selected failed transfer
     */
    fun retryFailedTransfer(transfer: CompletedTransfer) {
        viewModelScope.launch {
            getCloudTransferEventByFailedTransfer(transfer)?.let { cloudTransferEvent ->
                transfer.id?.let { id ->
                    val idAndEvent = mapOf(id to cloudTransferEvent)
                    setStartEvent(TransferTriggerEvent.RetryTransfers(idAndEvent))
                }
            }
        }
    }

    /**
     * Retry selected failed transfers.
     */
    fun retrySelectedFailedTransfers() {
        val selectedIds = uiState.value.selectedFailedTransfersIds ?: return
        retryFailedTransfers(uiState.value.failedTransfers.filter { it.id in selectedIds })
        stopTransfersSelection()
    }

    /**
     * Retry all failed transfers.
     */
    fun retryAllFailedTransfers() = retryFailedTransfers(uiState.value.failedTransfers)

    private fun retryFailedTransfers(failedTransfers: List<CompletedTransfer>) {
        viewModelScope.launch(ioDispatcher) {
            buildMap {
                failedTransfers.forEach { transfer ->
                    getCloudTransferEventByFailedTransfer(transfer)?.let { cloudTransferEvent ->
                        transfer.id?.let { id -> put(id, cloudTransferEvent) }
                    }
                }
            }.let { cloudTransferEvents ->
                setStartEvent(TransferTriggerEvent.RetryTransfers(cloudTransferEvents))
            }
        }
    }

    internal suspend fun getCloudTransferEventByFailedTransfer(failedTransfer: CompletedTransfer): TransferTriggerEvent.CloudTransfer? {
        with(failedTransfer) {
            when {
                type.isDownloadType() -> {
                    getNodeByIdUseCase(NodeId(handle))?.let { typedNode ->
                        return if (isOffline == true) {
                            TransferTriggerEvent.StartDownloadForOffline(
                                node = typedNode,
                                withStartMessage = false
                            )
                        } else {
                            TransferTriggerEvent.RetryDownloadNode(
                                node = typedNode,
                                downloadLocation = path,
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
                            // This is failing and needs update
                            retryChatUploadUseCase(
                                appData?.mapNotNull { it as? TransferAppData.ChatUpload }.orEmpty()
                            )
                            failedTransfer.id?.let {
                                deleteCompletedTransfersByIdUseCase(listOf(it))
                            }
                            return null
                        }.onFailure {
                            //No chat uploads retried, try general upload only.
                            return TransferTriggerEvent.StartUpload.Files(
                                mapOf(path to null),
                                NodeId(parentHandle)
                            )
                        }
                    } else {
                        return TransferTriggerEvent.StartUpload.Files(
                            mapOf(path to null),
                            NodeId(parentHandle)
                        )
                    }
                }

                else -> throw IllegalArgumentException("This transfer type cannot be retried here for now")
            }
        }

        return null
    }

    private fun setStartEvent(event: TransferTriggerEvent) {
        _uiState.update { state -> state.copy(startEvent = triggered(event)) }
    }

    /**
     * Consume start transfer event
     */
    fun consumeStartEvent() {
        _uiState.update { state -> state.copy(startEvent = consumed()) }
    }

    private fun List<TransferAppData>.getOriginalContentUri(): String? = this
        .filterIsInstance<TransferAppData.OriginalUriPath>()
        .firstOrNull()?.originalUriPath?.value

    /**
     * Clear all failed transfers.
     */
    fun clearAllFailedTransfers() {
        viewModelScope.launch {
            runCatching {
                deleteFailedOrCancelledTransfersUseCase()
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Clear all completed transfers.
     */
    fun clearAllCompletedTransfers() {
        viewModelScope.launch {
            runCatching {
                deleteCompletedTransfersUseCase()
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Active transfers are being actively reordered, but still not confirmed.
     * Transfers are reordered in the ui-state but not in the SDK
     * @param fromIndex the index of the transfers that has been moved
     * @param toIndex the index that the transfer should be moved to
     */
    fun onActiveTransfersReorderPreview(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in uiState.value.activeTransfers.indices || toIndex !in uiState.value.activeTransfers.indices) {
            Timber.e(IndexOutOfBoundsException("Reordering indices are not correct: $fromIndex to $toIndex should be in ${uiState.value.activeTransfers.indices}"))
            return
        }
        reordering = true
        setTransfers(
            uiState.value.activeTransfers.toMutableList().moveElement(fromIndex, toIndex)
        )
    }

    /**
     * Active transfers reorder is confirmed. The change will be send to the SDK
     * @param transfer the transfer that has been confirmed, uiState position of this transfer will be send to the SDK
     */
    fun onActiveTransfersReorderConfirmed(transfer: InProgressTransfer) {
        reordering = false
        uiState.value.activeTransfers.indexOfFirst { it.tag == transfer.tag }.takeIf { it >= 0 }
            ?.let { destinationIndex ->
                viewModelScope.launch {
                    runCatching {
                        if (destinationIndex == 0) {
                            moveTransferToFirstByTagUseCase(transfer.tag)
                        } else if (destinationIndex >= uiState.value.activeTransfers.lastIndex) {
                            moveTransferToLastByTagUseCase(transfer.tag)
                        } else {
                            uiState.value.activeTransfers.getOrNull(destinationIndex + 1)?.tag?.let { prevTag ->
                                moveTransferBeforeByTagUseCase(
                                    tag = transfer.tag,
                                    prevTag = prevTag
                                )
                            }
                        }
                    }.onFailure {
                        Timber.d(it, "Error reordering active transfers")
                    }
                    reordering = false
                }
            }
    }

    /**
     * Start active transfers selection by updating the selected active transfers to an empty list
     */
    fun startActiveTransfersSelection() {
        _uiState.update {
            it.copy(selectedActiveTransfersIds = emptyList<Long>().toImmutableList())
        }
    }

    /**
     * Stop transfers selection by updating the selected transfers to null
     */
    fun stopTransfersSelection() {
        _uiState.update {
            it.copy(
                selectedActiveTransfersIds = null,
                selectedCompletedTransfersIds = null,
                selectedFailedTransfersIds = null,
            )
        }
    }

    /**
     * Add the transfer to selected transfers
     */
    fun toggleActiveTransferSelected(inProgressTransfer: InProgressTransfer) {
        val newSelection =
            (uiState.value.selectedActiveTransfersIds ?: emptyList()).let { selected ->
                if (selected.contains(inProgressTransfer.uniqueId)) {
                    selected - inProgressTransfer.uniqueId
                } else {
                    selected + inProgressTransfer.uniqueId
                }
            }
        _uiState.update {
            it.copy(selectedActiveTransfersIds = newSelection.toImmutableList())
        }
    }

    /**
     * Add all the active transfers to the selected transfers
     */
    fun selectAllActiveTransfers() {
        _uiState.update { uiState ->
            uiState.copy(selectedActiveTransfersIds = uiState.activeTransfers.map { it.uniqueId }
                .toImmutableList())
        }
    }

    /**
     * Cancel all selected active transfers
     */
    fun cancelSelectedActiveTransfers() {
        if (uiState.value.areAllActiveTransfersSelected) {
            cancelAllTransfers()
        } else {
            viewModelScope.launch {
                runCatching<Unit> {
                    coroutineScope {
                        uiState.value.selectedActiveTransfersIds?.map { uniqueId ->
                            async {
                                uiState.value.activeTransfers.firstOrNull { it.uniqueId == uniqueId }
                                    ?.let {
                                        cancelTransferByTagUseCase(it.tag)
                                    }
                            }
                        }?.awaitAll()
                    }
                }.onFailure { Timber.e(it) }
            }
        }
        stopTransfersSelection()
    }

    //internal value to preserve previous priority while dragged changes are not send to SDK yet
    private var reordering = false

    /**
     * Add the transfer to selected transfers
     */
    fun toggleCompletedTransferSelection(completedTransfer: CompletedTransfer) {
        val newSelection =
            (uiState.value.selectedCompletedTransfersIds ?: emptyList()).let { selected ->
                if (selected.contains(completedTransfer.id)) {
                    selected - completedTransfer.id
                } else {
                    selected + completedTransfer.id
                }
            }.filterNotNull()
        _uiState.update {
            it.copy(selectedCompletedTransfersIds = newSelection.toImmutableList())
        }
    }

    /**
     * Toggle the transfer to selected transfers
     */
    fun toggleFailedTransferSelection(completedTransfer: CompletedTransfer) {
        val newSelection =
            (uiState.value.selectedFailedTransfersIds ?: emptyList()).let { selected ->
                if (selected.contains(completedTransfer.id)) {
                    selected - completedTransfer.id
                } else {
                    selected + completedTransfer.id
                }
            }.filterNotNull()
        _uiState.update {
            it.copy(selectedFailedTransfersIds = newSelection.toImmutableList())
        }
    }


    /**
     * Add all the active transfers to the selected transfers
     */
    fun selectAllCompletedTransfers() {
        _uiState.update { uiState ->
            uiState.copy(
                selectedCompletedTransfersIds = uiState.completedTransfers.mapNotNull { it.id }
                    .toImmutableList()
            )
        }
    }

    /**
     * Add all the failed transfers to the selected transfers
     */
    fun selectAllFailedTransfers() {
        _uiState.update { uiState ->
            uiState.copy(selectedFailedTransfersIds = uiState.failedTransfers.mapNotNull { it.id }
                .toImmutableList())
        }
    }

    /**
     * Clear all selected completed transfers
     */
    fun clearSelectedCompletedTransfers() {
        if (uiState.value.areAllCompletedTransfersSelected) {
            clearAllCompletedTransfers()
        } else {
            deleteCompletedTransfersByIds(uiState.value.selectedCompletedTransfersIds)
        }
        stopTransfersSelection()
    }

    /**
     * Clear all selected failed transfers
     */
    fun clearSelectedFailedTransfers() {
        if (uiState.value.areAllFailedTransfersSelected) {
            clearAllFailedTransfers()
        } else {
            deleteCompletedTransfersByIds(uiState.value.selectedFailedTransfersIds)
        }
        stopTransfersSelection()
    }

    private fun deleteCompletedTransfersByIds(ids: List<Int>?) {
        viewModelScope.launch {
            runCatching<Unit> {
                ids?.takeIf { it.isNotEmpty() }
                    ?.let { selectedCompletedTransfers ->
                        deleteCompletedTransfersByIdUseCase(selectedCompletedTransfers)
                    }
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Start completed transfers selection by updating the selected active transfers to an empty list
     */
    fun startCompletedTransfersSelection() {
        _uiState.update {
            it.copy(selectedCompletedTransfersIds = emptyList<Int>().toImmutableList())
        }
    }

    /**
     * Start failed transfers selection by updating the selected failed transfers to an empty list
     */
    fun startFailedTransfersSelection() {
        _uiState.update {
            it.copy(selectedFailedTransfersIds = emptyList<Int>().toImmutableList())
        }
    }
}