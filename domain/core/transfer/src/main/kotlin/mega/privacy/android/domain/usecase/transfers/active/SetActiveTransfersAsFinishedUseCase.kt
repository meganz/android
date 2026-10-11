package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

class SetActiveTransfersAsFinishedUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    suspend operator fun invoke(transfers: List<Transfer>, cancelled: Boolean) {
        transferRepository.putActiveTransfers(transfers.map {
            it.copy(
                isFinished = true,
                state = if (cancelled) TransferState.STATE_CANCELLED else it.state
            )
        })
    }
}