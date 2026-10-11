package mega.privacy.android.domain.usecase.viewedlinks

import mega.privacy.android.domain.repository.ViewedLinksRepository
import javax.inject.Inject

/**
 * Removes a single viewed link entry by its link URL. Used to prune entries whose
 * underlying node is no longer reachable (removed, expired, owner-terminated).
 *
 * No-op if no entry matches the URL.
 *
 * @property viewedLinksRepository
 */
class RemoveViewedLinkByUrlUseCase @Inject constructor(
    private val viewedLinksRepository: ViewedLinksRepository,
) {
    /**
     * @param linkUrl the link URL whose entry should be removed.
     */
    suspend operator fun invoke(linkUrl: String) =
        viewedLinksRepository.removeLinkByUrl(linkUrl)
}
