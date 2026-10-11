package mega.privacy.android.domain.usecase.transfers.offline

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isOfflineDownload
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to check whether the user started a download of a node for offline that is still running.
 *
 * True from the moment the download is recorded as a pending transfer until its transfers are done.
 * Needed because the offline information of a node is only saved when its download finishes, so
 * until then the node does not report as available offline.
 */
class IsOfflineDownloadInProgressUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val getActiveOfflineDownloadTransfersUseCase: GetActiveOfflineDownloadTransfersUseCase,
) {
    /**
     * Invoke
     *
     * @param node the [TypedNode] to check
     * @return true if the node has an offline download in progress, false otherwise.
     */
    suspend operator fun invoke(node: TypedNode): Boolean =
        getActiveOfflineDownloadTransfersUseCase(node).isNotEmpty() || hasPendingTransfer(node)

    /**
     * Checked after the active transfers, which are already in memory, as it reads the database.
     * It only covers the node while its download has not been started by the SDK yet.
     */
    private suspend fun hasPendingTransfer(node: TypedNode) = transferRepository
        .getPendingTransfersByType(TransferType.DOWNLOAD)
        .any {
            it.isOfflineDownload()
                    && !it.resolved()
                    && it.nodeIdentifier.nodeId == node.id
        }
}
