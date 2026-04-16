package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for getting the maximum number of upload connections.
 */
class GetMaxUploadConnectionsUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke.
     *
     * @return the maximum number of upload connections.
     */
    suspend operator fun invoke(): Int = transferRepository.getMaxUploadConnections()
}
