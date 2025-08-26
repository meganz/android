package mega.privacy.android.domain.usecase.transfers.overquota

import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import javax.inject.Inject

/**
 * Use case for updating the timestamp when the transfer over quota error was informed.
 */
class UpdateTransferOverQuotaTimestampUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val getCurrentTimeInMillisUseCase: GetCurrentTimeInMillisUseCase,
) {

    /**
     * Invoke
     */
    operator fun invoke() {
        transferRepository.transferOverQuotaTimestamp.set(getCurrentTimeInMillisUseCase())
    }
}