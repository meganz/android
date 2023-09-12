package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Broadcast transfer over quota
 *
 */
class BroadcastTransferOverQuotaUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = transferRepository.broadcastTransferOverQuota()
}