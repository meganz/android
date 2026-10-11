package mega.privacy.android.domain.usecase.transfers.previews

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to broadcast transfer tag to cancel.
 */
class BroadcastTransferTagToCancelUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    suspend operator fun invoke(transferTag: Int?) =
        transferRepository.broadcastTransferTagToCancel(transferTag)
}