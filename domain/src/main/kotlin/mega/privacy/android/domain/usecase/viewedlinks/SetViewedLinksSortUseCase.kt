package mega.privacy.android.domain.usecase.viewedlinks

import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.viewedlinks.ViewedLinksSortField
import mega.privacy.android.domain.repository.ViewedLinksRepository
import javax.inject.Inject

/**
 * Persists the sort preference for the viewed-links list.
 */
class SetViewedLinksSortUseCase @Inject constructor(
    private val repository: ViewedLinksRepository,
) {
    suspend operator fun invoke(
        sortField: ViewedLinksSortField,
        sortDirection: SortDirection,
    ) = repository.setSortPreference(sortField, sortDirection)
}
