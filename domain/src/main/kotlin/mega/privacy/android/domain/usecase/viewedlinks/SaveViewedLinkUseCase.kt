package mega.privacy.android.domain.usecase.viewedlinks

import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.repository.ViewedLinksRepository
import javax.inject.Inject

/**
 * Saves or updates a viewed link entry.
 *
 * If a link with the same node handle already exists, its timestamp is updated (upsert).
 *
 * @property viewedLinksRepository
 */
class SaveViewedLinkUseCase @Inject constructor(
    private val viewedLinksRepository: ViewedLinksRepository,
) {
    /**
     * @param viewedLink the [ViewedLink] to persist.
     */
    suspend operator fun invoke(viewedLink: ViewedLink) =
        viewedLinksRepository.saveLink(viewedLink)
}
