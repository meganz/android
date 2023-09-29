package mega.privacy.android.domain.usecase.transfers.overquota

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Monitor transfer over quota
 *
 */
class MonitorTransferOverQuotaUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     *
     * @return
     */
    operator fun invoke() = transferRepository.monitorTransferOverQuota()
}