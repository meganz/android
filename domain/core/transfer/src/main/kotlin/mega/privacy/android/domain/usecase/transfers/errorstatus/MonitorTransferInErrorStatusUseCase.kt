package mega.privacy.android.domain.usecase.transfers.errorstatus

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to get the transfer in error status flow
 */
class MonitorTransferInErrorStatusUseCase @Inject constructor(private val transferRepository: TransferRepository) {
    /**
     * Invoke
     */
    operator fun invoke(): Flow<Boolean> = transferRepository.monitorTransferInErrorStatus()
}