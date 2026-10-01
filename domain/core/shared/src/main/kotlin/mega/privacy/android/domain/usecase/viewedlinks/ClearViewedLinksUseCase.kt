package mega.privacy.android.domain.usecase.viewedlinks

import mega.privacy.android.domain.repository.ViewedLinksRepository
import javax.inject.Inject

/**
 * Deletes all viewed link entries (both file and folder links).
 *
 * @property viewedLinksRepository
 */
class ClearViewedLinksUseCase @Inject constructor(
    private val viewedLinksRepository: ViewedLinksRepository,
) {
    /**
     * Removes all viewed links.
     */
    suspend operator fun invoke() = viewedLinksRepository.clearLinks()
}
