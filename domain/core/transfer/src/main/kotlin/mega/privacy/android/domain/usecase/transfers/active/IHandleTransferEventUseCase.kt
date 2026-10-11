package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.TransferEvent

/**
 * Common interface for use cases to handle transfer events.
 */
interface IHandleTransferEventUseCase {
    /**
     * Invoke
     */
    suspend operator fun invoke(vararg events: TransferEvent)
}