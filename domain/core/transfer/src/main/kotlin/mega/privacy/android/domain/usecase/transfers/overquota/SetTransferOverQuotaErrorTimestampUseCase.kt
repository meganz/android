package mega.privacy.android.domain.usecase.transfers.overquota

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Set transfer over quota error timestamp as current time.
 *
 * @property transferRepository [TransferRepository]
 */
class SetTransferOverQuotaErrorTimestampUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    suspend operator fun invoke() = transferRepository.setTransferOverQuotaErrorTimestamp()
}