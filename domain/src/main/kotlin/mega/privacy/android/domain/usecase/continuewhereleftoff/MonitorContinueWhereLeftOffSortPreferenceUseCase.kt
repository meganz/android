package mega.privacy.android.domain.usecase.continuewhereleftoff

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffSortField
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository
import javax.inject.Inject

/**
 * Monitors the persisted sort preference for the continue-where-left-off list.
 */
class MonitorContinueWhereLeftOffSortPreferenceUseCase @Inject constructor(
    private val repository: ContinueWhereLeftOffRepository,
) {
    operator fun invoke(): Flow<Pair<ContinueWhereLeftOffSortField, SortDirection>> =
        repository.monitorSortPreference()
}
