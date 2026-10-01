package mega.privacy.android.domain.exception.transfers

/**
 * Exception thrown when the value for max transfer connections is outside of
 * [mega.privacy.android.domain.repository.TransferRepository.MAX_TRANSFER_CONNECTIONS_RANGE].
 */
class InvalidMaxTransferConnectionsValueException(value: Int) :
    IllegalArgumentException("Invalid max transfer connections value: $value")