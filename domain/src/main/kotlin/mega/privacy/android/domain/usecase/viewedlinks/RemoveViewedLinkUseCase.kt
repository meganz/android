package mega.privacy.android.domain.usecase.viewedlinks

import mega.privacy.android.domain.repository.ViewedLinksRepository
import javax.inject.Inject

/**
 * Removes a single viewed link entry by its node handle.
 *
 * @property viewedLinksRepository
 */
class RemoveViewedLinkUseCase @Inject constructor(
    private val viewedLinksRepository: ViewedLinksRepository,
) {
    /**
     * @param nodeHandle the handle of the node to remove.
     */
    suspend operator fun invoke(nodeHandle: Long) = viewedLinksRepository.removeLink(nodeHandle)
}
