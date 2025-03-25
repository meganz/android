package mega.privacy.android.domain.usecase.transfers.overquota

import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Monitor transfer over quota
 *
 */
class MonitorTransferOverQuotaUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val isInTransferOverQuotaUseCase: IsInTransferOverQuotaUseCase,
) {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke() = transferRepository.monitorTransferOverQuota()
        .onStart { isInTransferOverQuotaUseCase() }
}