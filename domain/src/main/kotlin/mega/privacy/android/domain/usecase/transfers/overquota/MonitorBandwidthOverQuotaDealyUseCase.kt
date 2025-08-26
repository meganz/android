package mega.privacy.android.domain.usecase.transfers.overquota

import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import javax.inject.Inject

/**
 * Use case to monitor bandwidth over quota delay
 */
class MonitorBandwidthOverQuotaDelayUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val getCurrentTimeInMillisUseCase: GetCurrentTimeInMillisUseCase,
    private val updateTransferOverQuotaTimestampUseCase: UpdateTransferOverQuotaTimestampUseCase,
    private val monitorTransferOverQuotaUseCase: MonitorTransferOverQuotaUseCase,
    private val getBandwidthOverQuotaDelayUseCase: GetBandwidthOverQuotaDelayUseCase,
) {

    operator fun invoke() = monitorTransferOverQuotaUseCase()
        .conflate()
        .map { isTransferOverQuota ->
            if (isTransferOverQuota && isTimeToWarnTransferOverQuota()) {
                val bandwidthDelay = getBandwidthOverQuotaDelayUseCase()
                if (bandwidthDelay.inWholeSeconds > 0) {
                    updateTransferOverQuotaTimestampUseCase()
                    bandwidthDelay
                } else null
            } else null
        }

    private fun isTimeToWarnTransferOverQuota() =
        (getCurrentTimeInMillisUseCase() - transferRepository.transferOverQuotaTimestamp.get()) > WAIT_TIME_TO_SHOW_TRANSFER_OVER_QUOTA_WARNING

    companion object {
        internal const val WAIT_TIME_TO_SHOW_TRANSFER_OVER_QUOTA_WARNING = 60000L
    }
}