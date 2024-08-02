package mega.privacy.android.domain.usecase.transfers.sd

import mega.privacy.android.domain.entity.transfer.DestinationUriAndSubFolders
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
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
        return when {
            transfer.transferType != TransferType.DOWNLOAD -> null
            transfer.isSyncTransfer -> null

            transfer.isRootTransfer -> {
                transfer.getSDCardTransferUri()?.let {
                    DestinationUriAndSubFolders(it)
                }
            }

            else -> {
                transfer.folderTransferTag?.let { rootTag ->
                    transferRepository.getSdTransferByTag(rootTag)?.let { rootSdTransfer ->
                        rootSdTransfer.path.let { rootPath ->
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
}