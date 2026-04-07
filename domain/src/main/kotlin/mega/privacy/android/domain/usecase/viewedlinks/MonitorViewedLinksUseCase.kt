package mega.privacy.android.domain.usecase.viewedlinks

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.repository.ViewedLinksRepository
import javax.inject.Inject

/**
 * Observes all viewed links (file and folder links), sorted by most recently accessed.
 *
 * @property viewedLinksRepository
 */
class MonitorViewedLinksUseCase @Inject constructor(
    private val viewedLinksRepository: ViewedLinksRepository,
) {
    /**
     * @return a [Flow] emitting the current list of [ViewedLink] items whenever the data changes.
     */
    operator fun invoke() = viewedLinksRepository.monitorLinks()
}
