package mega.privacy.android.domain.usecase.transfers.overquota

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case for getting the time during which transfers will be stopped due to a bandwidth over quota.
 */
class GetBandwidthOverQuotaDelayUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    /**
     * Invoke
     */
    suspend operator fun invoke() = transferRepository.getBandwidthOverQuotaDelay()
}