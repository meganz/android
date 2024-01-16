package mega.privacy.android.domain.usecase.transfers.downloads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.canceltoken.InvalidateCancelTokenUseCase
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.AddOrUpdateActiveTransferUseCase
import mega.privacy.android.domain.usecase.transfers.sd.HandleSDCardEventUseCase
import mega.privacy.android.domain.usecase.transfers.shared.AbstractTransferNodesUseCase
import javax.inject.Inject

/**
 * Downloads a list of nodes to the specified path and returns a Flow to monitor the progress
 */
class DownloadNodesUseCase @Inject constructor(
    cancelCancelTokenUseCase: CancelCancelTokenUseCase,
    invalidateCancelTokenUseCase: InvalidateCancelTokenUseCase,
    addOrUpdateActiveTransferUseCase: AddOrUpdateActiveTransferUseCase,
    handleSDCardEventUseCase: HandleSDCardEventUseCase,
    monitorTransferEventsUseCase: MonitorTransferEventsUseCase,
    private val transferRepository: TransferRepository,
    private val fileSystemRepository: FileSystemRepository,
) : AbstractTransferNodesUseCase<TypedNode, NodeId>(
    cancelCancelTokenUseCase,
    invalidateCancelTokenUseCase,
    addOrUpdateActiveTransferUseCase,
    handleSDCardEventUseCase,
    monitorTransferEventsUseCase,
) {
    /**
     * Invoke
     * @param nodes The desired nodes to download
     * @param destinationPath Full destination path of the node, including file name if it's a file node. If this path does not exist it will try to create it
     * @param appData Custom app data to save in the MegaTransfer object.
     * @param isHighPriority Puts the transfer on top of the download queue.
     *
     * @return a flow of [MultiTransferEvent]s to monitor the download state and progress
     */
    operator fun invoke(
        nodes: List<TypedNode>,
        destinationPath: String,
        appData: TransferAppData?,
        isHighPriority: Boolean,
    ): Flow<MultiTransferEvent> {
        if (destinationPath.isEmpty()) {
            return nodes.asFlow().map { MultiTransferEvent.TransferNotStarted(it.id, null) }
        }
        return super.commonInvoke(
            items = nodes,
            beforeStartTransfer = {
                fileSystemRepository.createDirectory(destinationPath)
            }) { node ->
            transferRepository.startDownload(
                node = node,
                localPath = destinationPath,
                appData = appData,
                shouldStartFirst = isHighPriority,
            )
        }
    }

    override fun generateIdFromItem(item: TypedNode) = item.id
    override fun generateIdFromTransferEvent(transferEvent: TransferEvent): NodeId =
        NodeId(transferEvent.transfer.nodeHandle)

}

