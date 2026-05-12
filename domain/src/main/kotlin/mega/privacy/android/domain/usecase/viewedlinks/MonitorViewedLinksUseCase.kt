package mega.privacy.android.domain.usecase.viewedlinks

import androidx.paging.PagingSource
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.repository.ViewedLinksRepository
import javax.inject.Inject

/**
 * Observes all viewed links (file and folder links), sorted by most recently accessed.
 *
 * Returns a fresh [PagingSource] on each invocation, suitable for use as the
 * `pagingSourceFactory` of an [androidx.paging.Pager]. The source is invalidated
 * automatically whenever the underlying table changes.
 *
 * @property viewedLinksRepository
 */
class MonitorViewedLinksUseCase @Inject constructor(
    private val viewedLinksRepository: ViewedLinksRepository,
) {
    /**
     * @return a [PagingSource] over [ViewedLink] items.
     */
    operator fun invoke(): PagingSource<Int, ViewedLink> =
        viewedLinksRepository.getViewedLinksPagingSource()
}
