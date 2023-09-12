package mega.privacy.android.domain.usecase.transfers

import mega.privacy.android.domain.repository.TransferRepository
import javax.inject.Inject

/**
 * Move transfer before by tag use case
 *
 */
class MoveTransferBeforeByTagUseCase @Inject constructor(
    private val repository: TransferRepository,
) {
    /**
     * Invoke
     *
     * @param tag
     * @param prevTag
     */
    suspend operator fun invoke(tag: Int, prevTag: Int) =
        repository.moveTransferBeforeByTag(tag, prevTag)
}