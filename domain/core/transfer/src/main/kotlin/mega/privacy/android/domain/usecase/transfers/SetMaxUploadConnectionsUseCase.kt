package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.exception.transfers.InvalidMaxTransferConnectionsValueException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.repository.TransferRepository.Companion.MAX_TRANSFER_CONNECTIONS_RANGE
import javax.inject.Inject

/**
 * Use case for setting the maximum number of upload connections.
 *
 * The [connections] value must be in [MAX_TRANSFER_CONNECTIONS_RANGE].
 * Otherwise, an [InvalidMaxTransferConnectionsValueException] is thrown.
 */
class SetMaxUploadConnectionsUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke.
     *
     * @param connections the maximum number of upload connections.
     * @throws InvalidMaxTransferConnectionsValueException if [connections] is
     * outside of [MAX_TRANSFER_CONNECTIONS_RANGE].
     */
    suspend operator fun invoke(connections: Int) {
        if (connections !in MAX_TRANSFER_CONNECTIONS_RANGE) {
            throw InvalidMaxTransferConnectionsValueException(connections)
        }
        transferRepository.setMaxUploadConnections(connections)
    }
}
