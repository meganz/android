package mega.privacy.android.domain.usecase.transfers.overquota

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Use case for checking if the user is in transfer over quota.
 */
class IsInTransferOverQuotaUseCase @Inject constructor(
    private val transfersRepository: TransferRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() = transfersRepository.getBandwidthOverQuotaDelay() > 0.seconds
}