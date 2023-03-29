package mega.privacy.android.app.main.managerSections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.AndroidCompletedTransfer
import mega.privacy.android.app.LegacyDatabaseHandler
import mega.privacy.android.app.globalmanagement.TransfersManagement
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.data.database.DatabaseHandler.Companion.MAX_TRANSFERS
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.transfer.GetInProgressTransfersUseCase
import mega.privacy.android.domain.usecase.transfer.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfer.MonitorFailedTransfer
import mega.privacy.android.domain.usecase.transfer.MoveTransferBeforeByTagUseCase
import mega.privacy.android.domain.usecase.transfer.MoveTransferToFirstByTagUseCase
import mega.privacy.android.domain.usecase.transfer.MoveTransferToLastByTagUseCase
import nz.mega.sdk.MegaApiJava
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
    private val dbH: LegacyDatabaseHandler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    monitorFailedTransfer: MonitorFailedTransfer,
    private val moveTransferBeforeByTagUseCase: MoveTransferBeforeByTagUseCase,
    private val moveTransferToFirstByTagUseCase: MoveTransferToFirstByTagUseCase,
    private val moveTransferToLastByTagUseCase: MoveTransferToLastByTagUseCase,
    private val getTransferByTagUseCase: GetTransferByTagUseCase,
    private val getInProgressTransfersUseCase: GetInProgressTransfersUseCase,
) : ViewModel() {
    private val _activeState = MutableStateFlow<ActiveTransfersState>(ActiveTransfersState.Default)

    /**
     * The state regarding active transfers UI
     */
    val activeState = _activeState.asStateFlow()

    private val _completedState =
        MutableStateFlow<CompletedTransfersState>(CompletedTransfersState.Default)

    /**
     * Failed transfer
     */
    val failedTransfer = monitorFailedTransfer()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

    /**
     * The state regarding completed transfers UI
     */
    val completedState = _completedState.asStateFlow()

    private var activeTransfers = mutableListOf<Transfer>()
    private var completedTransfers = mutableListOf<AndroidCompletedTransfer?>()

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
                activeTransfers.clear()
                activeTransfers.addAll(transfers)
                _activeState.update {
                    ActiveTransfersState.TransfersUpdated(transfers)
                }
            }.onFailure { exception ->
                if (exception is ConcurrentModificationException) {
                    Timber.e("Exception setting transfers: ${exception.message}")
                } else {
                    Timber.e(exception.message)
                }
            }.onSuccess {
                Timber.d("Active transfers correctly set.")
            }
        }

    /**
     * Get active transfer list
     *
     * @return active transfer list
     */
    fun getActiveTransfers() = activeTransfers

    /**
     * Get active transfer
     *
     * @param pos the item position
     * @return active transfer item
     */
    fun getActiveTransfer(pos: Int) = activeTransfers[pos]

    /**
     * Update the active transfer
     *
     * @param pos the item position
     * @param transfer updated item
     */
    fun updateActiveTransfer(pos: Int, transfer: Transfer) {
        activeTransfers[pos] = transfer
        _activeState.update {
            ActiveTransfersState.TransferUpdated(pos, transfer, activeTransfers.toList())
        }
    }

    /**
     * Swap active transfer
     *
     * @param currentPos the current position
     * @param targetPos the target position
     * @return new [MegaTransfer] list
     */
    fun activeTransfersSwap(currentPos: Int, targetPos: Int): List<Transfer> {
        Collections.swap(activeTransfers, currentPos, targetPos)
        return activeTransfers
    }

    /**
     * Tries to update a MegaTransfer in the transfers list.
     *
     * @param transfer The MegaTransfer to update.
     * @return The position of the updated transfer if success, INVALID_POSITION otherwise.
     */
    fun getUpdatedTransferPosition(transfer: Transfer): Int {
        try {
            val index = activeTransfers.indexOfFirst {
                it.tag == transfer.tag
            }
            if (index != INVALID_POSITION) {
                activeTransfers[index] = transfer
                return index
            }
        } catch (e: Exception) {
            Timber.e(e, "IndexOutOfBoundsException trying to update a transfer.")
        }
        return INVALID_POSITION
    }

    /**
     * Updates the UI in consequence after a transfer movement.
     * The update depends on if the movement finished with or without success.
     * If it finished with success, simply update the transfer in the transfers list and in adapter.
     * If not, reverts the movement, leaving the transfer in the same position it has before made the change.
     *
     * @param success     True if the movement finished with success, false otherwise.
     * @param transferTag Identifier of the transfer.
     */
    fun activeTransferFinishMovement(success: Boolean, transferTag: Int) =
        viewModelScope.launch(ioDispatcher) {
            getTransferByTagUseCase(transferTag).let { transfer ->
                if (transfer != null && transfer.transferState >= TransferState.STATE_COMPLETING) {
                    val transferPosition = getUpdatedTransferPosition(transfer)
                    if (transferPosition != INVALID_POSITION) {
                        activeTransfers[transferPosition] = transfer
                        if (!success) {
                            activeTransfers.sortBy { it.priority }
                        }
                        _activeState.update {
                            ActiveTransfersState.TransferMovementFinishedUpdated(
                                success = success,
                                pos = transferPosition,
                                newTransfers = activeTransfers
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
    fun activeTransferStart(transfer: Transfer) {
        activeTransfers.add(transfer)
        activeTransfers.sortBy { it.priority }
        _activeState.update {
            ActiveTransfersState.TransferStartUpdated(transfer, activeTransfers)
        }
    }

    /**
     * Removes a active transfer when finishes.
     *
     * @param transferTag identifier of the transfer to remove
     */
    fun activeTransferFinished(transferTag: Int) {
        val index = activeTransfers.indexOfFirst { transfer ->
            transfer.tag == transferTag
        }
        if (index != INVALID_POSITION) {
            activeTransfers.removeIf { transfer ->
                transfer.tag == transferTag
            }
            _activeState.update {
                ActiveTransfersState.TransferFinishedUpdated(index, activeTransfers.toList())
            }
        }
    }

    /**
     * Active transfer status is changed
     */
    fun activeTransferChangeStatus(tag: Int) = viewModelScope.launch(Dispatchers.IO) {
        Timber.d("tag: $tag")
        val index = activeTransfers.indexOfFirst { transfer ->
            transfer.tag == tag
        }
        if (index != INVALID_POSITION) {
            getTransferByTagUseCase(tag)?.let { transfer ->
                Timber.d("The transfer with index : $index has been paused/resumed, left: ${activeTransfers.size}")
                updateActiveTransfer(index, transfer)
                _activeState.update {
                    ActiveTransfersState.TransferChangeStatusUpdated(index, transfer)
                }
            }
        }
    }

    /**
     * Set the completed transfers
     */
    fun setCompletedTransfers() {
        completedTransfers.clear()
        completedTransfers.addAll(dbH.completedTransfers)
        _completedState.update {
            CompletedTransfersState.TransfersUpdated(completedTransfers.toList())
        }
    }

    /**
     * Adds new completed transfer.
     *
     * @param transfer the transfer to add
     */
    fun completedTransferFinished(transfer: AndroidCompletedTransfer) {
        completedTransfers.add(0, transfer)
        if (completedTransfers.size >= MAX_TRANSFERS) {
            completedTransfers.removeAt(completedTransfers.size - 1)
        }
        _completedState.update {
            CompletedTransfersState.TransferFinishUpdated(completedTransfers.toList())
        }
    }

    /**
     * Removes a completed transfer.
     *
     * @param transfer transfer to remove
     * @param isRemovedCache If ture, remove cache file, otherwise doesn't remove cache file
     */
    fun completedTransferRemoved(transfer: AndroidCompletedTransfer, isRemovedCache: Boolean) =
        viewModelScope.launch(ioDispatcher) {
            if (isRemovedCache) {
                transfer.originalPath?.let {
                    File(it).let { cacheFile ->
                        if (cacheFile.exists()) {
                            if (cacheFile.delete()) {
                                Timber.d("Deleted success, path is $cacheFile")
                            } else {
                                Timber.d("Deleted failed, path is $cacheFile")
                            }
                        }
                    }
                }

            }
            kotlin.runCatching {
                val index = completedTransfers.indexOfFirst { completedTransfer ->
                    completedTransfer?.let {
                        areTheSameTransfer(transfer, it)
                    } ?: false
                }
                if (index != INVALID_POSITION) {
                    completedTransfers.removeAt(index)
                    _completedState.update {
                        CompletedTransfersState.TransferRemovedUpdated(
                            index,
                            completedTransfers.toList()
                        )
                    }
                }
            }.onFailure { exception ->
                if (exception is ConcurrentModificationException) {
                    Timber.e("Exception removing completed transfer: ${exception.message}")
                } else {
                    Timber.e(exception.message)
                }
            }.onSuccess {
                Timber.d("Completed transfer correctly removed.")
            }
        }

    private fun areTheSameTransfer(
        transfer1: AndroidCompletedTransfer,
        transfer2: AndroidCompletedTransfer,
    ) =
        transfer1.id == transfer2.id ||
                (isValidHandle(transfer1) && isValidHandle(transfer2) &&
                        transfer1.nodeHandle == transfer2.nodeHandle) ||
                (transfer1.error == transfer2.error && transfer1.fileName == transfer2.fileName &&
                        transfer1.size == transfer2.size)

    /**
     * Checks if a transfer has a valid handle.
     *
     * @param transfer AndroidCompletedTransfer to check.
     * @return True if the transfer has a valid handle, false otherwise.
     */
    private fun isValidHandle(transfer: AndroidCompletedTransfer) =
        !TextUtil.isTextEmpty(transfer.nodeHandle) && transfer.nodeHandle != MegaApiJava.INVALID_HANDLE.toString()

    /**
     * Removes all completed transfers.
     */
    fun clearCompletedTransfers() = viewModelScope.launch(ioDispatcher) {
        dbH.failedOrCancelledTransfers.mapNotNull { transfer ->
            transfer?.let {
                it.originalPath?.let { path -> File(path) }
            }
        }.forEach { cacheFile ->
            if (cacheFile.exists()) {
                if (cacheFile.delete()) {
                    Timber.d("Deleted success, path is $cacheFile")
                } else {
                    Timber.d("Deleted failed, path is $cacheFile")
                }
            }
        }
        completedTransfers.clear()
        _completedState.update {
            CompletedTransfersState.ClearTransfersUpdated
        }
    }

    /**
     * Get the completed transfers
     *
     * @return [AndroidCompletedTransfer] list
     */
    fun getCompletedTransfers() = completedTransfers

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
        val result = runCatching {
            when (newPosition) {
                0 -> moveTransferToFirstByTagUseCase(tag = transfer.tag)
                activeTransfers.lastIndex -> moveTransferToLastByTagUseCase(tag = transfer.tag)
                else -> moveTransferBeforeByTagUseCase(
                    tag = transfer.tag,
                    prevTag = activeTransfers[newPosition + 1].tag,
                )
            }
        }
        activeTransferFinishMovement(result.isSuccess, transfer.tag)
    }
}