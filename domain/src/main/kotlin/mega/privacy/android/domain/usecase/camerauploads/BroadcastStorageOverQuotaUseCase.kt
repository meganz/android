package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Broadcast Storage Over Quota Use Case
 */
class BroadcastStorageOverQuotaUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = transferRepository.broadcastStorageOverQuota()
}
