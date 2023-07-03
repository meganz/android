package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Monitor Storage Over Quota use case
 */
class MonitorStorageOverQuotaUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke
     */
    operator fun invoke() = transferRepository.monitorStorageOverQuota()
}
