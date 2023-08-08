package mega.privacy.android.app.main.managerSections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.data.database.DatabaseHandler.Companion.MAX_TRANSFERS
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.transfer.DeleteCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfer.GetAllCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.transfer.GetFailedOrCanceledTransfersUseCase
import mega.privacy.android.domain.usecase.transfer.GetInProgressTransfersUseCase
import mega.privacy.android.domain.usecase.transfer.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfer.MonitorCompletedTransferEventUseCase
import mega.privacy.android.domain.usecase.transfer.MonitorFailedTransfer
import mega.privacy.android.domain.usecase.transfer.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfer.MoveTransferBeforeByTagUseCase
import mega.privacy.android.domain.usecase.transfer.MoveTransferToFirstByTagUseCase
import mega.privacy.android.domain.usecase.transfer.MoveTransferToLastByTagUseCase
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import java.io.File
import java.util.Collections
import javax.inject.Inject

/**
 * The logic regarding transfers UI
 */
@HiltViewModel
class TransfersViewModel @Inject constructor(
    private val transfersManagement: TransfersManagement,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    monitorFailedTransfer: MonitorFailedTransfer,
    private val moveTransferBeforeByTagUseCase: MoveTransferBeforeByTagUseCase,
    private val moveTransferToFirstByTagUseCase: MoveTransferToFirstByTagUseCase,
    private val moveTransferToLastByTagUseCase: MoveTransferToLastByTagUseCase,
    private val getTransferByTagUseCase: GetTransferByTagUseCase,
    private val getInProgressTransfersUseCase: GetInProgressTransfersUseCase,
    private val getAllCompletedTransfersUseCase: GetAllCompletedTransfersUseCase,
    monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    monitorCompletedTransferEventUseCase: MonitorCompletedTransferEventUseCase,
    private val getFailedOrCanceledTransfersUseCase: GetFailedOrCanceledTransfersUseCase,
    private val deleteCompletedTransferUseCase: DeleteCompletedTransferUseCase,
) : ViewModel() {
    private val _activeState = MutableStateFlow<ActiveTransfersState>(ActiveTransfersState.Default)

    /**
     * The state regarding active transfers UI
     */
    val activeState = _activeState.asStateFlow()

    /**
     * Failed transfer
     */
    val failedTransfer = monitorFailedTransfer()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    private val _activeTransfers = MutableStateFlow(emptyList<Transfer>())

    /**
     * Active transfer
     */
    val activeTransfer = _activeTransfers.asStateFlow()

    /**
     * Monitor transfer event
     */
    val monitorTransferEvent = monitorTransferEventsUseCase()
        .catch { Timber.e(it) }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    private val _completedTransfers = MutableStateFlow(emptyList<CompletedTransfer>())

    /**
     * Completed transfers
     */
    val completedTransfers = _completedTransfers.asStateFlow()

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
                        || transferCallback > it.transfer.notificationNumber
                    ) return@collect
                    transferCallback = it.transfer.notificationNumber
                    when (it) {
                        is TransferEvent.TransferFinishEvent -> transferFinished(it.transfer)
                        is TransferEvent.TransferStartEvent -> startTransfer(it.transfer)
                        is TransferEvent.TransferUpdateEvent -> updateTransfer(it.transfer)
                        is TransferEvent.TransferTemporaryErrorEvent,
                        is TransferEvent.TransferDataEvent,
                        -> Unit
                    }
                }
        }
        viewModelScope.launch {
            monitorCompletedTransferEventUseCase()
                .catch { Timber.e(it) }
                .collect {
                    completedTransferFinished()
                }
        }
        viewModelScope.launch {
            getAllCompletedTransfersUseCase(MAX_TRANSFERS)
                .catch {
                    Timber.e(it)
                }.collect { completedTransfers ->
                    _completedTransfers.update { completedTransfers }
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
                    transfersManagement.isOnTransferOverQuota()
                )
            }
        }
    }

    /**
     * Checks if it is on transfer over quota.
     *
     * @return True if it is on transfer over quota, false otherwise.
     */
    fun isOnTransferOverQuota() = transfersManagement.isOnTransferOverQuota()

    /**
     * Set active transfers
     */
    fun getAllActiveTransfers() =
        viewModelScope.launch {
            runCatching {
                val transfers = getInProgressTransfersUseCase()
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
        val current = activeTransfer.value.toMutableList()
        current.indexOfFirst { it.tag == transfer.tag }
            .takeIf { pos -> pos in current.indices }
            ?.let { pos ->
                current[pos] = transfer
                _activeTransfers.update { current }
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
        Collections.swap(current, currentPos, targetPos)
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
        if (currentTab == TransfersTab.COMPLETED_TAB) {
            transfersManagement.setAreFailedTransfers(false)
        }
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
     * @param success     True if the movement finished with success, false otherwise.
     * @param transferTag Identifier of the transfer.
     */
    private fun activeTransferFinishMovement(success: Boolean, transferTag: Int) =
        viewModelScope.launch(ioDispatcher) {
            getTransferByTagUseCase(transferTag).let { transfer ->
                if (transfer != null && transfer.state >= TransferState.STATE_COMPLETING) {
                    val current = activeTransfer.value.toMutableList()
                    val transferPosition = current.indexOfFirst { it.tag == transferTag }
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
        val current = activeTransfer.value.toMutableList()
        current.add(transfer)
        current.sortBy { it.priority }
        _activeTransfers.update { current }
    }

    /**
     * Removes a active transfer when finishes.
     *
     * @param transfer
     */
    private fun transferFinished(transfer: Transfer) {
        val current = activeTransfer.value.toMutableList()
        current.indexOfFirst {
            it.tag == transfer.tag
        }.takeIf { pos -> pos in current.indices }
            ?.let { pos ->
                current.removeAt(pos)
                _activeTransfers.update { current }
                _activeState.update {
                    ActiveTransfersState.TransferFinishedUpdated(pos, current)
                }
            }
    }

    /**
     * Active transfer status is changed
     */
    fun activeTransferChangeStatus(tag: Int) = viewModelScope.launch {
        Timber.d("tag: $tag")
        getTransferByTagUseCase(tag)?.let { transfer ->
            Timber.d("The transfer with tag : $tag has been paused/resumed, left: ${_activeTransfers.value.size}")
            updateTransfer(transfer)
        }
    }

    /**
     * Adds new completed transfer.
     *
     * @param transfer the transfer to add
     */
    private fun completedTransferFinished() {
        if (currentTab == TransfersTab.COMPLETED_TAB) {
            transfersManagement.setAreFailedTransfers(false)
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
     * Removes all completed transfers.
     */
    fun deleteFailedOrCancelledTransferFiles() = viewModelScope.launch(ioDispatcher) {
        runCatching { getFailedOrCanceledTransfersUseCase() }
            .onSuccess { transfers ->
                transfers.forEach { transfer ->
                    File(transfer.originalPath).takeIf { it.exists() }?.delete()
                }
            }
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
        activeTransferFinishMovement(result.isSuccess, transfer.tag)
    }

    /**
     * Has failed or cancelled transfer
     */
    fun hasFailedOrCancelledTransfer() =
        completedTransfers.value.any { it.state == MegaTransfer.STATE_FAILED || it.state == MegaTransfer.STATE_CANCELLED }
}
