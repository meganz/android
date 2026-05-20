package mega.privacy.android.domain.usecase.viewedlinks

import mega.privacy.android.domain.repository.ViewedLinksRepository
import javax.inject.Inject

/**
 * Removes a single viewed link entry by its node handle.
 *
 * @property viewedLinksRepository
 */
class RemoveViewedLinksUseCase @Inject constructor(
    private val viewedLinksRepository: ViewedLinksRepository,
) {
    /**
     * @param nodeHandles set of node handles to remove.
     */
    suspend operator fun invoke(nodeHandles: Set<Long>) =
        viewedLinksRepository.removeLinks(nodeHandles)
}
