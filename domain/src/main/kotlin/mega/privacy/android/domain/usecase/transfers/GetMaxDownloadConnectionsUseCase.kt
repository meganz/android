package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for getting the maximum number of download connections.
 */
class GetMaxDownloadConnectionsUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke.
     *
     * @return the maximum number of download connections.
     */
    suspend operator fun invoke(): Int = transferRepository.getMaxDownloadConnections()
}
