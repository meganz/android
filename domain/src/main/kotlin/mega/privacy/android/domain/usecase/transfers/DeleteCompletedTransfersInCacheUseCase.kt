package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to delete completed transfers in cache folder
 */
class DeleteCompletedTransfersInCacheUseCase @Inject constructor(
    private val cacheRepository: CacheRepository,
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = transferRepository.deleteCompletedTransfersByPath(
        cacheRepository.getPreviewDownloadPathForNode().trimEnd('/')
    )
}