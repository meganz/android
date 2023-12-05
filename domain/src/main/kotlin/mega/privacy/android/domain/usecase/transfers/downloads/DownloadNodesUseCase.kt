package mega.privacy.android.domain.usecase.transfers.downloads

import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.DownloadNodesEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.canceltoken.InvalidateCancelTokenUseCase
import mega.privacy.android.domain.usecase.transfers.active.AddOrUpdateActiveTransferUseCase
import javax.inject.Inject

/**
 * Downloads a list of nodes to the specified path and returns a Flow to monitor the progress
 */
class DownloadNodesUseCase @Inject constructor(
    private val cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    private val invalidateCancelTokenUseCase: InvalidateCancelTokenUseCase,
    private val addOrUpdateActiveTransferUseCase: AddOrUpdateActiveTransferUseCase,
    private val transferRepository: TransferRepository,
    private val fileSystemRepository: FileSystemRepository,
) {
    /**
     * Invoke
     * @param nodes The desired nodes to download
     * @param destinationPath Full destination path of the node, including file name if it's a file node. If this path does not exist it will try to create it
     * @param appData Custom app data to save in the MegaTransfer object.
     * @param isHighPriority Puts the transfer on top of the download queue.
     *
     * @return a flow of [DownloadNodesEvent]s to monitor the download state and progress
     */
    operator fun invoke(
        nodes: List<TypedNode>,
        destinationPath: String,
        appData: TransferAppData?,
        isHighPriority: Boolean,
    ): Flow<DownloadNodesEvent> {
        if (destinationPath.isEmpty()) {
            return nodes.asFlow().map { DownloadNodesEvent.TransferNotStarted(it.id, null) }
        }
        val alreadyProcessed = mutableSetOf<Long>()
        val allIds = nodes.map { it.id.longValue }
        var finishProcessingSend = false
        return channelFlow {
            fileSystemRepository.createDirectory(destinationPath)
            //start all downloads in parallel
            nodes.map { node ->
                launch {
                    transferRepository.startDownload(
                        node = node,
                        localPath = destinationPath,
                        appData = appData,
                        shouldStartFirst = isHighPriority,
                    )
                        .catch { cause ->
                            if (cause is NodeDoesNotExistsException) {
                                send(DownloadNodesEvent.TransferNotStarted(node.id, cause))
                            }
                            alreadyProcessed.add(node.id.longValue)
                        }
                        .collect {
                            send(DownloadNodesEvent.SingleTransferEvent(it))
                        }
                }
            }
        }
            .buffer(capacity = UNLIMITED)
            .transform { event ->
                emit(event)

                if (event is DownloadNodesEvent.SingleTransferEvent) {
                    //update active transfers db
                    addOrUpdateActiveTransferUseCase(event.transferEvent)

                    //check if single node processing is finished
                    if (event.isFinishProcessingEvent()) {
                        val nodeId = NodeId(event.transferEvent.transfer.nodeHandle)
                        if (!alreadyProcessed.contains(nodeId.longValue)) {
                            //this node is already processed: save it and emit the event
                            alreadyProcessed.add(nodeId.longValue)
                            emit(DownloadNodesEvent.TransferFinishedProcessing(nodeId))

                            //check if all nodes have finished processing
                            if (!finishProcessingSend && alreadyProcessed.containsAll(allIds)) {
                                finishProcessingSend = true
                                invalidateCancelTokenUseCase() //we need to avoid a future cancellation from now on
                                emit(DownloadNodesEvent.FinishProcessingTransfers)
                            }
                        }
                    }
                }
            }.onCompletion {
                runCatching { cancelCancelTokenUseCase() }
            }.cancellable()
    }
}