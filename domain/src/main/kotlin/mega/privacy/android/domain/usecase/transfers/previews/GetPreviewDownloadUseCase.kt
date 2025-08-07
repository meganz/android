package mega.privacy.android.domain.usecase.transfers.previews

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isPreviewDownload
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to get preview download transfer for a given node.
 */
class GetPreviewDownloadUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    suspend operator fun invoke(node: TypedNode): Transfer? =
        transferRepository.getCurrentActiveTransfersByType(TransferType.DOWNLOAD)
            .filter { activeTransfer ->
                activeTransfer.isPreviewDownload() && activeTransfer.fileName == node.name
            }
            .firstNotNullOfOrNull { activeTransfer ->
                transferRepository.getTransferByUniqueId(activeTransfer.uniqueId)?.let { transfer ->
                    if (transfer.nodeHandle == node.id.longValue) {
                        transfer
                    } else {
                        null
                    }
                }
            }
}
