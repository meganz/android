package mega.privacy.android.domain.usecase.continuewhereleftoff

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import javax.inject.Inject

/**
 * Monitors recently used items for the widget carousel.
 */
class MonitorContinueWhereLeftOffItemsUseCase @Inject constructor(
    private val repository: ContinueWhereLeftOffRepository,
) {
    operator fun invoke(limit: Int): Flow<List<ContinueWhereLeftOffItem>> =
        repository.monitorContinueWhereLeftOffItems(limit)
}
