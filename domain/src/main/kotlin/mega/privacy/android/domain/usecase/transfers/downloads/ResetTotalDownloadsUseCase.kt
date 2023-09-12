package mega.privacy.android.domain.usecase.transfers.downloads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to reset total downloads
 */
class ResetTotalDownloadsUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() = transferRepository.resetTotalDownloads()
}