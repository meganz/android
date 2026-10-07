package mega.privacy.android.domain.usecase.transfers.downloads

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.chat.ChatFile
import mega.privacy.android.domain.entity.transfer.DownloadBatchItem
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferQuotaStateUseCase
import javax.inject.Inject

/**
 * Download a single node as a one item batch, so the SDK predicts the transfer quota state for it
 * and the user can be warned that the quota is running low.
 *
 * The predicted state is broadcast rather than emitted, the same way the pending downloads do.
 */
class DownloadNodeInBatchUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val transferRepository: TransferRepository,
    private val broadcastTransferQuotaStateUseCase: BroadcastTransferQuotaStateUseCase,
) {
    /**
     * Invoke
     *
     * @param node            The node to download.
     * @param destinationPath Full path to the destination folder of [node].
     * @param appData         Custom app data to save in the MegaTransfer object of the node.
     * @param isHighPriority  Puts the transfer on top of the download queue.
     * @return a [Flow] of the batch [TransferEvent]s, without the predicted quota state.
     */
    operator fun invoke(
        node: TypedNode,
        destinationPath: String,
        appData: List<TransferAppData>?,
        isHighPriority: Boolean,
    ): Flow<TransferEvent> = flow {
        fileSystemRepository.createDirectory(destinationPath)
        val finalAppData = if (node is ChatFile) {
            (appData ?: emptyList()) + TransferAppData.ChatDownload(
                node.chatId,
                node.messageId,
                node.messageIndex
            )
        } else appData
        val item = DownloadBatchItem(
            node = node,
            destinationPath = destinationPath,
            fileName = null,
            appData = finalAppData,
            isHighPriority = isHighPriority,
        )
        transferRepository.startDownloadBatch(listOf(item)).collect { event ->
            if (event is TransferEvent.TransferQuotaStateEvent) {
                broadcastTransferQuotaStateUseCase(event.state)
            } else {
                emit(event)
            }
        }
    }
}
