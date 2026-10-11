package mega.privacy.android.domain.usecase.transfers.offline

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isOfflineDownload
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflineNodeInformationUseCase
import java.io.File
import javax.inject.Inject

/**
 * Use case to get the active transfers downloading a node for offline.
 *
 * The transfer the SDK creates for a folder is an empty container with no app data, handle or
 * path, so a folder is recognised by the files being downloaded into its offline folder. Those
 * count whether the user chose the node or an ancestor; only the download of the node itself can
 * be cancelled, see [CancelAndDeleteOfflineDownloadUseCase].
 */
class GetActiveOfflineDownloadTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val getOfflineNodeInformationUseCase: GetOfflineNodeInformationUseCase,
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
) {
    suspend operator fun invoke(node: TypedNode): List<Transfer> {
        val transfers = transferRepository.getActiveTransfersByType(TransferType.DOWNLOAD)
            .filterIsInstance<Transfer>()
            .filter { !it.isCancelled && !it.finishedWithErrors() }
        if (transfers.isEmpty()) return emptyList()

        val ownTransfers = transfers.filter {
            it.isRootTransfer && it.isOfflineDownload() && it.nodeHandle == node.id.longValue
        }
        val fileTransfers = transfers.filterNot { it.isRootTransfer }
        if (fileTransfers.isEmpty()) return ownTransfers

        val offlinePath = runCatching {
            getOfflineFileUseCase(getOfflineNodeInformationUseCase(node)).path
        }.getOrNull() ?: return ownTransfers

        return ownTransfers + fileTransfers.filter { it.localPath.isInside(offlinePath) }
    }

    /**
     * A folder transfer does not transfer bytes itself, so its transferred bytes never reach its
     * total and cannot tell whether it failed.
     */
    private fun Transfer.finishedWithErrors() =
        isFinished && !isAlreadyTransferred && !isFolderTransfer && transferredBytes != totalBytes

    private fun String.isInside(path: String) =
        this == path || startsWith(path.ensureEndsWithFileSeparator())

    private fun String.ensureEndsWithFileSeparator() =
        if (endsWith(File.separator)) this else plus(File.separator)
}
