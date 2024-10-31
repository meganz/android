package mega.privacy.android.domain.usecase.transfers.pending

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdateScanningFoldersData
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodesUseCase
import javax.inject.Inject

/**
 * Use case to start all pending transfers of download type
 */
class StartAllPendingDownloadsUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val getPendingTransfersByTypeAndStateUseCase: GetPendingTransfersByTypeAndStateUseCase,
    private val updatePendingTransferStateUseCase: UpdatePendingTransferStateUseCase,
    private val getTypedNodeFromPendingTransferUseCase: GetTypedNodeFromPendingTransferUseCase,
    private val downloadNodesUseCase: DownloadNodesUseCase,
    private val updatePendingTransferStartedCountUseCase: UpdatePendingTransferStartedCountUseCase,
) {
    /**
     * Invoke
     *
     * @return a flow with the number of pending downloads that needs to be started
     */
    operator fun invoke(): Flow<Int> =
        channelFlow {
            getPendingTransfersByTypeAndStateUseCase(
                TransferType.DOWNLOAD,
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
                        //start all downloads in parallel to get the scanning result without the need to finish previous downloads.
                        launch {
                            val node = runCatching {
                                getTypedNodeFromPendingTransferUseCase(pendingTransfer)
                            }.getOrElse {
                                errorOnStartingPendingTransfer(pendingTransfer, null, it)
                                return@launch
                            } ?: run {
                                errorOnStartingPendingTransfer(pendingTransfer, null,
                                    NodeDoesNotExistsException())
                                return@launch
                            }
                            downloadNodesUseCase(
                                nodes = listOfNotNull(node),
                                destinationPath = pendingTransfer.path,
                                appData = pendingTransfer.appData,
                                isHighPriority = pendingTransfer.isHighPriority,
                            )
                                .onEach { transferEvent ->
                                    // Wait for SDK scanning process to be finished. In the meanwhile update the state. At the end delete the pending transfers.
                                    (transferEvent as? MultiTransferEvent.SingleTransferEvent)?.let { singleTransferEvent ->
                                        if (singleTransferEvent.allTransfersUpdated) {
                                            updatePendingTransferStartedCountUseCase(
                                                pendingTransfer,
                                                singleTransferEvent.startedFiles,
                                                singleTransferEvent.alreadyTransferred,
                                            )
                                        } else {
                                            if (singleTransferEvent.scanningFinished) {
                                                updatePendingTransferStateUseCase(
                                                    listOf(pendingTransfer),
                                                    PendingTransferState.SdkScanned
                                                )
                                            }
                                            (singleTransferEvent.transferEvent as? TransferEvent.FolderTransferUpdateEvent)?.let {
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
                                        }
                                    }
                                }
                                .catch {
                                    errorOnStartingPendingTransfer(pendingTransfer, node, it)
                                }
                                .lastOrNull()
                        }
                    }
                }
        }.catch { e ->
            getPendingTransfersByTypeAndStateUseCase(
                TransferType.DOWNLOAD,
                PendingTransferState.NotSentToSdk,
            ).firstOrNull()?.forEach {
                errorOnStartingPendingTransfer(it, null, e)
            }
        }

    private suspend fun errorOnStartingPendingTransfer(
        pendingTransfer: PendingTransfer,
        node: TypedNode?,
        exception: Throwable,
    ) {
        updatePendingTransferStateUseCase(
            listOf(pendingTransfer),
            PendingTransferState.ErrorStarting
        )
        transferRepository.addCompletedTransferFromFailedPendingTransfer(
            pendingTransfer,
            (node as? FileNode)?.size ?: 0,
            exception,
        )
    }
}