package mega.privacy.android.domain.usecase.downloads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.FinishProcessingTransfers
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferNotStarted
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.canceltoken.InvalidateCancelTokenUseCase
import javax.inject.Inject

/**
 * Downloads a list of nodes to the specified path
 */
class DownloadNodesUseCase @Inject constructor(
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    private val invalidateCancelTokenUseCase: InvalidateCancelTokenUseCase,
    private val transferRepository: TransferRepository,
) {
    /**
     * Downloads a node to the specified path and returns a Flow to monitor the progress
     * @param nodeIds The desired nodes to download
     * @param destinationPath Full destination path of the node, including file name if it's a file node. If this path does not exist it will try to create it
     * @param appData Custom app data to save in the MegaTransfer object.
     * @param isHighPriority Puts the transfer on top of the download queue.
     *
     * @return a flow of [Transfer]s to monitor the download state and progress
     */
    operator fun invoke(
        nodeIds: List<NodeId>,
        destinationPath: String,
        appData: String?,
        isHighPriority: Boolean,
    ): Flow<MultiTransferEvent> {
        if (destinationPath.isEmpty()) {
            return nodeIds.asFlow().map { TransferNotStarted(it, null) }
        }
        val alreadyProcessed = mutableSetOf<NodeId>()
        var finishProcessingSend = false
        return flow<MultiTransferEvent> {
            nodeIds.forEach { nodeId ->
                runCatching {
                    emitAll(
                        transferRepository.startDownload(
                            nodeId = nodeId,
                            localPath = destinationPath,
                            appData = appData,
                            shouldStartFirst = isHighPriority,
                        )
                    )
                }.onFailure { cause ->
                    if (cause is NodeDoesNotExistsException) {
                        alreadyProcessed.add(nodeId)
                        emit(TransferNotStarted(nodeId, cause))
                    }
                }
            }
        }.onEach { transferEvent ->
            if ((transferEvent as? TransferEvent)?.isFinishProcessingEvent() == true) {
                alreadyProcessed.add(NodeId(transferEvent.transfer.nodeHandle))
            }
        }.transform {
            emit(it)
            if (!finishProcessingSend) {
                if (alreadyProcessed.containsAll(nodeIds)) {
                    finishProcessingSend = true
                    invalidateCancelTokenUseCase() //we need to avoid a future cancellation from now on
                    emit(FinishProcessingTransfers)
                }
            }
        }.onCompletion {
            runCatching { cancelCancelTokenUseCase() }
        }.cancellable()
    }
}