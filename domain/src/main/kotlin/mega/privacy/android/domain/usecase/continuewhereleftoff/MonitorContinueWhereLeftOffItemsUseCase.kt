package mega.privacy.android.domain.usecase.continuewhereleftoff

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffSortField
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import javax.inject.Inject

/**
 * Monitors recently used items.
 *
 * When both [sortField] and [sortDirection] are non-null, those explicit values are
 * used and the persisted preference is ignored. Otherwise (either or both null) items
 * follow the persisted sort preference.
 */
class MonitorContinueWhereLeftOffItemsUseCase @Inject constructor(
    private val repository: ContinueWhereLeftOffRepository,
) {
    operator fun invoke(
        limit: Int,
        sortField: ContinueWhereLeftOffSortField? = null,
        sortDirection: SortDirection? = null,
    ): Flow<List<ContinueWhereLeftOffItem>> =
        repository.monitorContinueWhereLeftOffItems(limit, sortField, sortDirection)
}
