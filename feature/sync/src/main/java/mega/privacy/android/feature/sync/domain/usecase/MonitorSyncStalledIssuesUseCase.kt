package mega.privacy.android.feature.sync.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Monitor the sync stalled issues
 */
class MonitorSyncStalledIssuesUseCase @Inject constructor(
    private val getSyncStalledIssuesUseCase: GetSyncStalledIssuesUseCase,
    private val syncRepository: SyncRepository,
) {

    /**
     * Invoke.
     *
     * @return A [Flow] that emits the sync stalled issues
     */
    suspend operator fun invoke(): Flow<List<StalledIssue>> =
        syncRepository
            .monitorSyncChanges()
            .map { getSyncStalledIssuesUseCase() }
            .onStart { emit(getSyncStalledIssuesUseCase()) }
}