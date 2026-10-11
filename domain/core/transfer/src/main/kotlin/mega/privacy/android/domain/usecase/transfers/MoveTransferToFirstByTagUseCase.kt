package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Move transfer to first by tag use case
 *
 */
class MoveTransferToFirstByTagUseCase @Inject constructor(
    private val repository: TransferRepository,
) {
    /**
     * Invoke
     *
     * @param tag
     */
    suspend operator fun invoke(tag: Int) = repository.moveTransferToFirstByTag(tag)
}