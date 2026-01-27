package mega.privacy.android.domain.usecase.transfers.active

import mega.privacy.android.domain.entity.transfer.getTransferGroup
import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Use case to update active transfers and clean active transfers groups.
 * Due to previous missing implementation, active transfer groups could not be deleted. This use-case checks the current active transfers once updated and deletes the groups that are not in use.
 */
class UpdateActiveTransfersAndCleanGroupsUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val updateActiveTransfersUseCase: UpdateActiveTransfersUseCase,
) {
    suspend operator fun invoke() {
        updateActiveTransfersUseCase() //ensure active transfers are updated
        val activeTransfers = transferRepository.getCurrentActiveTransfers()
        transferRepository.getActiveTransferGroups().forEach { group ->
            group.groupId?.let { groupId ->
                if (activeTransfers.none { it.getTransferGroup()?.groupId == groupId.toLong() }) {
                    transferRepository.deleteActiveTransferGroup(groupId)
                }
            }
        }
    }
}