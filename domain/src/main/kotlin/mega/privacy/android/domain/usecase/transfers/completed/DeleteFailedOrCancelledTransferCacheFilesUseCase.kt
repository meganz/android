package mega.privacy.android.domain.usecase.transfers.completed

import mega.privacy.android.domain.repository.CacheRepository
import mega.privacy.android.domain.usecase.transfers.GetFailedOrCanceledTransfersUseCase
import java.io.File
import javax.inject.Inject

/**
 * Use case for removing cache files which belongs to cancelled or failed transfers.
 */
class DeleteFailedOrCancelledTransferCacheFilesUseCase @Inject constructor(
    private val getFailedOrCanceledTransfersUseCase: GetFailedOrCanceledTransfersUseCase,
    private val cacheRepository: CacheRepository,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() {
        getFailedOrCanceledTransfersUseCase().forEach { transfer ->
            File(transfer.originalPath).let { file ->
                if (cacheRepository.isFileInCacheDirectory(file)) {
                    file.delete()
                }
            }
        }
    }
}