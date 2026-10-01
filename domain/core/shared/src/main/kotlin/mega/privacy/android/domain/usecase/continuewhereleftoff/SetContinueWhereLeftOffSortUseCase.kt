package mega.privacy.android.domain.usecase.continuewhereleftoff

import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffSortField
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import javax.inject.Inject

/**
 * Persists the sort preference for the continue-where-left-off list.
 */
class SetContinueWhereLeftOffSortUseCase @Inject constructor(
    private val repository: ContinueWhereLeftOffRepository,
) {
    suspend operator fun invoke(
        sortField: ContinueWhereLeftOffSortField,
        sortDirection: SortDirection,
    ) = repository.setSortPreference(sortField, sortDirection)
}
