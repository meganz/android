package mega.privacy.android.domain.usecase.viewedlinks

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.viewedlinks.ViewedLinksSortField
import mega.privacy.android.domain.repository.ViewedLinksRepository
import javax.inject.Inject

/**
 * Monitors the persisted sort preference for the viewed-links list.
 */
class MonitorViewedLinksSortPreferenceUseCase @Inject constructor(
    private val repository: ViewedLinksRepository,
) {
    operator fun invoke(): Flow<Pair<ViewedLinksSortField, SortDirection>> =
        repository.monitorSortPreference()
}
