package mega.privacy.android.domain.usecase.transfers.overquota

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject
import kotlin.time.ExperimentalTime

/**
 * Monitor transfer over quota error timestamp
 *
 * @property transferRepository [TransferRepository]
 */
@OptIn(ExperimentalTime::class)
class MonitorTransferOverQuotaErrorTimestampUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    operator fun invoke() = transferRepository.monitorTransferOverQuotaErrorTimestamp()
}