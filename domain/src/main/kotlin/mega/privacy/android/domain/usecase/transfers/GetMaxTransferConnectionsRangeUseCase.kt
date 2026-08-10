package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository.Companion.MAX_TRANSFER_CONNECTIONS_RANGE
import javax.inject.Inject

/**
 * Use case for getting the valid [IntRange] for the maximum number of
 * transfer connections (both downloads and uploads).
 */
class GetMaxTransferConnectionsRangeUseCase @Inject constructor() {
    /**
     * Invoke.
     *
     * @return the [IntRange] of valid values for the maximum number of
     * transfer connections.
     */
    operator fun invoke(): IntRange = MAX_TRANSFER_CONNECTIONS_RANGE
}
