package mega.privacy.android.feature.sync.domain.usecase.stalledIssue

import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Get sync stalled issues use case
 *
 * @property syncRepository Sync repository
 */
class GetSyncStalledIssuesUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
) {

    /**
     * Invoke
     *
     * @return List<StalledIssue> stalled issues
     */
    suspend operator fun invoke(): List<StalledIssue> =
        syncRepository.getSyncStalledIssues()
}