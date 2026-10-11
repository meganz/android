package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Delete an active transfers group by id
 */
class DeleteActiveTransferGroupUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
) {
    suspend operator fun invoke(groupId: Int) {
        transferRepository.deleteActiveTransferGroup(groupId)
    }
}