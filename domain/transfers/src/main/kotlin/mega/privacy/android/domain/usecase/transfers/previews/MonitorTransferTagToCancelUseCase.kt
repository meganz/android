package mega.privacy.android.domain.usecase.transfers.previews

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to monitor transfer tag to cancel.
 */
class MonitorTransferTagToCancelUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {

    operator fun invoke() = transferRepository.monitorTransferTagToCancel()
}