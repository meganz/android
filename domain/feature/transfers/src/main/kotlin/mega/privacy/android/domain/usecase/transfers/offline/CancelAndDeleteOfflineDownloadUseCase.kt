package mega.privacy.android.domain.usecase.transfers.offline

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.getTransferGroup
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflinePathForNodeUseCase
import mega.privacy.android.domain.usecase.offline.IsOfflinePathUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.offline.CancelAndDeleteOfflineDownloadUseCase.Companion.CANCELLATION_TIMEOUT
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Use case to cancel the offline download of a node and delete what it already downloaded, so the
 * node can be removed from offline while its download is still in flight.
 *
 * Only the download the user started for this node is cancelled, so a download of an ancestor is
 * left running. The SDK deletes the temporary file of each cancelled transfer, but not the folders
 * it created for a folder download nor the files that already finished.
 */
class CancelAndDeleteOfflineDownloadUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val fileSystemRepository: FileSystemRepository,
    private val getActiveOfflineDownloadTransfersUseCase: GetActiveOfflineDownloadTransfersUseCase,
    private val getOfflineNodeInformationUseCase: GetOfflineNodeInformationUseCase,
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
    private val getOfflinePathForNodeUseCase: GetOfflinePathForNodeUseCase,
    private val cancelTransferByTagUseCase: CancelTransferByTagUseCase,
    private val isOfflinePathUseCase: IsOfflinePathUseCase,
) {
    /**
     * Invoke
     *
     * @param node the [TypedNode] whose offline download will be cancelled
     */
    suspend operator fun invoke(node: TypedNode) {
        val transfers = getActiveOfflineDownloadTransfersUseCase(node).filterNot { it.isFinished }
        if (transfers.isEmpty()) return

        val ownTransfers = node.getTransfersStartedForNode(transfers)
        if (ownTransfers.isEmpty()) return

        cancelTransfers(ownTransfers)
        awaitTransfersStopped(ownTransfers)
        deleteDownloadedFiles(node)
    }

    private suspend fun cancelTransfers(transfers: List<Transfer>) {
        transfers
            .flatMap { listOfNotNull(it.folderTransferTag, it.tag) }
            .distinct()
            .forEach { tag ->
                runCatching { cancelTransferByTagUseCase(tag) }
            }
    }

    /**
     * Transfers can belong to several groups while a download of an ancestor runs at the same time.
     */
    private suspend fun TypedNode.getTransfersStartedForNode(transfers: List<Transfer>): List<Transfer> {
        transfers.filter { it.isRootTransfer && it.nodeHandle == this.id.longValue }
            .takeIf { it.isNotEmpty() }
            ?.let { return it }

        val offlinePath = getOfflinePathForNodeUseCase(this) ?: return emptyList()
        val ownGroupIds = transfers
            .mapNotNull { it.getTransferGroup()?.groupId }
            .distinct()
            .filter {
                transferRepository.getActiveTransferGroupById(it.toInt())?.destination == offlinePath
            }

        return transfers.filter { it.getTransferGroup()?.groupId in ownGroupIds }
    }

    /**
     * Waits until the canceled transfers have actually stopped, so their files are not deleted
     * while they are still being written, giving up after [CANCELLATION_TIMEOUT].
     */
    private suspend fun awaitTransfersStopped(cancelledTransfers: List<Transfer>) {
        val uniqueIds = cancelledTransfers.mapTo(mutableSetOf()) { it.uniqueId }
        withTimeoutOrNull(CANCELLATION_TIMEOUT) {
            transferRepository.monitorActiveTransfersByType(TransferType.DOWNLOAD)
                .first { activeTransfers ->
                    activeTransfers.none { it.uniqueId in uniqueIds && !it.isFinished }
                }
        }
    }

    private suspend fun deleteDownloadedFiles(node: TypedNode) {
        val offlinePath = runCatching {
            getOfflineFileUseCase(getOfflineNodeInformationUseCase(node)).path
        }.getOrNull() ?: return
        if (!isOfflinePathUseCase(offlinePath)) return

        fileSystemRepository.deleteFolderAndItsFiles(offlinePath)
    }

    internal companion object {
        val CANCELLATION_TIMEOUT = 10.seconds
    }
}
