package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Monitor transfer events use case
 *
 * @property repository
 */
class MonitorTransferEventsUseCase @Inject constructor(
    private val repository: TransferRepository
) {
    /**
     * Invoke
     *
     */
    operator fun invoke() = repository.monitorTransferEvents()
}