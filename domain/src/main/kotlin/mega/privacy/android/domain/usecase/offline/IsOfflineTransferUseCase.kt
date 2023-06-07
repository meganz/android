package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.repository.FileSystemRepository
import javax.inject.Inject

/**
 * Check if a Transfer is to downloading a node for offline use
 */
class IsOfflineTransferUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
) {

    /**
     * Check if a Transfer is to downloading a node for offline use
     * @param transfer the [Transfer] that will be checked
     * @return true if it's a download for offline use
     */
    suspend operator fun invoke(transfer: Transfer) =
        transfer.localPath.startsWith(fileSystemRepository.getOfflinePath())
                || transfer.localPath.startsWith(fileSystemRepository.getOfflineInboxPath())
}