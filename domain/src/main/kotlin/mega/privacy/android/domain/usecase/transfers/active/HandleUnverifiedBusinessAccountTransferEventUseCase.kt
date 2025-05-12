package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.exception.BusinessAccountUnverifiedException
import mega.privacy.android.domain.usecase.account.SetUnverifiedBusinessAccountUseCase
import javax.inject.Inject

/**
 * Use case to handle unverified business account transfer events.
 */
class HandleUnverifiedBusinessAccountTransferEventUseCase @Inject constructor(
    private val setUnverifiedBusinessAccountUseCase: SetUnverifiedBusinessAccountUseCase,
) : IHandleTransferEventUseCase {
    /**
     * Invoke
     */
    override suspend operator fun invoke(vararg events: TransferEvent) {
        if (events.any { event -> event is TransferEvent.TransferFinishEvent && event.error is BusinessAccountUnverifiedException }) {
            setUnverifiedBusinessAccountUseCase(true)
        }
    }
}