package mega.privacy.android.feature.sync.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Monitor the sync stalled issues
 */
class MonitorSyncStalledIssuesUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {

    /**
     * Invoke.
     *
     * @return A [Flow] that emits the sync stalled issues
     */
    operator fun invoke(): Flow<List<StalledIssue>> = syncRepository.monitorStalledIssues()
}