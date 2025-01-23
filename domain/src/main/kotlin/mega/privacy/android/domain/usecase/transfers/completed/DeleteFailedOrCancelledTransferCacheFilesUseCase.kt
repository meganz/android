package mega.privacy.android.domain.usecase.transfers.completed

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.transfers.DeleteCacheFilesUseCase
import mega.privacy.android.domain.usecase.transfers.GetFailedOrCanceledTransfersUseCase
import javax.inject.Inject

/**
 * Use case for removing cache files which belongs to cancelled or failed transfers.
 */
class DeleteFailedOrCancelledTransferCacheFilesUseCase @Inject constructor(
    private val getFailedOrCanceledTransfersUseCase: GetFailedOrCanceledTransfersUseCase,
    private val deleteCacheFilesUseCase: DeleteCacheFilesUseCase,
) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() {
        getFailedOrCanceledTransfersUseCase().map { transfer ->
            UriPath(transfer.originalPath)
        }.let { pathsToDelete ->
            if (pathsToDelete.isNotEmpty()) {
                deleteCacheFilesUseCase(pathsToDelete)
            }
        }
    }
}