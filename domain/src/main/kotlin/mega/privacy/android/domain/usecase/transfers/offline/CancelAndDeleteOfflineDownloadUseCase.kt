package mega.privacy.android.domain.usecase.transfers.offline

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationUseCase
import mega.privacy.android.domain.usecase.offline.IsOfflinePathUseCase
import mega.privacy.android.domain.usecase.transfers.CancelTransferByTagUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Use case to cancel the offline download of a node and delete what it already downloaded, so the
 * node can be removed from offline while its download is still in flight.
 *
 * Cancelling the root transfer of a folder makes the SDK drop the files it has not started yet, the
 * ones it already started are cancelled here as well. Nothing is cancelled for a node downloaded as
 * part of the folder download of an ancestor, see [GetActiveOfflineDownloadTransfersUseCase].
 *
 * The SDK deletes the temporary file of each cancelled transfer, but not the folders it created for
 * a folder download nor the files that already finished.
 */
class CancelAndDeleteOfflineDownloadUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val fileSystemRepository: FileSystemRepository,
    private val getActiveOfflineDownloadTransfersUseCase: GetActiveOfflineDownloadTransfersUseCase,
    private val getOfflineNodeInformationUseCase: GetOfflineNodeInformationUseCase,
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
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

        transfers.forEach { runCatching { cancelTransferByTagUseCase(it.tag) } }
        awaitTransfersStopped(transfers)
        deleteDownloadedFiles(node)
    }

    /**
     * Waits until the cancelled transfers have actually stopped, so their files are not deleted
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
