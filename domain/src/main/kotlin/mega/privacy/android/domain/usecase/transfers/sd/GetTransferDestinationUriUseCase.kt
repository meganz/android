package mega.privacy.android.domain.usecase.transfers.sd

import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.getSDCardTransferPath
import mega.privacy.android.domain.entity.transfer.getSDCardTransferUri
import mega.privacy.android.domain.repository.TransferRepository
import java.io.File
import javax.inject.Inject

/**
 * Get the destination uri and sub folders of this download transfer or null if it has no destination uri (the transfer's path is already the final destination)
 * Destination uri is usually a content uri where the transferred file needs to be moved once downloaded.
 * Content provider uris can not be accessed by the sdk as are an android specific uris, so they need to be saved in cache folder and later moved by android code
 * The uri represents the selected folder, sub folders are required for transferring child files and replicate the same hierarchy starting from the user-selected destination.
 */
class GetTransferDestinationUriUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke(transfer: Transfer): DestinationUriAndSubFolders? {
        return if (transfer.transferType != TransferType.DOWNLOAD) {
            null
        } else if (transfer.isRootTransfer) {
            transfer.getSDCardTransferUri()?.let {
                DestinationUriAndSubFolders(it)
            }
        } else {
            transfer.folderTransferTag?.let { rootTag ->
                transferRepository.getSdTransferByTag(rootTag)?.let { rootSdTransfer ->
                    rootSdTransfer.getSDCardTransferPath()?.let { rootPath ->
                        val missingFolders = transfer.parentPath
                            .removePrefix(rootPath)
                            .split(File.separator)
                            .filter { it.isNotBlank() }
                        rootSdTransfer.getSDCardTransferUri()?.let {
                            DestinationUriAndSubFolders(it, missingFolders)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Data class to return the result of this use case.
 * @property destinationUri the destination uri for the transfer
 * @property subFolders The sub folders required for transferring child files and replicate the same hierarchy starting from the user-selected destination ([destinationUri])
 */
data class DestinationUriAndSubFolders(
    val destinationUri: String,
    val subFolders: List<String> = emptyList(),
)