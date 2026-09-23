package mega.privacy.android.domain.usecase.transfers.offline

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isOfflineDownload
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to get the active transfers of the offline download of a node.
 *
 * Only the download the user started for this node is returned: its root transfer, matched by node
 * handle as the only one carrying the offline app data, and the transfers the SDK already started
 * for the files inside it, matched by the tag of that root transfer.
 *
 * A node downloaded as part of the folder download of one of its ancestors is not included. Those
 * transfers belong to the download of the ancestor, which is the only one that can be cancelled,
 * and the SDK keeps starting more of them until it is.
 */
class GetActiveOfflineDownloadTransfersUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    suspend operator fun invoke(node: TypedNode): List<Transfer> {
        val ownTransfers = mutableListOf<Transfer>()
        val childTransfers = mutableListOf<Transfer>()
        transferRepository.getActiveTransfersByType(TransferType.DOWNLOAD)
            .forEach { activeTransfer ->
                val transfer = activeTransfer as? Transfer ?: return@forEach
                if (transfer.isCancelled || transfer.finishedWithErrors()) return@forEach
                when {
                    !transfer.isRootTransfer -> childTransfers.add(transfer)
                    transfer.isOfflineDownload() && transfer.nodeHandle == node.id.longValue ->
                        ownTransfers.add(transfer)
                }
            }
        if (ownTransfers.isEmpty()) return emptyList()

        val ownTags = ownTransfers.mapTo(mutableSetOf()) { it.tag }
        return ownTransfers + childTransfers.filter { it.folderTransferTag in ownTags }
    }

    /**
     * A folder transfer does not transfer bytes itself, so its transferred bytes never reach its
     * total and cannot tell whether it failed.
     */
    private fun Transfer.finishedWithErrors() =
        isFinished && !isAlreadyTransferred && !isFolderTransfer && transferredBytes != totalBytes
}
