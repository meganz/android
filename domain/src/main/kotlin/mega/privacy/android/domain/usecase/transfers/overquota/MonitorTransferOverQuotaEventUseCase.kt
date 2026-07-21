package mega.privacy.android.domain.usecase.transfers.overquota

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.transfer.TransferOverQuotaStatus
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Monitor transfer over quota events (both over quota and almost over quota).
 *
 * Unlike [MonitorTransferOverQuotaUseCase], this emits an event on every occurrence rather than
 * only on state changes, so the presentation layer is notified for each transfer attempt.
 */
class MonitorTransferOverQuotaEventUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     *
     * @return a [Flow] emitting a [TransferOverQuotaStatus] each time a transfer over quota event occurs
     */
    operator fun invoke(): Flow<TransferOverQuotaStatus> =
        transferRepository.monitorTransferOverQuotaEvent()
}
