package mega.privacy.android.domain.usecase.transfers.pending

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isAlreadyTransferredEvent
import mega.privacy.android.domain.entity.transfer.isFinishScanningEvent
import mega.privacy.android.domain.entity.transfer.isTransferUpdated
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdateScanningFoldersData
import mega.privacy.android.domain.repository.TransferRepository

abstract class StartAllPendingTransfersUseCase(
    private val transferType: TransferType,
    private val transferRepository: TransferRepository,
    private val getPendingTransfersByTypeAndStateUseCase: GetPendingTransfersByTypeAndStateUseCase,
    private val updatePendingTransferStateUseCase: UpdatePendingTransferStateUseCase,
    private val updatePendingTransferStartedCountUseCase: UpdatePendingTransferStartedCountUseCase,
) {

    abstract suspend fun doTransfer(pendingTransfer: PendingTransfer): Flow<TransferEvent>

    /**
     * Invoke
     *
     * @return a flow with the number of pending transfers that needs to be started
     */
    operator fun invoke(): Flow<Int> =
        channelFlow {
            getPendingTransfersByTypeAndStateUseCase(
                transferType,
                PendingTransferState.NotSentToSdk,
            )
                .conflate()
                .distinctUntilChanged()
                .collect { pendingTransfers ->
                    updatePendingTransferStateUseCase(
                        pendingTransfers,
                        PendingTransferState.SdkScanning
                    )
                    send(pendingTransfers.size)
                    pendingTransfers.forEach { pendingTransfer ->
                        //start all transfers in parallel to get the scanning result without the need to finish previous transfer.
                        launch {
                            doTransfer(pendingTransfer).takeWhile { transferEvent ->
                                if (transferEvent is TransferEvent.TransferStartEvent) {
                                    //to be sure that the active transfer is added before deleting the pending transfer. Transfer Workers use collectChunked to monitor transfer events
                                    transferRepository.insertOrUpdateActiveTransfer(
                                        transferEvent.transfer
                                    )
                                }
                                // Wait for SDK scanning process to be finished. In the meanwhile update the state.
                                if (transferEvent.isTransferUpdated) {
                                    updatePendingTransferStartedCountUseCase(
                                        pendingTransfer,
                                        1,
                                        if (transferEvent.isAlreadyTransferredEvent) 1 else 0,
                                    )
                                    return@takeWhile false // worker will keep monitoring events, no need to monitor anything else regarding pending transfers for this node
                                } else {
                                    if (transferEvent.isFinishScanningEvent) {
                                        updatePendingTransferStateUseCase(
                                            listOf(pendingTransfer),
                                            PendingTransferState.SdkScanned
                                        )
                                    }
                                    (transferEvent as? TransferEvent.FolderTransferUpdateEvent)?.let {
                                        transferRepository.updatePendingTransfer(
                                            UpdateScanningFoldersData(
                                                pendingTransfer.pendingTransferId,
                                                stage = it.stage,
                                                fileCount = it.fileCount.toInt(),
                                                folderCount = it.folderCount.toInt(),
                                                createdFolderCount = it.createdFolderCount.toInt(),
                                            )
                                        )
                                    }
                                    return@takeWhile true //scanning finished. Waiting to check if there are any Already transferred. Corresponding view model could wait a bit for it to update the UI.
                                }
                            }
                                .catch {
                                    errorOnStartingPendingTransfer(pendingTransfer, it)
                                }
                                .lastOrNull()
                        }
                    }
                }
        }.catch { e ->
            getPendingTransfersByTypeAndStateUseCase(
                transferType,
                PendingTransferState.NotSentToSdk,
            ).firstOrNull()?.forEach {
                errorOnStartingPendingTransfer(it, e)
            }
        }

    protected suspend fun errorOnStartingPendingTransfer(
        pendingTransfer: PendingTransfer,
        exception: Throwable,
    ) {
        updatePendingTransferStateUseCase(
            listOf(pendingTransfer),
            PendingTransferState.ErrorStarting
        )
        transferRepository.addCompletedTransferFromFailedPendingTransfer(
            pendingTransfer,
            0,
            exception,
        )
    }
}